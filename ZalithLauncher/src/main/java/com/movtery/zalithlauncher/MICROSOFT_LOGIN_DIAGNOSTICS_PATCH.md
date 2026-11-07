# Microsoft Login diagnostics patch

## Changes

- Added stage markers for Xbox Live, XSTS, Minecraft Services, and Minecraft ownership verification.
- Added bounded, credential-redacted response diagnostics for Microsoft/Xbox/Minecraft HTTP failures.
- Changed Minecraft Services HTTP 403 handling: a generic 403 is no longer automatically labeled as an IP block. The launcher only uses the `BLOCKED_IP` message when the response body explicitly mentions an IP address together with blocked/denied/restricted wording.
- Preserved the existing Microsoft Device Code Flow, XBL/XSTS exchange, ownership check, and account creation behavior.
- Kept HTTP 429 mapped to the existing rate-limit message.

## Important build limitation

The supplied ZIP is a source subset (it contains Kotlin/Java source files but no Gradle wrapper, Gradle build files, settings, Android resources, or dependency configuration). Therefore this patch ZIP cannot be built independently and a full Android compile could not be run against the supplied archive. Apply the changed `game/account/microsoft/MicrosoftAuthenticator.kt` to the complete Zalith project and build there.

## How to diagnose the next login attempt

1. Install a build made from the patched source.
2. Try Microsoft login once.
3. Export the launcher log.
4. Search for `MicrosoftAuth` and the `Microsoft login stage:` markers.
5. Share the log only after removing account email and any credentials. This patch redacts common token fields and bearer/JWT strings in the new authentication response diagnostics, but users should still review exported logs before sharing.

This patch does not bypass Microsoft, Xbox, or Minecraft access controls and cannot unblock an IP. It makes the failing stage and server response clearer so the actual cause can be addressed legitimately.
