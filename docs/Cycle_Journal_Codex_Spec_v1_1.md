# Offline Period Tracker for Android — Codex Implementation Specification

**Specification version:** 1.1  
**Status:** Implementation-ready functional specification; final visual design is **not approved**.  
**Product name:** Working title **Cycle Journal** (replaceable; not an approved brand).  
**Audience:** Codex or another engineering agent building a complete, production-quality native Android application.

## 0. Instructions to the implementing agent

Build a **fully working native Android application**, not a demo, prototype, web view, or set of mock screens. Follow this specification as the functional source of truth. Use good Android engineering judgment for minor unspecified implementation choices, and record these choices in `DECISIONS.md`. Do not add unrelated features. There is **no backend and no account**. All normal functionality, predictions, reminders, and storage must work without internet access. The repository must also provide a Docker-based Android build and a one-command developer launch workflow as specified in Section 21.

The user has **approved** the product scope, three-tab navigation, unified monthly Calendar, period countdown, contextual Start Period / End Period action, date-based daily symptom entry, insights, settings, opt-in notifications, encrypted backup export/import, and a *direction* of soft/elegant violet (not pink). The user has **not approved** final layouts, palette values, fonts, component styling, or brand. Build a consistent, attractive **working visual implementation** using centralized replaceable design tokens. Do not represent that implementation as an approved final visual identity.

Do not interrupt implementation for trivial decisions. Do not silently add fertility, pregnancy, ovulation, sexual activity, medical diagnosis, cloud synchronization, analytics, advertisements, subscriptions, onboarding accounts, or AI. Use real persistence and real interactions. No hardcoded sample health data in release builds. If some external build tooling is unavailable, complete what is possible and describe exactly what could not be verified; never report unrun tests as passing.

## 1. Product overview

A privacy-first menstrual period tracker for Android. Users can record when periods start and end, mark symptoms and mood on individual dates, see a marked monthly calendar and an estimate of the next period, examine simple cycle statistics, and receive optional local reminders. No registration, internet, server, or sharing of health records is required. Users may manually create an encrypted backup file and restore it later.

**Audience:** Adults who want straightforward personal period tracking. Use inclusive, neutral copy such as “your period” rather than assumptions about gender. This is a personal recordkeeping and estimate app, **not contraception, fertility guidance, pregnancy prediction, or a medical diagnostic tool**.

**Core principles:** (1) privacy by default, (2) honest estimates, (3) very fast logging, (4) clear distinction between recorded versus predicted dates, (5) uncluttered and consistent UI, (6) dependable offline behavior, (7) no data loss when editing.

## 2. Scope and exclusions

### In scope for the first complete release

- One-time offline onboarding and initial cycle preferences.
- Exactly three bottom-level tabs: **Calendar**, **Insights**, and **Settings**.
- A merged main Calendar with estimated-period countdown/status above a full **one-month-at-a-time** calendar.
- Calendar indicators for recorded periods, predicted period dates, today, and dates with daily logs.
- Contextual **Start Period** or **End Period** main action according to ongoing-period state.
- Tap a calendar day to open a Day Details bottom sheet with date-specific entry, editing, and appropriate period actions.
- Record, modify, finalize, and delete historical period records.
- Daily log fields: flow, symptoms, mood, pain, and optional notes.
- Cycle estimates based initially on the user-entered typical length, then on observed history.
- Insights: cycle length, period length, history, simple charts and explanatory empty states.
- Two independently optional local notification types: upcoming-period reminder and daily logging reminder.
- Settings for cycle preferences, appearance, reminders, data/privacy, and app information.
- Light and dark themes with consistent polished visual system.
- Password-encrypted export/import of the entire user-owned dataset via Android's document picker.
- Local data deletion, validation, accessibility, testing, and release-friendly project setup.

### Explicitly out of scope

Backend, internet APIs, sync, sign-in, tracking other users, sharing, fertility windows, ovulation, intercourse records, pregnancy mode, medical advice, AI, camera, Bluetooth, wearables, ads, purchases, gamification, third-party tracking or crash-reporting SDKs, home-screen widgets, biometric app lock, and automatic cloud backups. Do not add these without a later change request.

## 3. Technology and project structure

Use:

- Kotlin, native Android, Gradle Kotlin DSL, single-activity architecture.
- Jetpack Compose + Material 3 foundations, custom centralized design tokens.
- `ViewModel`, `StateFlow`, unidirectional UI data flow, coroutines.
- Room for period and daily log records. DataStore for small user preferences.
- Repository layer separating UI from persistence; dedicated pure-Kotlin prediction and statistics services.
- Lifecycle-aware state collection; navigation using a current stable AndroidX solution.
- WorkManager or appropriate current Android local scheduling for **best-effort** reminders; do not use exact-alarm permissions without a justified requirement.
- Android Storage Access Framework (`ACTION_CREATE_DOCUMENT` and `ACTION_OPEN_DOCUMENT`) for encrypted export/import.
- Android standard cryptography for authenticated backup encryption (details below).
- Dependency injection with Hilt or an equivalently clear, tested solution.
- Minimum Android API 26 unless a specific library forces a justified change; compile/target current stable SDK and use mutually compatible current stable libraries verified at implementation time. Compose dependencies should use the official Compose BOM; avoid arbitrary versions or experimental APIs without need.

