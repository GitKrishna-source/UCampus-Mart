import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

final class MarketplacePropertiesCodec {
    private static final String VERSION = "1";

    private MarketplacePropertiesCodec() {
    }

    static Marketplace decode(Properties data) {
        if (!VERSION.equals(required(data, "version"))) {
            throw new IllegalArgumentException("Unsupported data format version.");
        }
        return new Marketplace(readListings(data), readOffers(data));
    }

    static Properties encode(Marketplace marketplace) {
        Properties data = new Properties();
        data.setProperty("version", VERSION);
        writeListings(data, marketplace.allListings());
        writeOffers(data, marketplace.allOffers());
        return data;
    }

    private static void writeListings(Properties data, List<Listing> listings) {
        data.setProperty("listing.count", Integer.toString(listings.size()));
        for (int index = 0; index < listings.size(); index++) {
            Listing listing = listings.get(index);
            String key = "listing." + index + ".";
            data.setProperty(key + "id", Integer.toString(listing.id()));
            data.setProperty(key + "owner", listing.owner());
            data.setProperty(key + "title", listing.title());
            data.setProperty(key + "category", listing.category());
            data.setProperty(key + "condition", listing.condition());
            data.setProperty(key + "ageMonths", Integer.toString(listing.ageMonths()));
            data.setProperty(key + "mode", listing.mode().name());
            data.setProperty(key + "status", listing.status().name());
            if (listing.price() != null) {
                data.setProperty(key + "price", listing.price().toPlainString());
            }
        }
    }

    private static void writeOffers(Properties data, List<Marketplace.Offer> offers) {
        data.setProperty("offer.count", Integer.toString(offers.size()));
        for (int index = 0; index < offers.size(); index++) {
            Marketplace.Offer offer = offers.get(index);
            String key = "offer." + index + ".";
            data.setProperty(key + "id", Integer.toString(offer.id()));
            data.setProperty(key + "requester", offer.requester());
            data.setProperty(key + "targetId", Integer.toString(offer.targetId()));
            data.setProperty(key + "status", offer.status().name());
            if (offer.isExchange()) {
                data.setProperty(key + "offeredId", Integer.toString(offer.offeredId()));
            }
        }
    }

    private static List<Listing> readListings(Properties data) {
        int count = count(data, "listing.count");
        List<Listing> listings = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String key = "listing." + index + ".";
            String price = data.getProperty(key + "price");
            listings.add(new Listing(
                    Integer.parseInt(required(data, key + "id")),
                    Marketplace.studentHandle(required(data, key + "owner")),
                    required(data, key + "title"),
                    required(data, key + "category"),
                    required(data, key + "condition"),
                    Integer.parseInt(required(data, key + "ageMonths")),
                    Listing.Mode.valueOf(required(data, key + "mode")),
                    price == null ? null : new BigDecimal(price),
                    Listing.Status.valueOf(required(data, key + "status"))));
        }
        return listings;
    }

    private static List<Marketplace.Offer> readOffers(Properties data) {
        int count = count(data, "offer.count");
        List<Marketplace.Offer> offers = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String key = "offer." + index + ".";
            String offeredId = data.getProperty(key + "offeredId");
            offers.add(new Marketplace.Offer(
                    Integer.parseInt(required(data, key + "id")),
                    required(data, key + "requester"),
                    Integer.parseInt(required(data, key + "targetId")),
                    offeredId == null ? null : Integer.valueOf(offeredId),
                    Marketplace.OfferStatus.valueOf(required(data, key + "status"))));
        }
        return offers;
    }

    private static int count(Properties data, String key) {
        int count = Integer.parseInt(required(data, key));
        if (count < 0) {
            throw new IllegalArgumentException("Negative value for " + key + ".");
        }
        return count;
    }

    private static String required(Properties data, String key) {
        String value = data.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing " + key + ".");
        }
        return value;
    }
}
