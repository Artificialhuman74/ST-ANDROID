# Bhagya Chakra on Android

The consumer lucky-draw flow uses the existing web backend. The app does not run
draws or calculate winning odds locally. Trader and admin management remain in
the web portal, consistent with this app's consumer authentication.

Entry points: the signed-in Shop card (when a round is open or enrolled),
Account → Bhagya Chakra, and reward containers → View rewards and returns.

Supported flows:

- Open-round selection, server-provided joining fees and draw schedule, optional welcome gift.
- Permanent trader linking with confirmation, saved-address prerequisite, Razorpay enrollment.
- Backend signature verification, confirmation retry, and reconciliation after interrupted payments.
- Monthly eligibility, loyalty entry count, account-specific result acknowledgment and recent winners.
- Monthly ₹0 reward claims, including month twelve, delivered/ordered state, and order tracking.
- Checkout qualification information, welcome/reward/guarantee order badges, reward-container returns,
  and confirmed withdrawal. The backend creates the month-thirteen guarantee order.

## Home card and motion

The Shop tab shows one card, chosen by `homeCardFor` (`DrawHomeState.kt`) in the web's order:
result waiting → winner → someone else won (until "Got it") → enrolled row → promotional banner.
Anyone not in a round (signed out, no round open, still loading, or an unfinished joining payment)
sees the banner, whose button follows their next step. Once-per-draw moments (reveal watched, result dismissed,
the "entered" gold pass) are remembered per account, scheme and month in `LuckyDrawPrefsStore`.

The round page loads behind the cart wheel (`ChakraWheel.kt`): it spins, brakes onto a spoke with a
velocity-matched settle, then flies to the hero mark. A new draw plays `WinMoment` full screen once:
the web's 13.6s six-beat reveal, plus the winner-only Crowning (shockwaves, oil-drop fountain, rays,
haptic triple-thud). Tap or Back skips to the settled frame; system animations off skips it entirely.
Motion spec: https://claude.ai/artifact/3ApGb69Yumq5WNn8Xyojrb

API paths are under `/api/lucky-draw/consumer/`: `status`, `welcome-gifts`,
`recent-winners`, `enroll`, `enroll/verify`, `reward-order`, and `leave`.
Trader linking uses `/api/consumer/link-dealer`. All use the existing consumer bearer token.

The current web implementation uses the configured draw time as the purchase
deadline. The older feature specification's hardcoded tenth-of-the-month cutoff
is not used. Dates are interpreted as UTC if the backend omits an offset and
displayed in IST on every device.

## Verification

`./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`

The lucky-draw unit tests exercise payment success/cancellation/recovery,
duplicate-action prevention, final-month claims, account-scoped reveals,
prerequisites, JSON contracts, order classification, and timezone boundaries.

`./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.sanathanatattva.LuckyDrawUiTest`

Device UI tests use fixtures and do not contact the backend or start payments.
For a complete payment-provider smoke test, use a test account on a backend with
Razorpay test keys: link a trader, save an address, join with and without the gift,
cancel/retry payment, confirm the same enrollment on web, run a test draw from the
web admin, then claim and track a reward. Never run the backend's force-win/reset
tools against a real customer's enrollment.
