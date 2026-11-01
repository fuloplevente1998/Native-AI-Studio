# Photo / Health Journal restore and interrupted replay verification

These tests extend `BackupImportValidationRegressionTest` using the existing
`BackupRestoreManager`, Room database, native SQLite Health Journal and durable
replacement receipts. Runtime application code, application ID, version, signing
and Compose UI are unchanged by this increment.

## Added cases

- ZIP export/import preserves referenced photo binary bytes and consented Health
  records. A separate SQLite helper reads the persisted Health Journal after restore.
- A changed archived photo fails the original SHA256 manifest verification before
  a replacement receipt is installed, retaining the current Room workout, live photo
  and Health record.
- Without Health backup consent, JSON export omits `nativeHealthJournal` and leaves
  the live Health record intact.
- Inject an `Error` at each of nine durable restore checkpoints: receipt prepared,
  individual photo installed, photos installed, Health prepared, Health installed,
  Room/settings imported, Health committed, data imported and receipt committed.
  A fresh manager must replay the durable receipt, restore the photo/Health/Room
  data, retire the fences and preserve the selected archive bytes.

The injected error deliberately bypasses ordinary exception rollback, exercising
on-disk intermediate state. The process still executes Kotlin `finally` blocks;
this is **not** evidence of a real Android process kill or physical-device restart.
Photo fixtures check binary archive preservation, not decoding or UI rendering.
Health fixtures are confined to unit tests and never supplied to the runtime UI.

## Remaining verification

Run `:app:assembleDebug :app:testDebugUnitTest` in Actions and inspect raw XML before
calling these cases successful. Real phone process death, photo rendering, external
Health provider sync and cloud import must still be checked on devices/accounts.
Do not clear user data or remove a recovery receipt as a workaround.

## Test fixture diagnosis

The first replay attempt failed at `receipt-prepared`: the cached Health singleton
contained the restored record (count 1), while a helper constructed with the new
Robolectric application context read count 0. Production singleton connections had
survived the prior test method and referenced that method's files. The fixture now
closes/resets the cached Room and Health connections before opening the current
context's files, and explicitly checks the Health database path. The independent
reopen and strict record-count/content assertions remain enabled. This changes test
isolation only; it does not clear real application data or relax restore validation.