Recommended packages (may reorganize sensibly):

```text
app/
  MainActivity.kt
  navigation/
  ui/theme/                 # design tokens, typography, dark/light, dimensions
  ui/components/            # shared buttons, day cells, period cards, chips, sheets
  ui/onboarding/
  ui/calendar/
  ui/daydetails/
  ui/period/
  ui/insights/
  ui/settings/
  ui/notifications/
  ui/privacy/
  domain/model/
  domain/prediction/
  domain/statistics/
  domain/usecase/
  data/room/
  data/datastore/
  data/repository/
  data/backup/
  reminders/
  util/date/
  test/ and androidTest/
```

Keep date arithmetic and core business logic independent of Android UI so it can be unit-tested quickly.

## 4. Navigation and screen map

There are **exactly three top-level destinations**, always available through bottom navigation after onboarding:

1. **Calendar** — default launch tab, contains status/countdown, monthly calendar, primary period action.
2. **Insights** — summary metrics, chart, cycle history.
3. **Settings** — cycle preferences, reminder settings, appearance, privacy/data, About.

There is **no separate Home screen** and no fourth main tab. The Calendar is the home experience. Use back navigation for detail screens and sheets; do not create duplicate primary destinations.

Secondary destinations/overlays:

- Onboarding: Welcome -> Initial cycle details -> Reminder/theme choices -> Calendar.
- Day Details: bottom sheet entered by tapping a date.
- Period Action: compact confirmation/date editor sheet, opened via Start/End or Day Details.
- Edit Period: sheet or detail page for changing an existing period's start, end, and status or deleting it.
- Cycle History: accessible from Insights.
- Cycle Detail: accessible from Cycle History.
- Reminder Settings: accessible from Settings.
- Privacy & Data: accessible from Settings.
- Appearance/Cycle Preferences: settings detail pages or well-structured subsections.

## 5. First launch / onboarding

**Goal:** start tracking in under a few minutes, without an account.

### Screen O1: Welcome

- Short product title and explanation: offline period tracking with privacy-first storage.
- State that estimates are approximate and data stays on-device unless exported by the user.
- Primary action `Get started`. No login or personal identifiers.

### Screen O2: Cycle setup

- `When did your last period start?` — optional date picker, cannot be future.
- `Usual cycle length` — numeric control, initial value 28 days, user-editable. Treat this as an estimate rather than a medical norm; allow broad reasonable values (15–90 days) with clear validation.
- `Usual period duration` — numeric control, initial value 5 days, 1–15 days for the initial estimate. This is not an automatic actual end date.
- Can skip the last-period date. If skipped, the Calendar works but no date-specific prediction is shown until a start date is recorded.
- If a historical last-period start is entered, ask optionally for the *actual* end date. If omitted, create an `END_UNKNOWN` record, **not** an ongoing period extending automatically to today.

### Screen O3: Preferences

- Theme preference: System default, Light, Dark.
- Optional upcoming-period reminder; optional daily-log reminder. Both **off by default**. Ask OS notification permission only if a reminder is enabled, not automatically on app launch.
- Brief mention of manual encrypted backup availability in Settings.
- Finish onboarding and open Calendar.

Onboarding state persists across process death. Back navigation retains entered information. User can later edit usual lengths in Settings. Never force an exact period date or any health data to use the app.

## 6. Calendar: main screen detailed specification

### 6.1 Vertical content order

Top-to-bottom default composition:

1. Lightweight header: `Calendar` and optional `Today` control when browsing another month. Avoid a duplicate Settings shortcut because Settings already has its own tab.
2. **Prediction/status card:** prominent countdown or current-period status, with supporting date/range and an estimate disclaimer.
3. **Month header:** current month/year and previous/next month arrows. Tapping month/year may open a month/year picker if simple to implement; arrows are mandatory.
4. **Seven-column calendar grid:** weekday labels, four to six rows as required, appropriate leading/trailing dates or blanks. One month at a time, not an endless list and not a collapsed week view.
5. **Compact legend:** recorded period, predicted period, logged-day indicator. Today and selected-date semantics must be visually distinguishable as well.
6. **One contextual period button:** `Start Period` when no period is ongoing, or `End Period` when a period is ongoing. Button applies to **today**, regardless of displayed month or which calendar date was previously selected; its confirmation sheet makes the date editable.
7. Persistent bottom navigation with Calendar, Insights, Settings.

For small screens, use a scrollable main content container that does not let the bottom navigation cover content. Support system insets and keyboard appearance.

### 6.2 Countdown/status card states

- **One or more recorded start dates and forecast available, no ongoing period:** `Next period estimated in ~11 days`, with `Around 27 Nov` or a wider predicted range if variable. Use approximate language; never imply certainty.
- **Today is the central estimated date:** `Period estimated around today`.
- **Estimate is in the past with no newer recorded start:** `Your previous estimate was Nov 27` and `Record a period when it starts`. Do not show a negative countdown, automatically move the date into the future, diagnose lateness, or imply pregnancy.
- **There is an explicit ongoing period:** `Period day N`, beginning from the recorded start date (day 1 = start date). Offer the contextual End Period action. Never infer symptom/flow values for intervening days.
- **No start date / insufficient information:** `Add your first period to see an estimate`; do not fabricate predictions.
- **Highly variable history:** show a broad estimated date range and a simple `Timing varies` note. The primary figure may be approximate, not a falsely precise promise.

