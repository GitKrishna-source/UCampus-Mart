import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

final class MarketplaceMenuActions {
    private final MarketplaceStore store;
    private final ConsoleInputOutput console;

    MarketplaceMenuActions(MarketplaceStore store, ConsoleInputOutput console) {
        this.store = store;
        this.console = console;
    }

    void browse() throws IOException {
        String query = console.prompt("Search by name or category (blank for all) > ");
        console.showListings(store.load().searchAvailable(query), "No available listings match your search.");
    }

    void postListing(String student) throws IOException {
        String title = console.prompt("Item name > ");
        String category = console.prompt("Category > ");
        String condition = console.prompt("Condition > ");
        int age = console.readPositiveInt("Age in months (" + Listing.AGE_RANGE + ")");
        Listing.Mode mode = switch (console.prompt("1. Sale  2. Exchange  3. Both > ")) {
            case "1" -> Listing.Mode.SALE;
            case "2" -> Listing.Mode.EXCHANGE;
            case "3" -> Listing.Mode.BOTH;
            default -> throw new IllegalArgumentException("Choose 1, 2, or 3 for the listing type.");
        };
        BigDecimal price = mode.allowsSale() ? console.readPrice() : null;
        Listing listing = store.update(marketplace ->
                marketplace.postListing(student, title, category, condition, age, mode, price));
        System.out.printf("Posted listing #%d for %s.%n", listing.id(), listing.title());
    }

    void requestPurchase(String student) throws IOException {
        List<Listing> forSale = store.load().searchAvailable("").stream()
                .filter(listing -> listing.mode().allowsSale() && !listing.owner().equals(student))
                .toList();
        if (forSale.isEmpty()) {
            System.out.println("There are no items for you to buy right now.");
            return;
        }
        forSale.forEach(console::printListing);
        int id = console.readPositiveInt("Listing ID to buy");
        Marketplace.Offer offer = store.update(marketplace -> marketplace.requestPurchase(student, id));
        System.out.printf("Purchase request #%d sent. Check My offers for the owner's decision.%n", offer.id());
    }

    void proposeExchange(String student) throws IOException {
        Marketplace marketplace = store.load();
        List<Listing> mine = marketplace.listingsByOwner(student).stream()
                .filter(listing -> listing.isAvailable() && listing.mode().allowsExchange())
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
        targets.forEach(console::printListing);
        int requested = console.readPositiveInt("Listing ID you want");
        System.out.println("Your items available to offer");
        mine.forEach(console::printListing);
        int offered = console.readPositiveInt("Listing ID you are offering");
        Marketplace.Offer offer = store.update(current -> current.proposeExchange(student, requested, offered));
        System.out.printf("Exchange offer #%d sent. Check My offers for the owner's decision.%n", offer.id());
    }

    void showMyListings(String student) throws IOException {
        console.showListings(store.load().listingsByOwner(student), "You have no listings yet.");
    }

    void removeListing(String student) throws IOException {
        List<Listing> available = store.load().listingsByOwner(student).stream()
                .filter(Listing::isAvailable)
                .toList();
        if (available.isEmpty()) {
            System.out.println("You have no available listings to remove.");
            return;
        }
        available.forEach(console::printListing);
        int id = console.readPositiveInt("Listing ID to remove");
        Listing removed = store.update(marketplace -> marketplace.removeListing(student, id));
        System.out.printf("Removed listing #%d for %s.%n", removed.id(), removed.title());
    }

    void showMyOffers(String student) throws IOException {
        Marketplace marketplace = store.load();
        List<Marketplace.Offer> offers = marketplace.offersFor(student);
        if (offers.isEmpty()) {
            System.out.println("You have no incoming or outgoing offers.");
            return;
        }
        System.out.printf("Total offers (%d):%n", offers.size());
        offers.forEach(offer -> console.printOffer(student, marketplace, offer));
    }

    void reviewOffers(String student) throws IOException {
        Marketplace marketplace = store.load();
        List<Marketplace.Offer> pending = marketplace.offersFor(student).stream()
                .filter(offer -> offer.status() == Marketplace.OfferStatus.PENDING
                        && marketplace.listing(offer.targetId()).owner().equals(student))
                .toList();
        if (pending.isEmpty()) {
            System.out.println("You have no pending incoming offers.");
            return;
        }
        System.out.printf("Pending incoming offers (%d):%n", pending.size());
        pending.forEach(offer -> console.printOffer(student, marketplace, offer));
        int id = console.readPositiveInt("Offer ID");
        boolean accept = switch (console.prompt("1. Accept  2. Decline > ")) {
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
}
