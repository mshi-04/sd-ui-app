# SD UI

[日本語](README.md) | English

An Android client created to explore Server-Driven UI. This repository preserves its Kotlin and Jetpack Compose implementation and development history as learning material.

The companion API and infrastructure are in [sd-ui-infra](https://github.com/mshi-04/sd-ui-infra). Running the app requires your own API and Microsoft Entra ID configuration. Configuration examples in this README are illustrative placeholders.

The detailed documents also preserve notes about the original development environment. Treat configuration values in the public archive as samples and adapt them to your own environment.

## Features

- Entra ID sign-in through MSAL, with login and dashboard screens selected by authentication state
- Fetching a server-resolved UI schema through `GET {API_BASE_URL}ui/dashboard`
- Rendering a tree of `Card` / `List` / `Chart` / `Button` / `Banner` / `Panel` components, including theme and density settings
- Skipping unknown component types and displaying distinct states for HTTP errors and invalid responses
- Conditional requests with `ETag` / `If-None-Match`, reusing the retained schema after a 304 response

## Structure

The single `app` module uses Clean Architecture and MVVM package boundaries. The basic dependency direction is `ui -> domain <- data`.

- `ui`: Compose routes and screens, ViewModels, UI state, and themes
- `domain`: Android-independent models, value objects, repository interfaces, and use cases
- `data`: Retrofit APIs, DTOs, mappers, repository implementations, the MSAL client, and Hilt modules

The main libraries are Jetpack Compose / Material 3, Hilt, Coroutines / StateFlow, Retrofit / OkHttp, and kotlinx.serialization. Dependency versions are maintained in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Prerequisites

| Item | Repository configuration |
| --- | --- |
| JDK | 17 (`jvmToolchain(17)`) |
| Android SDK | compileSdk 37.1 / targetSdk 37 / minSdk 24 |
| Android Gradle Plugin | 9.4.1 |
| Gradle | Use the included 9.8.0 Wrapper |

Use an Android Studio version that supports this build configuration and install Android SDK Platform 37.1. Running the app requires Android 7.0 (API 24) or later. Set the local SDK location through `sdk.dir` in `local.properties`; keep that file out of commits.

## Configure your own environment

1. Follow [sd-ui-infra](https://github.com/mshi-04/sd-ui-infra) to prepare an API and separate Entra ID app registrations for the API and Android client. Configure the API's `UiSchema.Read` scope and permission for the Android client to use it.
2. Set `API_BASE_URL` and `API_SCOPE` in `gradle.properties` to your own environment's values.
3. Update `client_id`, `authorities[].audience.tenant_id`, and `redirect_uri` in [`app/src/main/res/raw/msal_config.json`](app/src/main/res/raw/msal_config.json) to match your Android app registration and signing certificate.
4. Configure the same package name and signing hash for `BrowserTabActivity` in [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml), and register the corresponding redirect URI in Entra ID.

Example Gradle properties follow. Replace `example.com` and `<api-application-id>` with your own values.

```properties
API_BASE_URL=https://example.com/api/
API_SCOPE=api://<api-application-id>/UiSchema.Read
```

Include `/api/` in `API_BASE_URL`; the build adds a trailing `/` when needed. You can override these properties with `-PAPI_BASE_URL=...` and `-PAPI_SCOPE=...`. Missing or empty values fail the build. Putting these API properties in `local.properties` does not make them available to the build.

The MSAL redirect URI has the form `msauth://com.example.sdui/<URL-encoded-signature-hash>`. URL-encode the hash in the JSON configuration and use `/<unencoded-signature-hash>` for the Manifest's `android:path`. Replace the illustrative configuration with values derived from your own signing certificate before using authentication. See [authentication configuration (Japanese)](docs/authentication.md) for details.

## Authentication behavior

At startup, the app signs out any cached account from the previous app session and requires sign-in on each launch. The browser's Entra ID session can remain active, so SSO may complete sign-in without another password prompt.

After sign-in, the dashboard fetches its UI schema. An API response of 401 triggers sign-out and returns to the login screen; a failed sign-out displays a retry screen.

Access tokens are attached as `Authorization: Bearer` only to HTTPS requests whose host and port match the configured API base URL.

## Connect to a local API

The companion repository's `scripts/dev_server.py` is a development server that returns seed schemas without validating tokens. Start it using the instructions in [sd-ui-infra](https://github.com/mshi-04/sd-ui-infra), complete the MSAL and `API_SCOPE` configuration above, and point the emulator at the local endpoint.

```powershell
.\gradlew.bat :app:installDebug "-PAPI_BASE_URL=http://10.0.2.2:8080/api/"
```

`10.0.2.2` addresses the host computer from an Android emulator. Cleartext HTTP is permitted only for `10.0.2.2` and `localhost` in debug builds.

The Android app still requires MSAL sign-in when using the local API. HTTP requests carry no access token, and the development server returns `usage-summary` for a user without assigned roles.

## Build and verification

After configuration, run these commands from the repository root. The examples use Windows PowerShell; on other platforms, replace `.\gradlew.bat` with `./gradlew`.

```powershell
# Build the debug app
.\gradlew.bat :app:assembleDebug

# JVM unit tests
.\gradlew.bat :app:testDebugUnitTest

# Android Lint
.\gradlew.bat :app:lintDebug

# Install on a device or emulator
.\gradlew.bat :app:installDebug

# Compose UI tests requiring a device or emulator
.\gradlew.bat :app:connectedDebugAndroidTest
```

JVM tests use JUnit Jupiter, MockK, Turbine, and kotlinx-coroutines-test. Device tests use the AndroidX Test JUnit4 runner. Coverage areas include JSON mapping, conditional requests, authentication, and UI state transitions.

## Sample limitations

- Components primarily render labels and containers. Fetching actual `Chart` / `List` data and executing component actions are not implemented; schema-defined `Button` components are disabled.
- `props` retain primitive values as strings; objects, arrays, and `null` are discarded.
- UI schemas and ETags live only in process memory and are cleared on sign-out. There is no persistence or HTTP cache that implements `Cache-Control` behavior.
- Release authentication and distribution configuration are not provided. Redirect URI registration and configuration must match the signing certificate you use.
- Build flavors, a local database, and CI are not implemented. The app UI is Japanese; this English README does not add English localization to the app.

## Related documentation

- [sd-ui-infra: companion server and infrastructure](https://github.com/mshi-04/sd-ui-infra)
- [Server-side client integration contract (Japanese)](https://github.com/mshi-04/sd-ui-infra/blob/main/docs/client_integration.md)
- [Project overview (Japanese)](docs/project-overview.md)
- [Architecture (Japanese)](docs/architecture.md)
- [Development setup and verification commands (Japanese)](docs/development-setup.md)
- [Authentication (Japanese)](docs/authentication.md)
- [Client-side server contract (Japanese)](docs/server-contract.md)
- [Unit testing guidelines (Japanese)](docs/unit-test.md)
