# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Sanathana Tattva is a single-module Android consumer shopping app (Kotlin, Jetpack Compose, Material 3) — the Android counterpart of an existing web app. It talks to a REST backend and uses Razorpay for payments. Domain specifics: products are sold in returnable containers with deposits; a "refill" purchase reuses a container the customer already holds (no deposit charged), and containers can be swapped, disputed, or refunded.

## Commands

```bash
./gradlew assembleDebug            # build debug APK
./gradlew installDebug             # build + install on connected device/emulator
./gradlew test                     # JVM unit tests
./gradlew connectedAndroidTest     # instrumented tests (needs device/emulator)
./gradlew lint                     # Android lint
# Single unit test class:
./gradlew testDebugUnitTest --tests "com.example.sanathanatattva.ExampleUnitTest"
```

## Backend environments

`BASE_URL` and `TRUST_ALL_CERTS` are BuildConfig fields set per build type in `app/build.gradle.kts`:
- **debug**: the Railway-hosted backend (`https://sanathanatattava-production-67b1.up.railway.app`), valid TLS cert, no trust-all. For a local backend use `https://10.0.2.2:5001` with `TRUST_ALL_CERTS = true`.
- **release**: currently also the Railway backend, normal cert validation. The vanity domain `api.sanathanatattva.shop` is **not yet provisioned** (returns NXDOMAIN) — switch release back to it only once its DNS/cert are live.

Backend error responses have the shape `{ "error": "..." }`.

## Architecture

No Hilt/Koin — manual DI via `AppContainer` in `App.kt`, holding the three app-wide singletons: `TokenStore`, `CartStore`, and the Retrofit `ApiService`. Composables reach it through `appContainer()` (`ui/AppRoot.kt`) and pass it into ViewModels created with the `viewModel { }` factory lambda.

**Data layer** (`data/`) — no repository layer; ViewModels call `ApiService` directly:
- `ApiService`: single Retrofit interface for all endpoints (auth, profile, products, containers, wallet, addresses, orders, payments). kotlinx.serialization models live in `data/model/Models.kt` and use snake_case property names matching the API JSON.
- `apiCall {}` (`data/ApiResult.kt`): wraps every API call, returning `Result<T>` with user-facing error messages parsed from the backend's error shape. Always use it instead of raw try/catch.
- `ApiClient`: OkHttp interceptor injects `Authorization: Bearer <token>` from `TokenStore` on every request.
- `TokenStore` / `CartStore`: DataStore-backed persistence. The cart is fully client-side (mirrors the web app's localStorage cart); a cart line is keyed by `(productId, isRefill)` — buy and refill of the same product are separate lines.

**UI layer** (`ui/`) — single-activity Compose navigation:
- `AppRoot.kt` owns the `NavHost` and bottom bar; all routes are string constants in `ui/navigation/Routes.kt`. Screens live in `ui/screens/<feature>/` as `XxxScreen.kt` + `XxxViewModel.kt` pairs.
- `SessionViewModel` is activity-scoped and shared across screens (passed as a parameter, not re-created): login state derives from token presence, `isLoggedIn` is `StateFlow<Boolean?>` where `null` means still loading. Guests can browse; checkout/account require login. A 401/403 on profile refresh triggers automatic logout.

**Payment flow** (checkout): `CheckoutViewModel.placeOrderAndPay()` → `POST /orders` creates an unpaid order → `POST /payments/create-order` → screen opens Razorpay SDK → Razorpay result arrives at `MainActivity` (via `PaymentResultWithDataListener`) and is bridged through the `PaymentEvents` shared flow back to the ViewModel → `POST /payments/verify` confirms → cart cleared. On failure/cancel, the unpaid order is deleted (`deleteUnpaidOrder`) to release stock. Client totals in `estimate()` are estimates only — the backend recomputes authoritatively.

**Auth flow**: register/login can lead to `VERIFY_PENDING` (email OTP); the pending email is carried in `SessionViewModel.pendingVerificationEmail`.

## Conventions

- Backend image paths are relative (`/uploads/x.jpg`); resolve with the top-level `imageUrl()` helper in `App.kt`.
- Currency formatting helpers are in `util/Money.kt`; shared loading/error/empty composables in `ui/components/States.kt`.
- Release build has R8 optimization disabled; keep rules live in `app/src/main/keepRules/rules.keep`.

## Design Context

Strategic design context (users, brand personality, principles) lives in `PRODUCT.md`; the visual system spec (colors, typography, components, named rules like "gold means money") lives in `DESIGN.md`. Read both before any UI design work.
