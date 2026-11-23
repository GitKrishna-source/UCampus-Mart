import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class UCampusMart {
    private final MarketplaceStore store;
    private final Scanner input;
    private String student;

    private UCampusMart(MarketplaceStore store, Scanner input) {
        this.store = store;
        this.input = input;
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java UCampusMart [data-file]");
            System.exit(2);
        }
        if (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h"))) {
            System.out.println("Usage: java UCampusMart [data-file]");
            System.out.println("The default file is .ucampus-mart/marketplace.properties in your home directory.");
            return;
        }

        try (Scanner input = new Scanner(System.in)) {
            Path path = args.length == 1
                    ? Path.of(args[0])
                    : Path.of(System.getProperty("user.home"), ".ucampus-mart", "marketplace.properties");
            new UCampusMart(new MarketplaceStore(path), input).run();
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            System.exit(2);
        } catch (NoSuchElementException exception) {
            System.out.println("\nGoodbye.");
        }
    }

    private void run() {
        System.out.println("UCampus Mart");
        System.out.println("Buy, sell, and swap hostel essentials aged 12 to 24 months.");
        student = chooseStudent();
        while (!student.isEmpty()) {
            System.out.println("\nStudent | " + student);
            System.out.print("""
                    1. Browse or search listings
                    2. Post a listing
                    3. Request a purchase
                    4. Propose an exchange
                    5. My listings
                    6. My offers
                    7. Review incoming offers
                    8. Switch student
                    0. Exit
                    """);
            try {
                switch (prompt("Choose an option > ")) {
                    case "1" -> browse();
                    case "2" -> postListing();
                    case "3" -> requestPurchase();
                    case "4" -> proposeExchange();
                    case "5" -> showListings(store.load().listingsByOwner(student), "You have no listings yet.");
                    case "6" -> showMyOffers();
                    case "7" -> reviewOffers();
                    case "8" -> student = chooseStudent();
                    case "0" -> student = "";
                    default -> System.out.println("Choose a number from 0 to 8.");
                }
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
            } catch (IOException exception) {
                System.out.println("Could not access marketplace data. " + exception.getMessage());
            }
        }
        System.out.println("Goodbye.");
    }

    private String chooseStudent() {
        while (true) {
            String handle = prompt("Student handle (blank to exit) > ");
            if (handle.isEmpty()) {
                return "";
            }
            try {
                return Marketplace.studentHandle(handle);
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
            }
        }
    }

    private void browse() throws IOException {
        String query = prompt("Search by name or category (blank for all) > ");
        showListings(store.load().searchAvailable(query), "No available listings match your search.");
    }

    private void postListing() throws IOException {
        String title = prompt("Item name > ");
        String category = prompt("Category > ");
        String condition = prompt("Condition > ");
        int age = readPositiveInt("Age in months (12 to 24)");
        Listing.Mode mode = switch (prompt("1. Sale  2. Exchange  3. Both > ")) {
            case "1" -> Listing.Mode.SALE;
            case "2" -> Listing.Mode.EXCHANGE;
            case "3" -> Listing.Mode.BOTH;
            default -> throw new IllegalArgumentException("Choose 1, 2, or 3 for the listing type.");
        };
        BigDecimal price = mode.allowsSale() ? readPrice() : null;
        Listing listing = store.update(marketplace ->
                marketplace.postListing(student, title, category, condition, age, mode, price));
        System.out.printf("Posted listing #%d for %s.%n", listing.id(), listing.title());
    }

    private void requestPurchase() throws IOException {
        List<Listing> forSale = store.load().searchAvailable("").stream()
                .filter(listing -> listing.mode().allowsSale() && !listing.owner().equals(student))
                .toList();
        if (forSale.isEmpty()) {
            System.out.println("There are no items for you to buy right now.");
            return;
        }
        forSale.forEach(this::printListing);
        int id = readPositiveInt("Listing ID to buy");
        Marketplace.Offer offer = store.update(marketplace -> marketplace.requestPurchase(student, id));
        System.out.printf("Purchase request #%d sent. Check My offers for the owner's decision.%n", offer.id());
    }

    private void proposeExchange() throws IOException {
        Marketplace marketplace = store.load();
        List<Listing> mine = marketplace.listingsByOwner(student).stream()
                .filter(listing -> listing.status() == Listing.Status.AVAILABLE && listing.mode().allowsExchange())
                .toList();
        if (mine.isEmpty()) {
            System.out.println("Post an item that allows exchanges before proposing a swap.");
            return;
        }
        List<Listing> targets = marketplace.searchAvailable("").stream()
                .filter(listing -> listing.mode().allowsExchange() && !listing.owner().equals(student))
                .toList();
        if (targets.isEmpty()) {
            System.out.println("There are no other listings available for exchange.");
            return;
        }
        System.out.println("Items available to request");
        targets.forEach(this::printListing);
        int requested = readPositiveInt("Listing ID you want");
        System.out.println("Your items available to offer");
        mine.forEach(this::printListing);
        int offered = readPositiveInt("Listing ID you are offering");
        Marketplace.Offer offer = store.update(current -> current.proposeExchange(student, requested, offered));
        System.out.printf("Exchange offer #%d sent. Check My offers for the owner's decision.%n", offer.id());
    }

    private void showMyOffers() throws IOException {
        Marketplace marketplace = store.load();
        List<Marketplace.Offer> offers = marketplace.offersFor(student);
        if (offers.isEmpty()) {
            System.out.println("You have no incoming or outgoing offers.");
            return;
        }
        offers.forEach(offer -> printOffer(marketplace, offer));
    }

    private void reviewOffers() throws IOException {
        Marketplace marketplace = store.load();
        List<Marketplace.Offer> pending = marketplace.offersFor(student).stream()
                .filter(offer -> offer.status() == Marketplace.OfferStatus.PENDING
                        && marketplace.listing(offer.targetId()).owner().equals(student))
                .toList();
        if (pending.isEmpty()) {
            System.out.println("You have no pending incoming offers.");
            return;
        }
        pending.forEach(offer -> printOffer(marketplace, offer));
        int id = readPositiveInt("Offer ID");
        boolean accept = switch (prompt("1. Accept  2. Decline > ")) {
            case "1" -> true;
            case "2" -> false;
            default -> throw new IllegalArgumentException("Choose 1 to accept or 2 to decline.");
        };
        Marketplace.Offer offer = store.update(current -> current.decideOffer(student, id, accept));
        System.out.printf("Offer #%d %s.%n", offer.id(), accept ? "accepted" : "declined");
        if (accept) {
            System.out.println("Arrange pickup and any payment directly with " + offer.requester() + ".");
        }
    }

    private void showListings(List<Listing> listings, String emptyMessage) {
        if (listings.isEmpty()) {
            System.out.println(emptyMessage);
            return;
        }
        listings.forEach(this::printListing);
    }

    private void printListing(Listing listing) {
        String terms = switch (listing.mode()) {
            case SALE -> formatPrice(listing);
            case EXCHANGE -> "Exchange only";
            case BOTH -> formatPrice(listing) + " or exchange";
        };
        String status = switch (listing.status()) {
            case AVAILABLE -> "Available";
            case SOLD -> "Sold";
            case EXCHANGED -> "Exchanged";
        };
        System.out.printf("#%d %s | %s | %s | %d months old | %s | %s | %s%n",
                listing.id(), listing.title(), listing.category(), listing.condition(),
                listing.ageMonths(), terms, status, listing.owner());
    }

    private void printOffer(Marketplace marketplace, Marketplace.Offer offer) {
        Listing target = marketplace.listing(offer.targetId());
        String direction = offer.requester().equals(student)
                ? "Outgoing to " + target.owner()
                : "Incoming from " + offer.requester();
        String terms;
        if (offer.isExchange()) {
            Listing offered = marketplace.listing(offer.offeredId());
            terms = "Swap for #" + offered.id() + " " + offered.title();
        } else {
            terms = "Purchase for " + formatPrice(target);
        }
        String status = switch (offer.status()) {
            case PENDING -> "Pending";
            case ACCEPTED -> "Accepted";
            case DECLINED -> "Declined";
            case ITEM_UNAVAILABLE -> "Item unavailable";
        };
        System.out.printf("Offer #%d | %s | #%d %s | %s | %s%n",
                offer.id(), direction, target.id(), target.title(), terms, status);
    }

    private static String formatPrice(Listing listing) {
        return "INR " + listing.price().toPlainString();
    }

    private int readPositiveInt(String label) {
        int number;
        try {
            number = Integer.parseInt(prompt(label + " > "));
        } catch (NumberFormatException exception) {
            number = 0;
        }
        if (number <= 0) {
            throw new IllegalArgumentException(label + " must be a positive whole number.");
        }
        return number;
    }

    private BigDecimal readPrice() {
        String amount = prompt("Price in INR > ");
        if (!amount.matches("[0-9]+(\\.[0-9]{1,2})?|\\.[0-9]{1,2}")) {
            throw new IllegalArgumentException("Enter a price such as 350 or 350.50.");
        }
        return new BigDecimal(amount);
    }

    private String prompt(String message) {
        System.out.print(message);
        System.out.flush();
        return input.nextLine().strip();
    }
}
