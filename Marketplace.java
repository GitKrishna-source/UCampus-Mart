import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Marketplace {
    public enum OfferStatus {
        PENDING, ACCEPTED, DECLINED, ITEM_UNAVAILABLE
    }

    public record Offer(int id, String requester, int targetId, Integer offeredId, OfferStatus status) {
        public Offer {
            if (id < 1 || targetId < 1 || (offeredId != null && (offeredId < 1 || offeredId == targetId))) {
                throw new IllegalArgumentException("Invalid listing or offer ID.");
            }
            requester = studentHandle(requester);
            if (status == null) {
                throw new IllegalArgumentException("Offer status is required.");
            }
        }

        public boolean isExchange() {
            return offeredId != null;
        }

        boolean involves(int listingId) {
            return targetId == listingId || (offeredId != null && offeredId == listingId);
        }

        Offer withStatus(OfferStatus result) {
            return new Offer(id, requester, targetId, offeredId, result);
        }
    }

    private final Map<Integer, Listing> listings = new LinkedHashMap<>();
    private final MarketplaceOffers offers;
    private int lastListingId;

    public Marketplace() {
        this(List.of(), List.of());
    }

    Marketplace(List<Listing> savedListings, List<Offer> savedOffers) {
        for (Listing listing : savedListings) {
            if (listings.putIfAbsent(listing.id(), listing) != null) {
                throw new IllegalArgumentException("Duplicate listing ID " + listing.id() + ".");
            }
            lastListingId = Math.max(lastListingId, listing.id());
        }
        offers = new MarketplaceOffers(this, savedOffers);
    }

    public static String studentHandle(String input) {
        String handle = Listing.required(input, "Student handle");
        if (!handle.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,23}")) {
            throw new IllegalArgumentException("Use 1 to 24 letters, numbers, underscores or hyphens. "
                    + "Start with a letter or number.");
        }
        return handle.toLowerCase(Locale.ROOT);
    }

    public Listing postListing(String owner, String title, String category, String condition,
                               int ageMonths, Listing.Mode mode, BigDecimal price) {
        Listing listing = new Listing(lastListingId + 1, studentHandle(owner), title, category,
                condition, ageMonths, mode, price, Listing.Status.AVAILABLE);
        listings.put(listing.id(), listing);
        lastListingId = listing.id();
        return listing;
    }

    public List<Listing> searchAvailable(String query) {
        String text = query.strip().toLowerCase(Locale.ROOT);
        return listings.values().stream()
                .filter(Listing::isAvailable)
                .filter(listing -> listing.title().toLowerCase(Locale.ROOT).contains(text)
                        || listing.category().toLowerCase(Locale.ROOT).contains(text))
                .toList();
    }

    public List<Listing> listingsByOwner(String student) {
        String owner = studentHandle(student);
        return listings.values().stream().filter(listing -> listing.owner().equals(owner)).toList();
    }

    public Listing listing(int id) {
        Listing listing = listings.get(id);
        if (listing == null) {
            throw new IllegalArgumentException("Listing #" + id + " was not found.");
        }
        return listing;
    }

    public List<Offer> offersFor(String student) {
        return offers.forStudent(student);
    }

    public Offer requestPurchase(String student, int listingId) {
        return offers.submit(student, listingId, null);
    }

    public Offer proposeExchange(String student, int listingId, int offeredListingId) {
        return offers.submit(student, listingId, offeredListingId);
    }

    public Offer decideOffer(String student, int offerId, boolean accept) {
        return offers.decide(student, offerId, accept);
    }

    void completeListings(int targetId, Integer offeredId) {
        Listing target = availableListing(targetId);
        if (offeredId == null) {
            listings.put(targetId, target.complete(Listing.Status.SOLD));
            return;
        }

        Listing offered = availableListing(offeredId);
        Listing completedTarget = target.complete(Listing.Status.EXCHANGED);
        Listing completedOffered = offered.complete(Listing.Status.EXCHANGED);
        listings.put(targetId, completedTarget);
        listings.put(offeredId, completedOffered);
    }

    Listing availableListing(int id) {
        Listing listing = listing(id);
        if (!listing.isAvailable()) {
            throw new IllegalArgumentException("Listing #" + id + " is no longer available.");
        }
        return listing;
    }

    List<Listing> allListings() {
        return List.copyOf(listings.values());
    }

    List<Offer> allOffers() {
        return offers.all();
    }
}
