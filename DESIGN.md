---
name: Sanathana Tattva
description: Calm, crafted Android storefront for traditional goods in returnable containers
colors:
  brand-green: "#1A6B2E"
  brand-green-deep: "#14532D"
  green-container: "#D9ECE0"
  on-green-container: "#092510"
  sage: "#506352"
  sage-container: "#D4E8D9"
  on-sage-container: "#0E1F13"
  ledger-gold: "#885C24"
  gold-container: "#F8EDCC"
  on-gold-container: "#4E2E0F"
  ink: "#1A1C1A"
  ink-muted: "#404943"
  mist: "#FBFDF8"
  surface-bright: "#FDFDFA"
  surface-dim: "#E0E4DC"
  outline-green: "#707973"
  outline-faint: "#C0C9C1"
  error-red: "#BA1A1A"
typography:
  headline:
    fontFamily: "Roboto"
    fontSize: "24sp"
    fontWeight: 600
    lineHeight: "32sp"
  title:
    fontFamily: "Roboto"
    fontSize: "20sp"
    fontWeight: 600
    lineHeight: "28sp"
  title-medium:
    fontFamily: "Roboto"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "24sp"
  body:
    fontFamily: "Roboto"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
  body-small:
    fontFamily: "Roboto"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
  label:
    fontFamily: "Roboto"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
    letterSpacing: "0.1sp"
rounded:
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
spacing:
  sm: "8dp"
  md: "16dp"
  lg: "32dp"
  xl: "48dp"
components:
  button-primary:
    backgroundColor: "{colors.brand-green}"
    textColor: "#FFFFFF"
    typography: "{typography.label}"
  button-primary-pressed:
    backgroundColor: "{colors.brand-green-deep}"
    textColor: "#FFFFFF"
  card:
    backgroundColor: "{colors.surface-bright}"
    textColor: "{colors.ink}"
    rounded: "{rounded.lg}"
    padding: "16dp"
  money-panel:
    backgroundColor: "{colors.gold-container}"
    textColor: "{colors.on-gold-container}"
    rounded: "{rounded.md}"
    padding: "16dp"
---

# Design System: Sanathana Tattva

## 1. Overview

**Creative North Star: "The Specialty Storefront"**

A calm, curated shop — not a marketplace. Few products per view, imagery leads, and the shelf never shouts. The system is built for households reordering staples in bright kitchens: a green-tinted world where hierarchy comes from type weight and tonal surfaces, never from noise. Every screen behaves like a well-kept storefront: the goods are the color, the chrome recedes, and money is the one thing the interface is never casual about.

The system explicitly rejects cold marketplace density (Amazon-style badge clutter), urgency mechanics of any kind, and eco-cliché decoration. The brand green carries the identity on its own; it needs no leaf icons or textures.

**Key Characteristics:**
- Light and dark themes that follow the device setting. Light rests on a green-tinted paper (`mist`); dark is true-black (OLED) with dark neutral cards lifting off it, and a fresher, more vivid grass green as the accent. Gold stays money-only in both
- Restrained color strategy: brand green ≤10% of any screen, on actions and selection only
- Gold appears exclusively at money moments — wallet, deposits, cashback, discounts
- One type family (Roboto), weight-driven hierarchy, 16sp body floor
- Depth is tonal; shadows are earned by floating surfaces only

## 2. Colors

A green-anchored restrained palette on green-tinted neutrals, with gold as a single, strictly-rationed semantic voice for money.