The countdown always concerns the latest/current actual cycle, **not** the historical month being browsed. It updates on local midnight, app resume, time-zone change, and edits to period records.

### 6.3 Calendar date cells

Mark **actual recorded period dates** with one solid style, and **predicted dates** with a visibly different lighter/dashed/patterned style. They must also differ in shape/legend/semantics, not just shades of violet. An ongoing period may be visually highlighted from its confirmed start through today as an ongoing status **without** creating inferred daily flow entries. `END_UNKNOWN` marks only the known start date or an explicitly known range, never invents an end day. Actual recorded information must take precedence over predictions for overlapping dates.

Other states: today indicator, selected-date indicator while the sheet is open, and a small dot/marker for days containing a saved daily log. Multiple statuses can coexist and remain legible. Show dates belonging to periods that cross month boundaries. Use localized weekday formatting and first day of week derived from device locale unless changed in a future release. Date calculations must not shift when the user travels between time zones.

Tapping **any day** selects that date and opens Day Details. Future dates may be viewed, but **future events/daily observations cannot be saved as actual historical facts**. Keep the month visible behind the sheet. Return from a sheet to the previously displayed month. Provide accessible names such as `16 November, recorded period, daily log available` rather than only a color state.

### 6.4 Calendar period action

Use **only one prominent button**, which depends on the latest explicit period status:

- If no `ONGOING` period exists: `Start Period`.
- If an `ONGOING` period exists: `End Period`.

A tap opens a **small date-confirmation sheet** (implementation default, not a separate user-approved interaction decision): Start uses today as default start; End uses today as default last day. Both permit choosing a valid date before saving. Date confirmation prevents accidental incorrect period dates. For advanced or historical edits, use Edit Period from a selected day.

`Start Period` may not create an overlapping period. When starting on an earlier date, explicitly offer `Completed (with end date)`, `Still ongoing`, or `End date unknown` rather than assuming it lasted through today. Starting another period cannot leave multiple active ongoing periods. `End Period` only acts on an active ongoing period, requires end >= start, uses an **inclusive** last bleeding date, and cannot run into another existing period. Period deletion requires a confirm dialog and leaves all independently stored daily logs intact.

## 7. Day Details sheet

Opened by tapping a date. This is the principal data-entry surface. Show:

- Full selected date and, if known, cycle day. Close/back affordance.
- Existing day record, prefilled for editing, or an empty state with fields ready to log.
- **Flow:** unspecified (default), None, Spotting, Light, Medium, Heavy. A user explicitly choosing `None` is different from never logging flow.
- **Symptoms:** multi-select chips for Cramps, Bloating, Headache, Fatigue, Back pain, Breast tenderness, Acne, Nausea, and Other (Other is a simple predefined category, not a custom symptom builder). Optional explicit `No symptoms` state; leave unselected when unknown. Symptom names and underlying codes must be stable and localized separately.
- **Mood:** unspecified, Very good, Good, Neutral, Low, Very low; single select.
- **Pain:** unspecified, None, Mild, Moderate, Severe; single select.
- **Notes:** optional multiline, max 2,000 characters, editable.
- Contextual period controls: `Start period on [date]` if valid and not in an existing period, `End period on [date]` if an ongoing period can validly end that day, or `View/Edit period` for dates within an existing period. Keep this action visually separate from saving the daily log.
- Sticky or easily reachable `Save daily log` action, and `Delete daily log` only if one exists. Allow partial entries: nothing except selected date is required. Avoid creating empty meaningless records when no fields were entered.

If only a particular field is changed, do not clear the other fields. When existing data exists, show its actual values. Leaving a field unset is distinct from an explicit `None`. Saved daily entries create a calendar indicator. Opening/closing with no changes does not write data. Confirm before losing unsaved changes, including when navigating to a period editor. Close the sheet after successful save, retain the calendar month, and show discreet confirmation.

Daily logs and period boundaries are **independent**: flow does **not** automatically start/end a period. Deleting/editing a period must never delete/edit independent daily logs unless the user separately chooses to delete those logs. Past and today can be logged; no future actual logs.

## 8. Period model, history editing, and validation

Each period has a start date and one of three statuses:

- `ONGOING`: user explicitly reports that the period has started and not yet been ended. End date is null. **At most one** ongoing period; it must be the latest period by start date.
- `COMPLETED`: has a valid inclusive end date `>= startDate`.
- `END_UNKNOWN`: start date known, ending date not recorded. Not considered ongoing; does not imply bleeding through today.

Start date cannot be in the future. Completed end date cannot be in the future or before start. No period start may be duplicated; completed ranges may not overlap. An unknown-ended record has no claimed full duration; avoid treating it as a range. Prevent starting a new period inside a known completed span. Permit consecutive starts with unusually short/long intervals after an explanatory warning if they do not overlap; do not enforce a simplistic diagnosis or reject legitimate irregular cycles. Edits affecting multiple adjacent periods must be validated transactionally.

