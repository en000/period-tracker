# Cycle Journal privacy

Cycle Journal stores period dates, daily observations, notes and preferences in
Android app-private storage. It has no account, server, advertising, analytics or
remote reporting. The app does not request internet permission.

Local reminders are optional. Notification text is discreet by default. Android
controls notification delivery and may delay reminders.

Manual backup files contain the user dataset and preferences, encrypted using
AES-256-GCM with PBKDF2-HMAC-SHA256 (600,000 iterations), a random salt and nonce.
The user chooses the file destination through Android's document picker. A chosen
document provider may itself sync exported files; that behavior belongs to the
provider. Forgotten backup passphrases cannot be recovered.

Import replaces local data only after authentication, validation and confirmation.
Delete all data removes local records, preferences and reminder receipts. It does
not delete exported files. Uninstalling without a backup may destroy all records.
Android automatic backup and transfer exclusions are configured; OEM behavior
must be checked on release devices before making stronger guarantees.

Before publishing, the publisher must provide their identity, contact address and
a publicly accessible privacy-policy URL, review current Play health declarations
and Data safety responses, and verify these statements against the release APK.
