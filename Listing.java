import java.math.BigDecimal;
import java.math.RoundingMode;

public record Listing(
        int id,
        String owner,
        String title,
        String category,
        String condition,
        int ageMonths,
        Mode mode,
        BigDecimal price,
        Status status) {

    public enum Mode {
        SALE, EXCHANGE, BOTH;

        public boolean allowsSale() {
            return this == SALE || this == BOTH;
        }

        public boolean allowsExchange() {
            return this == EXCHANGE || this == BOTH;
        }
    }

    public enum Status {
        AVAILABLE, SOLD, EXCHANGED, REMOVED
    }

    public Listing {
        if (id < 1) {
            throw new IllegalArgumentException("Listing ID must be positive.");
        }
        owner = required(owner, "Owner");
        title = required(title, "Item name");
        category = required(category, "Category");
        condition = required(condition, "Condition");
        if (ageMonths < 12 || ageMonths > 24) {
            throw new IllegalArgumentException("Items must be 12 to 24 months old.");
        }
        if (mode == null || status == null) {
            throw new IllegalArgumentException("Listing mode and status are required.");
        }
        if (mode.allowsSale()) {
            price = salePrice(price);
        } else if (price != null) {
            throw new IllegalArgumentException("Exchange-only listings cannot have a sale price.");
        }
        if (status == Status.SOLD && !mode.allowsSale()) {
            throw new IllegalArgumentException("This listing does not allow a sale.");
        }
        if (status == Status.EXCHANGED && !mode.allowsExchange()) {
            throw new IllegalArgumentException("This listing does not allow an exchange.");
        }
    }

    public boolean isAvailable() {
        return status == Status.AVAILABLE;
    }

    public boolean isSold() {
        return status == Status.SOLD;
    }

    public boolean isExchanged() {
        return status == Status.EXCHANGED;
    }

    public boolean canBeCompleted() {
        return isAvailable();
    }

    public Listing complete(Status result) {
        if (!isAvailable()) {
            throw new IllegalArgumentException("This item is no longer available.");
        }
        if (result != Status.SOLD && result != Status.EXCHANGED) {
            throw new IllegalArgumentException("Choose a completed listing status.");
        }
        return withStatus(result);
    }

    Listing withStatus(Status result) {
        return new Listing(id, owner, title, category, condition, ageMonths, mode, price, result);
    }

    private static BigDecimal salePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Enter a price greater than zero.");
        }
        try {
            return price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Use at most two decimal places for the price.");
        }
    }

    static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.strip();
    }
}