**Edit Period screen/sheet:** display start, end when completed, and status; allow corrections, save, and delete with confirmation. For completed records, date range is editable. For unknown-ended records, allow entering an end date. For ongoing records, support finishing or changing the start date. History backfill must support periods in either order. Prevent contradictory states and show clear validation errors instead of silent corrections.

**Terminology:** A *period* is one bleeding episode. A *cycle* is measured from one period's start date to the next period's start date. There is **no** separate user action called `Create Cycle`; cycles are derived from period records.

## 9. Predictions — deterministic, nonmedical v1 algorithm

Implement a pure, versioned `CyclePredictionEngine` with an injectable `Clock` and unit tests. Recalculate after every period change, cycle-preference change, and day transition. Do not persist predictions as independent facts; compute them from saved records so they cannot become stale.

### Inputs

Sorted actual period start dates; completed period lengths; onboarding `usualCycleLength` (default 28 days); onboarding `usualPeriodDuration` (default 5 days); current local date.

### Cycle length

A cycle interval is the number of `LocalDate` days between **consecutive recorded period start dates**. A period's end date does not determine cycle length. Take at most the **six most recent** observed intervals. No arbitrary filtering of irregular values; flag broad variation rather than hiding it.

- No recorded start date: no date-specific prediction.
- One recorded start, zero intervals: use the user-entered usual cycle length; label `Based on your usual cycle length`.
- Two recorded starts, one interval: use the rounded midpoint of usual cycle length and observed interval as a **preliminary** estimate.
- Three or more recorded starts: use the rounded **median** of up to six latest observed intervals as the estimate. For an even number of intervals, median is the midpoint of the two middle values, rounded consistently.
- Central next estimated start date = most recent actual start date + estimated cycle length (calendar days).
- If the central estimate has passed and the user has not recorded another start, retain/display it as a **past estimate** rather than rolling it forward artificially.

### Uncertainty and predicted period span

- Zero intervals: provisional range around central date of +/-3 days.
- One interval: preliminary range around central date of +/- max(3, half the absolute difference between observed and usual lengths, rounded up).
- Two or more intervals: projected earliest/latest start dates from recent **minimum and maximum** cycle intervals; ensure at least +/-2 days around the central estimate. For highly variable intervals use the wider observed range and label as uncertain.
- Estimated bleeding duration: rounded median of up to six most recent **completed** period durations; if none, use onboarding usual period duration. Show predicted period days using the central expected start and estimated duration, but retain a separate uncertainty window for the start date. Use a visually different style for predicted versus recorded.
- Never show a fertile window, probability of pregnancy, ovulation date, or contraceptive assurance. Include a subtle explanatory line `Estimates are based on recorded dates and may vary.`

Choose a single documented rounding convention for all calculations (for example half-up), and test it. All predictions use date arithmetic, not 24-hour timestamp increments.

### Examples that must be tested

- Typical 28-day cycle, last start Nov 1 -> estimated central start Nov 29.
- Starts Jan 1, Jan 29, Feb 28, Mar 27 -> lengths 28, 30, 27; median 28; next start Apr 24 (in a non-leap year).
- Last recorded start exists but predicted date is now past -> past-estimate state, never negative countdown.
- Explicit ongoing period -> current `Period day N` overrides countdown.
- Travel between time zones -> recorded calendar dates remain unchanged.

## 10. Insights tab and history

Show explanatory empty states until sufficient history exists. **Never fabricate statistics**.

### Insights content hierarchy

1. Title `Insights`.
2. Statistic cards: `Typical cycle length` (median observed when available, otherwise show `Not enough data` or mark user-estimated), `Typical period duration` (median of completed durations), `Shortest cycle`, `Longest cycle` (only when enough intervals exist).
3. A simple, readable bar or line chart for the **last up to 6 observed cycle lengths**; date labels and a numerical y-axis or value labels, plus an accessibility text equivalent.
4. Optional compact row `Recent symptoms` listing the most commonly logged selected symptoms from recent records with clearly stated sample size; no causal health claims.
5. `Cycle history` entry point, with a chronological list of cycles and period records.
6. A short note: observations are informational, not medical assessments.

### History

- Newest first, each record shows start date, end date or `Ongoing` / `End unknown`, known duration, and observed cycle length to the next recorded start when available.
- Tapping one opens Cycle Detail with period metadata and daily logs within the known cycle boundaries.
- Edit and delete the corresponding period from detail. Do not delete daily logs implicitly.
- Completed period duration = number of calendar days from start through end **inclusive**. Unknown/ongoing entries do not contribute to completed-duration averages.
- A one-off observed interval may be shown in history but summary trend claims should indicate limited data.

No web calls, external prediction SDKs, fertility scores, or medical alerts.

## 11. Settings tab

Use clear grouped sections and a consistent row pattern.

**Cycle preferences**
- Usual cycle length (editable; used as fallback and with very limited history).
- Usual period duration (editable fallback for predicted duration).
- Explanation that recorded history takes precedence when sufficient data exists.

