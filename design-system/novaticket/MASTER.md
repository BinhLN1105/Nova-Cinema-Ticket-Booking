# Design System Master File

> **LOGIC:** When building a specific page, first check `design-system/pages/[page-name].md`.
> If that file exists, its rules **override** this Master file.
> If not, strictly follow the rules below.

---

**Project:** NovaTicket
**Generated:** 2026-05-30 19:53:18
**Category:** Theater/Cinema

---

## Global Rules

### Color Palette

| Role | Hex | CSS Variable |
|------|-----|--------------|
| Primary | `#0D1B2A` | `--color-primary` |
| Secondary | `#1B263B` | `--color-secondary` |
| CTA/Accent | `#F5C518` | `--color-cta` |
| Background | `#0B0C10` | `--color-background` |
| Text | `#F8FAFC` | `--color-text` |

**Color Notes:** Cinema-inspired dark navy (`#0D1B2A`) + accent gold (`#F5C518`) as defined in GEMINI.md rules.

### Typography

- **Heading Font:** Inter
- **Body Font:** Inter
- **Mood:** Dramatic + Bold typography

### Spacing Variables

| Token | Value | Usage |
|-------|-------|-------|
| `--space-xs` | `4px` / `0.25rem` | Tight gaps |
| `--space-sm` | `8px` / `0.5rem` | Icon gaps, inline spacing |
| `--space-md` | `16px` / `1rem` | Standard padding |
| `--space-lg` | `24px` / `1.5rem` | Section padding |
| `--space-xl` | `32px` / `2rem` | Large gaps |
| `--space-2xl` | `48px` / `3rem` | Section margins |
| `--space-3xl` | `64px` / `4rem` | Hero padding |

### Shadow Depths

| Level | Value | Usage |
|-------|-------|-------|
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,0.05)` | Subtle lift |
| `--shadow-md` | `0 4px 6px rgba(0,0,0,0.1)` | Cards, buttons |
| `--shadow-lg` | `0 10px 15px rgba(0,0,0,0.1)` | Modals, dropdowns |
| `--shadow-xl` | `0 20px 25px rgba(0,0,0,0.15)` | Hero images, featured cards |

---

## Component Specs & Cross-Platform Implementation

### Color Tokens Mapping

#### Web Frontend (Tailwind Config)
```javascript
// tailwind.config.js
module.exports = {
  theme: {
    extend: {
      colors: {
        primary: '#0D1B2A',
        secondary: '#1B263B',
        accent: '#F5C518',
        background: '#0B0C10',
        text: '#F8FAFC',
        borderMuted: '#415A77',
      }
    }
  }
}
```

#### Android Mobile (`res/values/colors.xml`)
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="color_primary">#0D1B2A</color>
    <color name="color_secondary">#1B263B</color>
    <color name="color_accent">#F5C518</color> <!-- Gold -->
    <color name="color_background">#0B0C10</color>
    <color name="color_text">#F8FAFC</color>
    <color name="color_border_muted">#415A77</color>
</resources>
```

---

### Buttons

#### 1. Primary Button (Action CTA)
- **Web (Tailwind):** `bg-accent text-primary font-semibold py-3 px-6 rounded-lg transition duration-200 hover:bg-[#E5B508] cursor-pointer shadow-md active:scale-95`
- **Android (Material Button Style):**
  ```xml
  <com.google.android.material.button.MaterialButton
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:backgroundTint="@color/color_accent"
      android:textColor="@color/color_primary"
      android:insetTop="0dp"
      android:insetBottom="0dp"
      app:cornerRadius="8dp"
      android:textStyle="bold" />
  ```

#### 2. Secondary Button (Secondary Action)
- **Web (Tailwind):** `bg-transparent text-accent border-2 border-accent font-semibold py-3 px-6 rounded-lg transition duration-200 hover:bg-accent/10 cursor-pointer`
- **Android (Outlined Button Style):**
  ```xml
  <com.google.android.material.button.MaterialButton
      style="@style/Widget.MaterialComponents.Button.OutlinedButton"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:textColor="@color/color_accent"
      app:strokeColor="@color/color_accent"
      app:strokeWidth="2dp"
      app:cornerRadius="8dp" />
  ```

---

### Cards (Movie/Cinema Cards)

- **Web (Tailwind):** `bg-secondary text-text p-6 rounded-xl shadow-md transition duration-200 hover:shadow-lg hover:-translate-y-0.5 cursor-pointer border border-borderMuted/30`
- **Android Card (Layout Component):**
  ```xml
  <com.google.android.material.card.MaterialCardView
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      app:cardBackgroundColor="@color/color_secondary"
      app:cardCornerRadius="12dp"
      app:cardElevation="4dp"
      app:strokeWidth="1dp"
      app:strokeColor="@color/color_border_muted"
      android:clickable="true"
      android:focusable="true"
      android:foreground="?android:attr/selectableItemBackground" />
  ```