### Primary
- **Brand Green** (#1A6B2E): primary actions, current selection, active navigation. From the logo; the only saturated color in everyday chrome.
- **Deep Brand Green** (#14532D): pressed states and emphasis on the primary.
- **Green Container** (#D9ECE0) with **On Green Container** (#092510): tinted fills for selected states and primary-adjacent surfaces.

### Secondary
- **Sage** (#506352): the muted green-grey support role — secondary actions, quiet indicators. **Sage Container** (#D4E8D9) / **On Sage Container** (#0E1F13) for its fills.

### Tertiary
- **Ledger Gold** (#885C24): money, and only money. Wallet balances, deposits, cashback, discounts. **Gold Container** (#F8EDCC) / **On Gold Container** (#4E2E0F) for money panels.

### Neutral
- **Ink** (#1A1C1A): primary text on all light surfaces.
- **Ink Muted** (#404943): secondary text; still passes 4.5:1 on `mist` and `surface-bright`.
- **Mist** (#FBFDF8): the app background — a bright, all-but-white ground with the faintest green whisper. Cards are crisp white (`surfaceContainerLowest`) defined against it by a **hairline outline** (`outlineVariant`), not by any fill tint. The outline is centralized in `ui/components/Accents.kt` — `cardBorder()` with a single `CardOutlineWidth` knob — so every card is consistent and the treatment is tuned from one place. Works in light and dark (the colour is a theme token).
- **Surface Bright** (#FDFDFA): cards and raised content surfaces.
- **Surface Dim** (#E0E4DC) and the container ramp (#F3F5F0 → #E1E5DF): tonal layering steps.
- **Outline Green** (#707973) / **Outline Faint** (#C0C9C1): borders and dividers.
- **Error Red** (#BA1A1A): errors only, with container #FFDAD6 / on-container #410002.

### Named Rules
**The Gold-Means-Money Rule.** Ledger Gold is forbidden anywhere money is not being shown. If gold appears on screen, the user is looking at rupees — wallet, deposit, cashback, or discount. No decorative gold, ever.

**The Quiet Shelf Rule.** Brand green covers at most ~10% of any screen. It marks the action and the selection; everything else is neutral. If a screen looks "green", it's over-saturated.

## 3. Typography

**Body/UI Font:** Roboto (`FontFamily.Default`)

**Character:** One family, weight carries hierarchy. Plain-spoken and highly legible — tuned for older and second-language readers with a hard 16sp floor on body text.

### Hierarchy
- **Headline** (SemiBold 600, 24sp / 32sp): screen titles.
- **Title** (SemiBold 600, 20sp / 28sp): section heads within a screen.
- **Title Medium** (SemiBold 600, 16sp / 24sp): card and list-item titles.
- **Body** (Regular 400, 16sp / 24sp): all primary reading text. Never smaller for prose.
- **Body Small** (Regular 400, 14sp / 20sp): supporting metadata only, never full sentences of essential content.
- **Label** (Medium 500, 14sp / 20sp, +0.1sp): buttons, form labels, chips. Label Medium (500, 12sp, +0.4sp) for the smallest badges.

### Named Rules
**The 16sp Floor Rule.** Anything the user must read to complete a purchase — prices, deposits, product names, form labels' values — renders at ≥16sp. 14sp and 12sp are reserved for supporting metadata. All sizes in sp; layouts must survive 1.3× system font scale.

## 4. Elevation

Tonal-first: depth comes from the green-tinted surface container ramp (`surface-bright` down through `surfaceContainerHighest` #E1E5DF), not from shadows. Shadows are earned only by surfaces that genuinely float over scrolling content.

### Shadow Vocabulary
- **Resting card** (1dp elevation): the near-invisible lift on product cards; tonal color does the real separation work.
- **Floating bar** (8dp shadow elevation): sticky checkout/cart summary bars that overlay scrolling content — the only pronounced shadow in the app.

### Named Rules
**The Earned Shadow Rule.** A shadow above 1dp requires the surface to float over other content (sticky bars, sheets, dialogs). Static cards never get bigger shadows to "pop"; if a card needs separation, step its container tone instead.

## 5. Components

The component voice is **refined and reassuring**: generous rounding (12–16dp), calm fills, weight-driven emphasis. Nothing sharp, nothing loud. Standard Material 3 affordances throughout — the brand shows in palette and tone, never in invented controls.

### Buttons
- **Shape:** Material 3 default (full pill)
- **Primary:** Brand Green (#1A6B2E) fill, white Label text; pressed shifts to Deep Brand Green (#14532D)
- **States:** default, pressed, disabled (standard M3 38%-alpha treatment), loading where an action awaits the network
- **Secondary/Text:** Sage or brand-green text buttons for lower-stakes actions

### Cards / Containers
- **Corner Style:** gently rounded — 16dp for product and summary cards, 12dp for imagery and inner blocks, 8–10dp for thumbnails and small chips
- **Background:** `surface-bright` (#FDFDFA) or a container-ramp step; money panels use Gold Container (#F8EDCC)
- **Shadow Strategy:** 1dp resting elevation max (see Elevation)
- **Internal Padding:** 16dp standard
- **Never nested:** a card never contains another card; use tonal steps or dividers inside

### Inputs / Fields
- **Style:** Material 3 outlined/filled text fields with Outline Green (#707973) strokes
- **Focus:** brand-green focus indicator (M3 default behavior)
- **Error:** Error Red (#BA1A1A) with a message that names the fix, not just "invalid"

### Status Chips
- **Style:** small rounded (8dp) tinted badges — container-color background with matching on-container text (e.g. order status)
- **Rule:** color is never the only signal; every status chip carries text

### States (Signature Vocabulary)
- **Loading:** pulsing `SkeletonBlock` placeholders (surfaceContainerHigh, 8dp corners, 700ms alpha pulse) — never a centered spinner replacing content
- **Empty:** `EmptyState` teaches the screen (icon + title + body + optional action), never bare "nothing here"
- **Error:** `ErrorState` names the fix and always offers "Try again"

## 6. Do's and Don'ts

### Do:
- **Do** reserve Ledger Gold (#885C24 / #F8EDCC) exclusively for money: wallet, deposits, cashback, discounts.
- **Do** keep brand green to the action and the selection — ≤10% of any screen.
- **Do** label every rupee on screen: product price vs. deposit vs. wallet credit vs. cashback, in plain words.
- **Do** use skeleton blocks for loading, teaching empty states, and fix-naming error states (`ui/components/States.kt`) on every screen.
- **Do** keep all text in sp, body ≥16sp, touch targets ≥48dp, and verify 4.5:1 contrast (Ink Muted is the lightest allowed body color).
- **Do** use standard Material 3 controls; earned familiarity over novelty.

### Don't:
- **Don't** build "cold marketplace" density — Amazon-style badge clutter, cramped listings, promo noise. This is a specialty storefront, not a catalog.
- **Don't** use urgency mechanics of any kind: countdown timers, "only 2 left!", flashing promos.
- **Don't** add eco-cliché decoration: no scattered leaf icons, kraft-paper textures, or greenwashing copy. The palette carries the brand.
- **Don't** put gold on anything that isn't money, or use color as the only signal for a state.
- **Don't** give resting cards shadows above 1dp, nest cards, or invent non-standard controls for standard tasks.
- **Don't** introduce a second type family. Both light and dark themes are supported and follow the device setting; keep the green identity and the gold-means-money rule consistent across both.