**Reminders**
- `Upcoming period reminder` toggle (default off).
- Advance notice: default 2 days before central estimate; choose 1, 2, 3, or 5 days.
- `Daily logging reminder` toggle (default off).
- Preferred reminder time, default 20:00 local time, editable.
- `Discreet notification text` on by default, with generic text such as `Your reminder is ready`; optional more specific wording only if explicitly enabled.
- Permission state and explanatory prompt if Android notification permission is denied.

**Appearance**
- System / Light / Dark theme.

**Privacy & Data**
- Explanation: no account, no automatic upload, local data, user-directed encrypted export.
- `Export encrypted backup`.
- `Import encrypted backup`.
- `Delete all data` with strong confirmation and cancellation of reminders, returning to clean onboarding or clean Calendar as appropriate.
- A visible notice that uninstalling/clearing app data without exporting may destroy records.
- Link/text for a privacy policy, release-ready and accurate.

**About**
- Working app name, version, explanation of prediction limitations, open-source licenses if required, privacy policy, and nonmedical disclaimer.

Do not include settings for features not implemented.

## 12. Local notifications

All notifications are **opt-in**. Upcoming-period reminder: notify at the user's preferred local time on the configured lead date relative to the **central estimated start**, only when a prediction exists and the upcoming reminder is applicable. Do not repeatedly notify for the same estimate or on multiple days unless explicitly selected. Maintain a deduplication key derived from prediction start date and reminder type. Daily reminder: at/near preferred local time once per local date, only when opted in; avoid another reminder if that day's log is already saved if practical.

Use WorkManager or the appropriate platform mechanism as **best effort**, not guaranteed to fire at the exact minute; Android background execution limits apply. Reschedule on changes to date/prediction/settings, time zone, relevant app lifecycle events, and device reboot where applicable. Ask `POST_NOTIFICATIONS` permission on Android 13+ only when user enables a reminder. If denied, show a clear settings state and do not falsely claim reminders are active.

