import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

final class ConsoleInputOutput {
    private final Scanner input;

    ConsoleInputOutput(Scanner input) {
        this.input = input;
    }

    String chooseStudent() {
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

    void showListings(List<Listing> listings, String emptyMessage) {
        if (listings.isEmpty()) {
            System.out.println(emptyMessage);
            return;
        }
        System.out.printf("Total listings (%d):%n", listings.size());
        listings.forEach(this::printListing);
    }

    void printListing(Listing listing) {
        String terms = switch (listing.mode()) {
            case SALE -> formatPrice(listing);
            case EXCHANGE -> "Exchange only";
            case BOTH -> formatPrice(listing) + " or exchange";
        };
        String status = switch (listing.status()) {
            case AVAILABLE -> "Available";
            case SOLD -> "Sold";
            case EXCHANGED -> "Exchanged";
            case REMOVED -> "Removed";
        };
        System.out.printf("#%d %s | %s | %s | %d months old | %s | %s | %s%n",
                listing.id(), listing.title(), listing.category(), listing.condition(),
                listing.ageMonths(), terms, status, listing.owner());
    }

    void printOffer(String student, Marketplace marketplace, Marketplace.Offer offer) {
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

    int readPositiveInt(String label) {
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

    BigDecimal readPrice() {
        String amount = prompt("Price in INR > ");
        if (!amount.matches("[0-9]+(\\.[0-9]{1,2})?|\\.[0-9]{1,2}")) {
            throw new IllegalArgumentException("Enter a price such as 350 or 350.50.");
        }
        return new BigDecimal(amount);
    }

    String prompt(String message) {
        System.out.print(message);
        System.out.flush();
        return input.nextLine().strip();
    }
}
