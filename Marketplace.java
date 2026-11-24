import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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

        private boolean involves(int listingId) {
            return targetId == listingId || (offeredId != null && offeredId == listingId);
        }

        private Offer withStatus(OfferStatus result) {
            return new Offer(id, requester, targetId, offeredId, result);
        }
    }

    private final Map<Integer, Listing> listings = new LinkedHashMap<>();
    private final Map<Integer, Offer> offers = new LinkedHashMap<>();
    private int lastListingId;
    private int lastOfferId;

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
        for (Offer offer : savedOffers) {
            listing(offer.targetId());
            if (offer.isExchange()) {
                listing(offer.offeredId());
            }
            if (offers.putIfAbsent(offer.id(), offer) != null) {
                throw new IllegalArgumentException("Duplicate offer ID " + offer.id() + ".");
            }
            lastOfferId = Math.max(lastOfferId, offer.id());
        }
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
        String handle = studentHandle(student);
        return offers.values().stream()
                .filter(offer -> offer.requester().equals(handle) || listing(offer.targetId()).owner().equals(handle))
                .toList();
    }

    public Offer requestPurchase(String student, int listingId) {
        return submitOffer(student, listingId, null);
    }

    public Offer proposeExchange(String student, int listingId, int offeredListingId) {
        return submitOffer(student, listingId, offeredListingId);
    }

    private Offer submitOffer(String student, int listingId, Integer offeredListingId) {
        String requester = studentHandle(student);
        Listing target = availableListing(listingId);
        if (target.owner().equals(requester)) {
            throw new IllegalArgumentException("You already own this listing.");
        }
        if (offeredListingId == null) {
            if (!target.mode().allowsSale()) {
                throw new IllegalArgumentException("This item is available for exchange only.");
            }
        } else {
            Listing offered = availableListing(offeredListingId);
            if (!offered.owner().equals(requester)) {
                throw new IllegalArgumentException("Choose one of your own listings to offer.");
            }
            if (!target.mode().allowsExchange() || !offered.mode().allowsExchange()) {
                throw new IllegalArgumentException("Both listings must allow exchanges.");
            }
        }
        boolean duplicate = offers.values().stream()
                .anyMatch(offer -> offer.status() == OfferStatus.PENDING && offer.requester().equals(requester)
                        && offer.targetId() == listingId && Objects.equals(offer.offeredId(), offeredListingId));
        if (duplicate) {
            throw new IllegalArgumentException("You already have this offer pending.");
        }
        Offer offer = new Offer(lastOfferId + 1, requester, listingId, offeredListingId, OfferStatus.PENDING);
        offers.put(offer.id(), offer);
        lastOfferId = offer.id();
        return offer;
    }

    public Offer decideOffer(String student, int offerId, boolean accept) {
        Offer offer = offers.get(offerId);
        if (offer == null || !listing(offer.targetId()).owner().equals(studentHandle(student))) {
            throw new IllegalArgumentException("Choose an offer sent to one of your listings.");
        }
        if (offer.status() != OfferStatus.PENDING) {
            throw new IllegalArgumentException("This offer has already been resolved.");
        }
        if (!accept) {
            Offer declined = offer.withStatus(OfferStatus.DECLINED);
            offers.put(offer.id(), declined);
            return declined;
        }

        Listing target = availableListing(offer.targetId());
        if (offer.isExchange()) {
            Listing offered = availableListing(offer.offeredId());
            Listing completedTarget = target.complete(Listing.Status.EXCHANGED);
            Listing completedOffered = offered.complete(Listing.Status.EXCHANGED);
            listings.put(completedTarget.id(), completedTarget);
            listings.put(completedOffered.id(), completedOffered);
        } else {
            listings.put(target.id(), target.complete(Listing.Status.SOLD));
        }

        Offer accepted = offer.withStatus(OfferStatus.ACCEPTED);
        offers.put(offer.id(), accepted);
        offers.replaceAll((id, pending) -> {
            boolean taken = pending.involves(offer.targetId())
                    || (offer.isExchange() && pending.involves(offer.offeredId()));
            if (pending.status() == OfferStatus.PENDING && taken) {
                return pending.withStatus(OfferStatus.ITEM_UNAVAILABLE);
            }
            return pending;
        });
        return accepted;
    }

    private Listing availableListing(int id) {
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
        return List.copyOf(offers.values());
    }
}
