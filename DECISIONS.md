# Cycle Journal implementation decisions

The functional source of truth is `docs/Cycle_Journal_Codex_Spec_v1_1.md`.
The working name, violet palette, typography and layouts are provisional, not approved branding.

- Retain the existing application ID `com.periodtracker` and native Compose project.
- Minimum Android version is API 26, as specified. Domain dates use `LocalDate`, never timestamp-based day increments.
- Prediction algorithm version 1 uses half-up rounding, the latest six observed intervals, and the latest six completed durations. Unknown endings contribute only start dates.
- Flag timing variation when the recent maximum minus minimum interval exceeds seven days. This only changes explanatory presentation, never removes observations.
- Suggest reviewing an explicit ongoing record after 15 days; do not diagnose or automatically end it.
- Validate the entire candidate period dataset inside the persistence transaction. Daily logs are independent of periods.
- Validate unusual intervals separately from hard overlap errors: intervals outside 15–90 days require explanatory confirmation, not rejection.

## Environment observed on 2026-11-16

- Existing project uses AGP 9.5.1, Gradle 9.6.0 and SDK 37.
- Gradle wrapper runs; Android SDK is installed locally.
- Android platform-tools are installed, but `adb devices` reports no connected target.
- Device installation and instrumented UI checks were initially unverified until their prerequisites became available.

## Application implementation

- Single `MainActivity`, Compose Navigation, application-scoped manual dependency graph.
- Data sources feed repository implementations behind domain interfaces; use cases feed ViewModels and Compose views.
- Room v1 schema is exported. No destructive migrations. Settings replacement is journaled in the same Room transaction as records, then written to DataStore.
- AES-256-GCM backup uses a 96-bit random nonce, 128-bit random salt, PBKDF2-HMAC-SHA256 at 600,000 iterations and authenticated binary headers; maximum file size is 20 MiB.
- WorkManager polls every 15 minutes as best effort, with durable reminder receipts and no exact alarm permission. Dates and reminder time are evaluated in the current local timezone.
- Changed daemon toolchain to locally available Java 17 after Java 25 provisioning failed.
- Windows dependency resolution required using Windows-ROOT trust; TLS validation remains enabled.
- Android Studio and the Gradle wrapper are the supported build workflows.
- Complete UI string resource extraction, comprehensive UI automation and real-device accessibility checks remain release work; do not treat the build as production validation.

## Quality pass

- Period and daily-log forms now have scoped ViewModels backed by SavedStateHandle; form validation is independently testable and opening an unchanged period no longer triggers a false discard warning.
- Room observation reads a transactionally consistent snapshot instead of combining independently emitted table contents. Daily-log edits use a date-indexed lookup.
- Added load retry UI, specific privacy-safe backup failures, numeric keyboards and range error indicators, and observable calendar locale formatting.
- Extracted 45 static UI messages; complete localization of dynamic copy and enum labels remains pending.
- Added draft regression tests and calendar Compose tests. The initial repository device tests passed on the connected Samsung phone; the phone disconnected before the subsequent calendar UI run.
- Launcher artwork is a replaceable violet cycle mark, not final branding.
