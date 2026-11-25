import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

final class DBConnection extends MarketplaceStore {
    private static final String DEFAULT_URL = "jdbc:mysql://127.0.0.1:3306/ucampus_mart";
    private static final String DEFAULT_USER = "root";

    private final String url;
    private final String user;
    private final String password;

    private DBConnection(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    static DBConnection fromEnvironment(Supplier<String> readPassword) {
        String url = System.getenv("UCAMPUS_DB_URL");
        String user = System.getenv("UCAMPUS_DB_USER");
        String password = System.getenv("UCAMPUS_DB_PASSWORD");
        if (url == null || url.isBlank()) {
            url = DEFAULT_URL;
        }
        if (!url.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("UCAMPUS_DB_URL must be a jdbc:mysql:// URL.");
        }
        if (user == null || user.isBlank()) {
            user = DEFAULT_USER;
        }
        if (password == null) {
            password = readPassword.get();
        }
        return new DBConnection(url, user, password);
    }

    @Override
    Marketplace load() throws IOException {
        return inTransaction(DBConnection::readMarketplace);
    }

    @Override
    <T> T update(Function<Marketplace, T> change) throws IOException {
        return inTransaction(connection -> {
            lockMarketplace(connection);
            Marketplace marketplace = readMarketplace(connection);
            Set<Listing> originalListings = Set.copyOf(marketplace.allListings());
            Set<Marketplace.Offer> originalOffers = Set.copyOf(marketplace.allOffers());
            T result = change.apply(marketplace);
            saveListings(connection, marketplace.allListings(), originalListings);
            saveOffers(connection, marketplace.allOffers(), originalOffers);
            return result;
        });
    }

    private <T> T inTransaction(SqlOperation<T> operation) throws IOException {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            try {
                T result = operation.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IOException("MySQL (" + exception.getSQLState() + "): " + exception.getMessage(), exception);
        }
    }

    private static void lockMarketplace(Connection connection) throws SQLException {
        // Serialize writers before loading their snapshot so they cannot reuse IDs or accept competing offers.
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT id FROM marketplace_lock WHERE id = 1 FOR UPDATE")) {
            if (!rows.next()) {
                throw new SQLException("Marketplace lock row is missing. Apply database/schema.sql.");
            }
        }
    }

    private static Marketplace readMarketplace(Connection connection) throws SQLException {
        try {
            return new Marketplace(readListings(connection), readOffers(connection));
        } catch (IllegalArgumentException exception) {
            throw new SQLException("Invalid marketplace data. " + exception.getMessage(), exception);
        }
    }

    private static List<Listing> readListings(Connection connection) throws SQLException {
        List<Listing> listings = new ArrayList<>();
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("""
                     SELECT id, owner, title, category, item_condition, age_months, mode, price, status
                     FROM listings ORDER BY id
                     """)) {
            while (rows.next()) {
                String price = rows.getString("price");
                listings.add(new Listing(rows.getInt("id"), Marketplace.studentHandle(rows.getString("owner")),
                        rows.getString("title"), rows.getString("category"), rows.getString("item_condition"),
                        rows.getInt("age_months"), Listing.Mode.valueOf(rows.getString("mode")),
                        price == null ? null : new BigDecimal(price), Listing.Status.valueOf(rows.getString("status"))));
            }
        }
        return listings;
    }

    private static List<Marketplace.Offer> readOffers(Connection connection) throws SQLException {
        List<Marketplace.Offer> offers = new ArrayList<>();
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT id, requester, target_id, offered_id, status FROM offers ORDER BY id")) {
            while (rows.next()) {
                offers.add(new Marketplace.Offer(rows.getInt("id"), rows.getString("requester"),
                        rows.getInt("target_id"), rows.getObject("offered_id", Integer.class),
                        Marketplace.OfferStatus.valueOf(rows.getString("status"))));
            }
        }
        return offers;
    }

    private static void saveListings(Connection connection, List<Listing> listings, Set<Listing> original)
            throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO listings (id, owner, title, category, item_condition, age_months, mode, price, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE status = ?
                """)) {
            for (Listing listing : listings) {
                if (original.contains(listing)) {
                    continue;
                }
                String status = listing.status().name();
                statement.setInt(1, listing.id());
                statement.setString(2, listing.owner());
                statement.setString(3, listing.title());
                statement.setString(4, listing.category());
                statement.setString(5, listing.condition());
                statement.setInt(6, listing.ageMonths());
                statement.setString(7, listing.mode().name());
                statement.setString(8, listing.price() == null ? null : listing.price().toPlainString());
                statement.setString(9, status);
                statement.setString(10, status);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void saveOffers(Connection connection, List<Marketplace.Offer> offers, Set<Marketplace.Offer> original)
            throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO offers (id, requester, target_id, offered_id, status)
                VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE status = ?
                """)) {
            for (Marketplace.Offer offer : offers) {
                if (original.contains(offer)) {
                    continue;
                }
                String status = offer.status().name();
                statement.setInt(1, offer.id());
                statement.setString(2, offer.requester());
                statement.setInt(3, offer.targetId());
                statement.setObject(4, offer.offeredId(), Types.INTEGER);
                statement.setString(5, status);
                statement.setString(6, status);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    @FunctionalInterface
    private interface SqlOperation<T> {
        T run(Connection connection) throws SQLException;
    }
}