---

### Inputs (Search, OTP, Form fields)

- **Web (Tailwind):** `bg-secondary text-text border border-borderMuted rounded-lg py-3 px-4 focus:outline-none focus:border-accent focus:ring-2 focus:ring-accent/20 transition duration-200 w-full`
- **Android Input (TextInputLayout Style):**
  ```xml
  <com.google.android.material.textfield.TextInputLayout
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      app:boxStrokeColor="@color/color_accent"
      app:boxBackgroundColor="@color/color_secondary"
      app:boxCornerRadiusTopStart="8dp"
      app:boxCornerRadiusTopEnd="8dp"
      app:boxCornerRadiusBottomStart="8dp"
      app:boxCornerRadiusBottomEnd="8dp"
      android:textColorHint="@color/color_border_muted">
      
      <com.google.android.material.textfield.TextInputEditText
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:textColor="@color/color_text" />
  </com.google.android.material.textfield.TextInputLayout>
  ```

---

### Modals / Popups (Promo Detail, Booking confirmation)

- **Web (Tailwind):** Backdrop-filter `backdrop-blur-md bg-primary/80` for overlay. Modal card uses `bg-secondary text-text border border-borderMuted rounded-2xl p-8 max-w-md w-full shadow-2xl`
- **Android Dialog (Style Configuration):**
  Apply custom layout using `@color/color_secondary` as background and apply custom style to your DialogFragment:
  ```xml
  <style name="CustomDialogTheme" parent="Theme.MaterialComponents.Dialog">
      <item name="android:windowBackground">@android:color/transparent</item>
      <item name="android:windowIsFloating">true</item>
      <item name="android:backgroundDimEnabled">true</item>
      <item name="android:backgroundDimAmount">0.85</item>
  </style>
  ```

---

### Page Pattern

**Pattern Name:** Movie Ticket Booking Flow

- **Conversion Strategy:** High-impact movie trailers, intuitive showtime picker, interactive visual seat map, countdown timer (5-10 mins) to prevent ghost bookings, seamless food/drink combo upsells, and instant VNPay sandbox payment.
- **CTA Placement:** 
  - "Book Now" on movie poster hover (Home)
  - Sticky "Select Showtimes" at the bottom of Movie Details
  - Sticky "Select Seats" and "Proceed to Payment" in flow steps
  - Prominent "Pay with VNPay" on the final Checkout screen
- **Section Order & Screens:**
  1. **Home Screen:** Hero Spotlight Banner (Latest Blockbuster) -> Now Showing (Carousel) -> Coming Soon (Grid) -> Quick Booking Bar (Select Movie > Cinema > Date > Show).
  2. **Movie Detail Screen:** Dramatic Backdrop Image -> Movie Info (Director, Cast, Duration, Genre) -> Synopsis & Trailer -> Audience Reviews -> Showtime & Hall Selector.
  3. **Seat Picker Screen:** Hall Screen Indicator -> Interactive Grid (Legend: Selected, Regular, VIP, Sweetbox, Reserved) -> Bottom Sticky Summary (Seats Chosen, Total Price) -> "Proceed to Food/Combo" CTA.
  4. **Checkout Screen:** Seat Summary & Show Info -> Food & Beverage Combo Selector -> Ticket Pricing Details -> Countdown Timer -> VNPay Sandbox Payment.

---

## Anti-Patterns (Do NOT Use)

- ❌ Poor booking UX
- ❌ No trailers

### Additional Forbidden Patterns

- ❌ **Emojis as icons** — Use SVG icons (Heroicons, Lucide, Simple Icons)
- ❌ **Missing cursor:pointer** — All clickable elements must have cursor:pointer
- ❌ **Layout-shifting hovers** — Avoid scale transforms that shift layout
- ❌ **Low contrast text** — Maintain 4.5:1 minimum contrast ratio
- ❌ **Instant state changes** — Always use transitions (150-300ms)
- ❌ **Invisible focus states** — Focus states must be visible for a11y

---

## Pre-Delivery Checklist

Before delivering any UI code, verify:

- [ ] No emojis used as icons (use SVG instead)
- [ ] All icons from consistent icon set (Heroicons/Lucide)
- [ ] `cursor-pointer` on all clickable elements
- [ ] Hover states with smooth transitions (150-300ms)
- [ ] Light mode: text contrast 4.5:1 minimum
- [ ] Focus states visible for keyboard navigation
- [ ] `prefers-reduced-motion` respected
- [ ] Responsive: 375px, 768px, 1024px, 1440px
- [ ] No content hidden behind fixed navbars
- [ ] No horizontal scroll on mobile
