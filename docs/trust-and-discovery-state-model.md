# StudioOS Trust and Discovery State Model

These concepts are intentionally independent. One state must never be inferred from another.

## Verification

Platform trust and identity review for a user or studio:

- `UNVERIFIED`: no StudioOS verification has been completed.
- `PENDING_REVIEW`: verification was requested and awaits review.
- `VERIFIED`: StudioOS approved the account or listing and the blue badge may be shown.
- `REJECTED`: the request was reviewed and declined.

Email or phone ownership verification is an authentication requirement, not a blue badge.

## Availability

Whether a creator is currently accepting work:

- `AVAILABLE`: open to relevant work or bookings.
- `AWAY`: temporarily not accepting new work.
- `UNAVAILABLE`: not accepting work until changed by the owner.

For the current boolean implementation, `true` maps to `AVAILABLE` and `false` maps to `UNAVAILABLE`.
The producer is the source of truth for owned studio availability.

## Ratings

Quality signals calculated only from eligible completed services:

- average rating
- review count

Ratings do not grant verification and verification does not imply a rating.

## Popularity

Engagement signals calculated server-side from events such as followers, saves, profile views,
bookings, and completed services. Popularity must not be accepted as a client-supplied number.

## Editorial Discovery

- `FEATURED`: selected by StudioOS staff or an approved editorial rule.
- `TRENDING`: calculated from recent activity within a defined time window.

Featured and trending labels are discovery placements, not trust or quality guarantees.

## Display Rules

Cards may display multiple independent signals:

- blue tick: `VERIFIED`
- rating and reviews: eligible completed-service reviews
- popularity: server-calculated engagement metric
- availability: creator-controlled status
- featured/trending: discovery placement

The API should expose these as separate fields so clients do not derive one from another.
