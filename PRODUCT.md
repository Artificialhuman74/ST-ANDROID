# Product

## Register

product

## Users

Households reordering everyday staples (oils, grains, household goods) sold in returnable deposit containers. The core loop is the repeat "refill" reorder: fast, familiar, low-friction. The audience skews toward older and second-language readers shopping in bright kitchens on mid-range Android phones — hence the light-only theme, 16sp body floor, and plain-spoken copy. New customers also need the container-deposit / refill model explained before they trust it, but onboarding education must never slow down the repeat buyer.

## Product Purpose

Sanathana Tattva is the Android storefront for a shop selling traditional products in returnable containers with deposits. Success looks like: a returning customer reorders their usual refills in under a minute, understands exactly what they're paying (product vs. deposit vs. wallet credit), and trusts the container/deposit ledger completely. Money and deposits are the trust surface — clarity there is the product.

## Brand Personality

Premium artisanal: small-batch, crafted quality expressed through quiet confidence and restraint — not luxury pricing signals. Three words: **calm, honest, crafted.** The feel of a trusted specialty store, not a marketplace. Reference qualities (not styles to copy):

- **Apple Store app** — quiet confidence: few items per screen, imagery does the talking, zero promotional noise.
- **Farmizen / BB Daily** — warm, functional repeat-ordering; the app knows what you usually buy.

## Anti-references

- **Cold marketplace UI** (Amazon-style): dense utilitarian listings, badge clutter, no warmth or story. This app should feel like a shop with a point of view, not a catalog.
- Urgency mechanics of any kind: countdown timers, "only 2 left!", promo-badge overload.
- Eco-cliché decoration: leaf iconography scattered everywhere, kraft-paper textures, greenwashing copy. The green palette carries the brand; it doesn't need illustration.

## Design Principles

1. **The refill is the hero.** The repeat-order path gets first-class treatment on every surface; discovery and education never obstruct it.
2. **Money is never ambiguous.** Every rupee on screen is labeled — product price, deposit, wallet, cashback. Gold is reserved exclusively for money moments; if gold appears, it means money.
3. **Restraint reads as quality.** Fewer elements per screen, generous spacing, weight-driven hierarchy in one type family. No decoration that doesn't inform.
4. **Earned familiarity over novelty.** Standard Material 3 affordances everywhere; the brand shows in palette, spacing, and tone — not invented controls.
5. **Legible for everyone.** WCAG AA contrast (≥4.5:1 body), ≥48dp touch targets, system font scaling respected, reduced-motion honored. Designed for older, second-language readers first.

## Accessibility & Inclusion

WCAG AA. Known audience needs: older readers and second-language English readers — 16sp body-size floor (already in `Type.kt`), simple sentence-length labels, no jargon in money/deposit copy. Touch targets ≥48dp. Respect Android system font scaling (sp everywhere, layouts must survive 1.3× scale). Honor system reduced-motion / animator-scale settings. Color is never the only signal (deposits, errors, and states also carry text/icons).
