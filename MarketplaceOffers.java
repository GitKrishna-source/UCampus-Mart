import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class MarketplaceOffers {
    private final Marketplace marketplace;
    private final Map<Integer, Marketplace.Offer> offers = new LinkedHashMap<>();
    private int lastOfferId;

    MarketplaceOffers(Marketplace marketplace, List<Marketplace.Offer> savedOffers) {
        this.marketplace = marketplace;
        for (Marketplace.Offer offer : savedOffers) {
            marketplace.listing(offer.targetId());
            if (offer.isExchange()) {
                marketplace.listing(offer.offeredId());
            }
            if (offers.putIfAbsent(offer.id(), offer) != null) {
                throw new IllegalArgumentException("Duplicate offer ID " + offer.id() + ".");
            }
            lastOfferId = Math.max(lastOfferId, offer.id());
        }
    }

    List<Marketplace.Offer> forStudent(String student) {
        String handle = Marketplace.studentHandle(student);
        return offers.values().stream()
                .filter(offer -> offer.requester().equals(handle)
                        || marketplace.listing(offer.targetId()).owner().equals(handle))
                .toList();
    }

    Marketplace.Offer submit(String student, int listingId, Integer offeredListingId) {
        String requester = Marketplace.studentHandle(student);
        Listing target = marketplace.availableListing(listingId);
        if (target.owner().equals(requester)) {
            throw new IllegalArgumentException("You already own this listing.");
        }
        if (offeredListingId == null) {
            if (!target.mode().allowsSale()) {
                throw new IllegalArgumentException("This item is available for exchange only.");
            }
        } else {
            Listing offered = marketplace.availableListing(offeredListingId);
            if (!offered.owner().equals(requester)) {
                throw new IllegalArgumentException("Choose one of your own listings to offer.");
            }
            if (!target.mode().allowsExchange() || !offered.mode().allowsExchange()) {
                throw new IllegalArgumentException("Both listings must allow exchanges.");
            }
        }
        boolean duplicate = offers.values().stream()
                .anyMatch(offer -> offer.status() == Marketplace.OfferStatus.PENDING
                        && offer.requester().equals(requester)
                        && offer.targetId() == listingId
                        && Objects.equals(offer.offeredId(), offeredListingId));
        if (duplicate) {
            throw new IllegalArgumentException("You already have this offer pending.");
        }
        Marketplace.Offer offer = new Marketplace.Offer(lastOfferId + 1, requester, listingId,
                offeredListingId, Marketplace.OfferStatus.PENDING);
        offers.put(offer.id(), offer);
        lastOfferId = offer.id();
        return offer;
    }

    Marketplace.Offer decide(String student, int offerId, boolean accept) {
        Marketplace.Offer offer = offers.get(offerId);
        if (offer == null
                || !marketplace.listing(offer.targetId()).owner().equals(Marketplace.studentHandle(student))) {
            throw new IllegalArgumentException("Choose an offer sent to one of your listings.");
        }
        if (offer.status() != Marketplace.OfferStatus.PENDING) {
            throw new IllegalArgumentException("This offer has already been resolved.");
        }
        if (!accept) {
            Marketplace.Offer declined = offer.withStatus(Marketplace.OfferStatus.DECLINED);
            offers.put(offer.id(), declined);
            return declined;
        }

        marketplace.completeListings(offer.targetId(), offer.offeredId());

        Marketplace.Offer accepted = offer.withStatus(Marketplace.OfferStatus.ACCEPTED);
        offers.put(offer.id(), accepted);
        offers.replaceAll((id, pending) -> {
            boolean taken = pending.involves(offer.targetId())
                    || (offer.isExchange() && pending.involves(offer.offeredId()));
            if (pending.status() == Marketplace.OfferStatus.PENDING && taken) {
                return pending.withStatus(Marketplace.OfferStatus.ITEM_UNAVAILABLE);
            }
            return pending;
        });
        return accepted;
    }

    List<Marketplace.Offer> all() {
        return List.copyOf(offers.values());
    }
}
