# Play Store Listing — Draft Content

Starting-point drafts, not final legal text. You (not me) are accountable for
Data Safety accuracy and the Privacy Policy — review against what the
*backend* actually does with this data too, since the form covers the whole
service, not just the Android client.

## Data Safety form

Based on `data/model/Models.kt` and what the app actually sends over the wire.

**Data collected:**

| Category | Type | Collected? | Notes |
|---|---|---|---|
| Personal info | Name | Yes | Account name, delivery address name |
| Personal info | Email address | Yes | Account/login |
| Personal info | Phone number | Yes | Account + delivery contact |
| Personal info | Address | Yes | Delivery addresses |
| Location | Approximate location | Likely | `latitude`/`longitude` saved with an address (geocoded from the typed address, not raw GPS — confirm how your backend actually derives this before answering "precise" vs "approximate") |
| Financial info | Purchase history | Yes | Orders |
| Financial info | Payment info | Via third party | Card/UPI details go straight to Razorpay's SDK; the app itself only sees an order ID/amount, not raw payment credentials — disclose Razorpay as a data processor |
| App activity | — | No | No analytics/crash SDK in the dependency graph (`libs.firebase.auth` only, no Firebase Analytics/Crashlytics) |
| Device/other IDs | — | Minimal | Firebase Auth session handling only |

**Is data encrypted in transit?** Yes — HTTPS only in release (`TRUST_ALL_CERTS = false`), confirm your backend enforces TLS.

**Can users request data deletion?** Confirm with backend — if there's no account-deletion endpoint yet, you'll need one before you can answer "yes" here, and Play increasingly expects it.

**Purpose for each data type:** App functionality (account, delivery, orders) — none of this reads as "advertising or marketing" or "personalization" from what's in the codebase.

## Store listing copy (draft)

**App name:** Sanathana Tattva

**Short description** (80 char max, currently 76):
> Cold-pressed oils & ghee in returnable steel containers. Reorder in a tap.

**Full description** (draft — edit freely). Pulled the "why steel" reasons
straight from the app's own onboarding copy (`SteelStorySheet.kt`) rather than
inventing new claims — it's stronger and it's consistent with what a user
sees five minutes after installing:

> Sanathana Tattva is a specialty storefront for traditional cold-pressed oils and ghee, sold the old way: in returnable steel containers with a fully refundable deposit.
>
> **Why steel, not plastic:**
> - Sunlight ruins good oil — steel blocks every ray, plastic lets it through
> - Oxygen slips through plastic walls in weeks — steel is sealed tight
> - Oil slowly pulls chemicals out of plastic — steel adds nothing at all
> - Plastic holds old odours and passes them to the next oil — steel stays neutral
>
> **Reorder in under a minute.** Refill a container you already hold and skip the deposit entirely, or start fresh with a new one. Every rupee on screen is labeled — product price, deposit, and wallet credit are never bundled together, so you always know exactly what you're paying for.
>
> - Cold-pressed oils and ghee, traditionally made
> - Returnable steel containers with a fully refundable deposit
> - One-tap refills once you hold a container — no deposit, no hassle
> - Track every container you hold, swap, or return
> - Secure checkout via Razorpay

**What's new (v1.0 release notes):**
> First release. Browse cold-pressed oils and ghee, buy or refill returnable steel containers, and track deposits, orders, and containers in one place.

**Category suggestion:** Shopping (or Food & Drink, depending on how Play classifies grocery/specialty-food storefronts — check both descriptions before picking).

## Privacy Policy

**Using your existing website privacy policy URL** — one thing worth a quick check before you paste it into Play Console: that policy was presumably written for the website flow. Skim it against the list below and add anything missing, since Play's Data Safety form gets cross-checked against the policy text, not just your form answers:

1. Delivery **address** collection, including the approximate location geocoded from it (the website may not do this the same way the app does).
2. **Firebase** as the auth provider (Google Sign-In token exchange) — a mobile-specific detail that may not be in the web policy if the web app authenticates differently.
3. **Razorpay** as the payment processor (probably already covered if the website also uses Razorpay).
4. Data retention / deletion process — same for both surfaces, but confirm it's stated.

If all of that's already in there, you're done — just paste the URL into **App content → Privacy policy** in Play Console.

## Assets

- **Hi-res icon (512×512):** done — `store-assets/play-store-icon-512.png`. Also replaced the app's actual launcher icon (was the default Android Studio placeholder) with the same design, in `app/src/main/res/drawable/ic_launcher_*.xml` and all `mipmap-*` densities.
- **Feature graphic (1024×500):** done — `store-assets/play-store-feature-graphic-1024x500.png`.
- **Phone screenshots (min. 2, Play requires at least 2):** 3 captured before this was paused, already above the minimum — `store-assets/screenshots/1-shop.png` (product listing), `2-product-detail.png` (deposit/return explanation), `3-steel-story.png` (the "why steel" onboarding story — genuinely distinctive, worth leading with). More can be added later; these are enough to submit.