Notifications should have a neutral default title/body and must not display cycle dates or health details on the lock screen by default. Tapping the notification opens Calendar (or today's log for the logging reminder). Do not include sensitive notification payloads in third-party services.

## 13. Data models and persistence

Use a stable Room schema with explicit migration strategy; names may differ but semantics must not.

```text
PeriodEntity
  id: UUID/String primary key
  startDate: LocalDate (stored as epoch-day INTEGER or strict ISO date)
  endDate: LocalDate?               # only for COMPLETED
  status: ONGOING | COMPLETED | END_UNKNOWN
  createdAt: Instant
  updatedAt: Instant

DailyLogEntity
  date: LocalDate primary key
  flow: null | NONE | SPOTTING | LIGHT | MEDIUM | HEAVY
  mood: null | VERY_GOOD | GOOD | NEUTRAL | LOW | VERY_LOW
  pain: null | NONE | MILD | MODERATE | SEVERE
  notes: String? (max 2000 characters)
  noSymptomsExplicit: Boolean?      # null=unknown, true=no symptoms, false=some selected
  createdAt: Instant
  updatedAt: Instant

DailyLogSymptomEntity
  date: LocalDate foreign key to DailyLog
  symptomCode: stable enum/string
  composite primary key(date, symptomCode)

DataStore UserPreferences
  onboardingComplete: Boolean
  usualCycleLengthDays: Int
  usualPeriodDurationDays: Int
  theme: SYSTEM | LIGHT | DARK
  upcomingReminderEnabled: Boolean
  upcomingReminderDaysBefore: Int
  dailyReminderEnabled: Boolean
  reminderLocalTime: LocalTime
  discreetNotificationText: Boolean
  reminderDeduplicationState: appropriate keys
```

Enforce no duplicate daily log dates, no duplicate symptom selections per day, valid period lifecycle, and exclusive overlapping completed ranges. Perform multi-record updates in transactions. Repositories expose `Flow`s of periods, daily logs, and settings. Room is the source of truth for health records. Use type converters for dates and enums; documented versioned migrations. No data should be written into application logs, analytics, crash reports, or debug messages. Redact data from errors.

Use Android app-private storage. Do **not** request `INTERNET`, location, camera, contacts, storage-wide permissions, or any health-connect permission. Minimal OS permissions only, including notifications if enabled. Treat Android's own file-based encryption as the baseline for app-private storage; the separate password-protected **export** is a firm requirement. A separate SQLCipher-style app-database encryption layer is not in v1 scope unless implemented reliably without breaking recovery or performance.

## 14. Privacy, encrypted backup, import, and deletion

**No remote services.** Turn off Android automatic/cloud backup of sensitive app data with explicit manifest and relevant data-extraction/backup exclusion rules, verifying behavior including Android 12+ device-to-device transfer variations. Do not claim perfect prevention on all OEM systems without testing. No Firebase, analytics SDK, ad SDK, or network-backed font fetching. Never embed user-entered health data in crash logs or notifications by default.

### Encrypted export

- User opens Settings -> Privacy & Data -> Export.
- Explain that the exported file contains sensitive data and requires a user-supplied passphrase to recover.
- Require entering passphrase twice, use an appropriate length check (e.g. at least 10 characters) and explain that it cannot be recovered if forgotten.
- Serialize all period records, daily records, symptom selections, and relevant app preferences to a versioned backup schema. No device-only notification permission state or reminder job IDs.
- Encrypt with authenticated encryption **AES-256-GCM**, random unique 96-bit nonce and salt per backup, and a derived key using a well-reviewed, available on-device KDF such as PBKDF2-HMAC-SHA256 with calibrated high iteration count. Include nonsecret version/KDF parameters in an authenticated file header. Use `SecureRandom`, avoid hardcoded keys, and authenticate integrity before restoring.
- Use system document creation picker; export a single encrypted file with a clear extension such as `.cyclebackup`. Never write or leave an unencrypted intermediate file on external/public storage. If an internal temporary is unavoidable, secure/clean it; prefer streaming/in-memory for small backups.
- Report completion only after the write successfully finishes. Handle canceled document selection, no space, wrong permissions, and unexpected I/O failures.

### Encrypted import

- Use system document picker, request passphrase, decrypt and authenticate, then validate schema version, dates, enum values, duplicates, and non-overlapping periods before **any** database mutation.
- Show preview totals (e.g. 12 periods, 57 daily logs) and explicitly ask whether to **replace all current local data**. For v1, import **replaces**, not merges. Clearly warn that local data will be replaced.
- Restore transactionally. A failed/corrupted/wrong-passphrase import must leave all existing data unchanged. Restore settings sensibly but do not silently activate notification permissions; re-register notification work only if enabled and allowed.
- Handle unsupported future backup versions without destructive fallback.

### Delete all data

- Dedicated destructive confirmation, preferably an extra explicit acknowledgment. Clear Room, DataStore health preferences/deduplication, pending reminders, and transient caches. Never delete user-owned exported backup files outside the app's control.

**Not required in v1:** biometric lock, user accounts, automatic backups, cloud encryption, account-based recovery.

## 15. Visual and interaction design: provisional working system

**The user selected the direction** “soft and elegant, premium, no pink, violet/lavender instead.” Exact styling remains **unapproved**. Codex should implement high-quality coherent defaults centralized in theme tokens so changing them later is easy, without changing business logic.

**Working light tokens (provisional):** background warm ivory `#FAF9F7`, surface `#FFFFFF`, primary violet `#7155A5`, soft lavender `#E9E2F4`, text ink `#292533`, secondary text muted gray-violet. **Working dark tokens (provisional):** background `#19171E`, surface `#27232E`, accent `#B9A5DF`, primary text `#F7F4FA`, secondary muted light gray-violet. Verify contrast; adjust as needed. Do **not** use pink as the brand accent.

- Use one clean premium sans-serif family (system font is acceptable until typography approved). Express hierarchy via consistent typographic scale rather than varied fonts.
- 4/8dp base spacing grid, standard screen gutters around 20dp, consistent card radii (e.g. 16–24dp), restrained borders and shadows, one consistent icon style.
- Strongest visual emphasis goes to the countdown/current-period state, then the monthly grid, then the contextual action. Avoid a crowded dashboard and decorative medical clichés.
- Recorded periods: solid violet/lavender treatment; predicted dates: distinct patterned/dashed/outlined treatment; daily logs: small indicator; selected day and today: separate indicators. Meaning must remain understandable in grayscale and with screen readers.
- Polished, short motion for sheets and month changes; respect system animation and reduced-motion preferences. No flashy gradients or confetti.
- Handle typography scaling and touch targets >=48dp, contrast in both themes, TalkBack semantics, keyboard/inset behavior, and small screens. Text must not overflow buttons/chips or render off-screen.
- Reusable components: `AppScaffold`, `CycleStatusCard`, `MonthHeader`, `CalendarDayCell`, `CalendarLegend`, `ContextualPeriodButton`, `DateDetailsSheet`, `PeriodDateSheet`, `FlowSelector`, `SymptomChip`, `MoodSelector`, `PainSelector`, `MetricCard`, `CycleChart`, `SettingsRow`, `ConfirmDialog`, `EmptyState`.
- Centralize strings and accessibility labels in resources. English UI required for v1; architecture should permit later localization. Format dates according to locale and device preference.
- No external runtime image/font downloads. Use tasteful vector icons and Compose drawing where possible.

## 16. Important states, error handling, and accessibility

Must support:

- Fresh install with zero period records; user skipped onboarding date.
- First recorded period but no observed cycle interval.
- One observed interval, then sufficient multi-cycle history.
- Currently ongoing period; unexpectedly long ongoing record (prompt to review, no diagnosis).
- Historical record with unknown end date.
- Previous predicted date passed without new records.
- Period crosses month/year boundary.
- Leap day/year, 28/29/30/31-day months, varying start weekdays, months requiring 6 calendar rows.
- Time zone/DST changes without changing historical calendar dates.
- Backfilling old periods in nonchronological order.
- Invalid or overlapping edits with human-readable errors and no partial writes.
- Save/edit/delete daily log without changing period dates.
- Unsaved changes and back navigation.
- Device rotation, activity recreation, process death, resume after midnight.
- Notifications disabled, blocked by OS, or delayed by the OS.
- Empty/large datasets, encrypted backup passphrase failure, corrupted/unsupported backups, interrupted import/export.
- Screen reader, large font scaling, dynamic contrast, and small device screens.

Error UX: specific and nontechnical messages, no crash, never discard old data following a failure. Destructive operations always require confirmation. Show loading/progress on import/export and prevent duplicate taps.

## 17. Test plan and definition of done

### Unit tests (minimum)

- Calendar `LocalDate` handling, inclusive period duration, cycle interval definition, cycle-day counting.
- Prediction modes: no data, onboarding-only fallback, one interval, 2+ intervals, medians, even median rounding, variable intervals, past estimates.
- Start/end transitions, only one ongoing period, invalid periods, cross-month periods, unknown end dates, nonoverlap.
- Saving/deleting daily logs does not modify period records; editing/deleting periods does not delete unrelated logs.
- Symptom multi-selection, explicit none vs unset, note validation.
- Insights statistics correct and missing-state rendering conditions.
- Backup round-trip produces equivalent records, wrong passphrase fails, tampering fails, unsupported schema fails, failed import is atomic.
- Notification deduplication and scheduling when toggles or predictions change.

### UI / integration tests (minimum)

1. First launch -> onboarding -> Calendar with no fabricated data.
2. Onboarding with last-period start -> estimated period is shown as approximate.
3. Calendar month navigation; return to current month; month retained when closing a sheet.
4. Tap a day -> Day Details -> select flow, symptoms, mood, pain, note -> save -> marker appears -> reopen fields preserved.
5. Contextual Start Period for today -> active state, `Period day 1`, primary button changes to End Period.
6. End Period on a selected end date -> completed record; period duration and Insights update.
7. Backfill historical completed/unknown-ended periods; edit/delete them without losing daily logs.
8. Predicted period visible with distinct style from actual record.
9. Insights empty state, chart and history after enough entries.
10. Settings theme changes apply throughout navigation and sheets.
11. Reminders can be enabled, disabled, and OS permission-denied case handled correctly.
12. Export to encrypted file; import after clearing data; old data restored; wrong password leaves current data unchanged.
13. Delete all data clears tracking and reminders.
14. Offline behavior: app runs in airplane mode; no requests to internet.
15. Accessibility smoke tests for controls, calendars, and 200% font scale.

### Required engineering outputs

- Compilable Android Studio / Gradle project and Gradle wrapper.
- Reproducible Docker-based build environment, Docker Compose configuration, and a one-command local launch script (`./run.sh`) that builds, installs, and starts the Android app on a connected device or running emulator.
- All listed screens and real end-to-end flows, no placeholder TODO implementations for required features.
- Real Room persistence with migration strategy, reliable backups, robust exception handling.
- Pure domain prediction/statistics engine and test coverage.
- `README.md`: setup, build, install, architecture, limitations, app use, and offline/privacy explanation.
- `DECISIONS.md`: any minor defaults and implementation tradeoffs, emphasizing unapproved visual styling.
- `PRIVACY.md`: accurate privacy behavior and instructions to publish an appropriate privacy policy.
- Evidence of building/tests actually executed, or exact blockers if not executable in the environment.

**Definition of done:** A user can install the APK on a supported Android device, finish onboarding, start/end periods, view the monthly calendar/countdown, enter and edit daily symptoms, view meaningful insights, configure optional reminders, export and restore an encrypted backup, and delete their data; everything works without network connectivity and without silently losing records. Light and dark screens are coherent and accessible.

## 18. Release considerations

- Set a clear app label/icon; working name is not brand approval.
- Target current Play requirements and check current Android SDK/Play policies before publishing.
- Google Play's **Health apps declaration** includes **Period Tracking**. Declare this category correctly.
- Provide an accurate privacy policy in app and via a public, non-geofenced, editable-only-by-publisher URL if published to Google Play. It should explicitly explain the device-local storage and manual user-initiated exports.
- Present the app as informational tracking only, not a medical device or reliable fertility/contraceptive instrument.
- Before release verify Android backup and device-to-device-transfer behavior, relevant OS permissions, and current Play Data safety/health-policy disclosures.

## 19. Proposed implementation sequence for Codex

1. Generate project scaffolding, Gradle configuration, navigation, theme tokens, reusable foundations.
2. Implement Room entities, repositories, DataStore, and pure domain date/prediction/statistics logic with unit tests.
3. Implement onboarding and Calendar with real persistence, date selection, contextual Start/End actions, Day Details and period editor.
4. Implement Insights and history/detail views.
5. Implement Settings, appearance, and local notifications.
6. Implement encrypted import/export, privacy controls, and full data deletion.
7. Add accessibility, screen sizes, state restoration, automated tests, documentation, and integration checks.
8. Add Dockerfile, Compose builder service, launch script, cache configuration, and clear prerequisites; verify one-command build/install/start on an available emulator or device.
9. Build a release-compatible APK; run all available tests; document remaining blockers. No backend, no network permission, no mocked health data in production.

## 20. Official reference links for developers (verify updates at implementation time)

- Android app architecture: https://developer.android.com/topic/architecture/recommendations
- Jetpack Compose BOM: https://developer.android.com/develop/ui/compose/bom
- DataStore: https://developer.android.com/topic/libraries/architecture/datastore
- Android backup rules: https://developer.android.com/identity/data/autobackup
- Android notification permission: https://developer.android.com/develop/ui/compose/notifications/notification-permission
- Google Play period-tracking health declaration: https://support.google.com/googleplay/android-developer/answer/14738291
- Google Play health privacy policy: https://support.google.com/googleplay/android-developer/answer/16679511

## 21. Dockerized builds and one-command launch (mandatory)

**User requirement:** Use Docker to make development setup reproducible and make launching the completed Android app as close to **one command** as technically possible. The standard developer entry point must be:

```bash
./run.sh
```

This command must **build the debug APK using Docker, install it, and launch the app** on a connected Android phone or a running Android emulator. It must not require manually running Gradle, finding APK paths, using `adb install`, or opening Android Studio for each build.

**Important platform distinction:** A native Android UI does **not** run inside an ordinary Docker container. Docker builds the APK, and the APK runs on Android hardware or an Android emulator. Do not invent a backend or web server to satisfy this requirement. A compatible emulator/AVD or physical phone and its ADB connection may require **one-time setup**. After that setup, `./run.sh` performs the routine build-and-run workflow. Never claim Docker alone can display the Android app on every host OS.

### Required repository files

- `Dockerfile`: a reproducible Android build image with a compatible pinned JDK, Android SDK command-line tools, licensed SDK platforms/build-tools/platform-tools, and dependencies needed by the Gradle wrapper. Install Android components at image-build time where practicable. Use supported, version-compatible combinations and document them; avoid `latest` for critical tools.
- `compose.yaml` (or `docker-compose.yml`): an `android-builder` service that mounts the project, uses the Dockerfile, caches Gradle and SDK artifacts persistently, and runs a headless debug APK build. It must not expose an unnecessary server port or attach an unrelated database/backend service.
- `.dockerignore`: exclude `.git`, local build outputs, `.gradle` directories, keystores, secrets, and transient IDE files from build context; do not exclude the source files needed for building.
- `run.sh`: executable, portable developer entry point that checks prerequisites, invokes the Docker Compose build, locates the generated debug APK deterministically, uses host-side `adb` to install it, and launches the actual application package/activity. Determine application ID from the project configuration; never hardcode an incorrect placeholder package name.
- `README.md`: quick-start instructions, one-time prerequisites, commands to run tests, troubleshooting for ADB/emulator/device connection, and the difference between building in Docker and running on Android.

### Required commands and behavior

1. **Build and launch:** `./run.sh` is the default single command for developers with a connected phone or a booted emulator. It should print clear build/install/launch stages and return a nonzero exit code on failure.
2. **Build only:** `docker compose run --rm android-builder` (or a similarly documented single Docker Compose command) produces `app-debug.apk` in a documented host-accessible path. Do not store the only output inside a disposable container.
3. **Tests:** provide a Compose invocation or `./test.sh` for local JVM unit tests (`testDebugUnitTest` or the actual module task). Instrumented tests require a real/emulated Android target and must be documented separately, not falsely represented as runnable in a build-only container.
4. **Existing emulator/device:** if exactly one ADB device is ready, use it. If multiple devices are ready, allow `ANDROID_SERIAL` or an explicit device-selection argument. Do not silently install on the wrong phone.
5. **No device found:** optionally auto-start an existing host AVD if supported, wait until `sys.boot_completed=1`, then deploy. If host emulator startup is unavailable, fail clearly with exact prerequisites; don't claim success or hang indefinitely. A **fully containerized emulator** is optional only if host platform and hardware acceleration support it; never make KVM availability a mandatory assumption on macOS, Windows, or non-KVM hosts.
6. **Reliability:** set suitable exit-status propagation, avoid interactive prompts in normal builds, make repeated invocations idempotent, and use Gradle/SDK caching so repeated builds are reasonably fast.
7. **Portability:** explain host requirements (Docker Engine/Desktop, Docker Compose v2, ADB/platform-tools, and a running emulator or USB-debuggable device). Support Linux and macOS where feasible; on Windows document WSL2 or Git Bash and ADB/USB/emulator host integration. Never silently rely on Linux-only networking such as `--network=host` for every platform.
8. **Privacy/security:** Docker build must not require app accounts, health data, keystore secrets, or internet access **at runtime**. Network may be necessary to download Docker/SDK/Gradle dependencies on first build. Do not embed production signing credentials into the Docker image or repository. No app `INTERNET` permission should be added solely for Docker tooling.

### Definition of done for Docker support

- From a clean checkout with the documented one-time prerequisites present, `./run.sh` builds the debug APK inside Docker, installs it, and opens the Calendar screen on the chosen Android target.
- The same repo can run the build without Android Studio through Docker Compose.
- The README explains that a device/emulator is necessary for displaying a native Android app.
- Build failures, unavailable ADB, missing hardware, and multiple devices produce clear actionable errors; no spurious success messages.
- Execute and verify the workflow where the environment permits. If the runner lacks Docker, ADB, hardware acceleration, or a target device, deliver the implementation and report precisely which parts were not executed.

**End of implementation specification.**
