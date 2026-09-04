# ProPass Digital Identity System — Project Handoff

> **Date:** August 27, 2026  
> **Status:** Frontend / UI Phase Complete & Physically Verified on Android Device  
> **Target OS:** Android (Min SDK: 24, Target SDK: 37, Java 11)  
> **Architecture:** Native Android Kotlin + XML Layouts + Material Design 3  

---

## 1. Project Overview

**ProPass** is a modern digital identity and credential management platform tailored for enterprise campuses, tech conferences, visitor management, and organizations. The mobile application allows users to:
* Manage a unified, verified digital identity pass.
* Scan physical and digital event QR codes in real time using CameraX and ML Kit.
* Auto-fill organizational registration forms with verified identity tokens.
* Review registration details and receive instant credential confirmations.
* Access high-resolution digital passes with contact actions and Google Wallet integration.

Currently, the Android application contains a complete, polished, and fully functional **frontend client** with real camera integration, live QR parsing, form validation, and local mock data routing.

---

## 2. Current Implementation Status

The **Frontend / UI Implementation Phase** is 100% complete and has been physically tested and verified on a live Android device (Serial: `KVAMAAXSPNOV7PXC`).

### Implemented End-to-End Navigation Flow:
```
SplashActivity
 └── LoginActivity
      └── OnboardingProfileCreationActivity
           └── OnboardingSmartScanActivity
                └── SmartFormRegistrationActivity (Direct or from Scan)
                     ├── HomeDashboardActivity ⇄ DigitalPassActivity
                     │                              └── ScanQRActivity (CameraX + ML Kit)
                     │                                   └── [Valid ProPass QR]
                     │                                        └── SmartFormRegistrationActivity
                     └── ReviewRegistrationActivity
                          └── RegistrationSuccessActivity
                               ├── "View My Pass" ➔ DigitalPassActivity
                               └── "Go Home"      ➔ HomeDashboardActivity
```

---

## 3. Frontend Screens Implemented

### 1. Splash Screen
* **File:** [`app/src/main/java/com/mpc/propass/SplashActivity.kt`](file:///home/divy/AndroidProjects/ProPass/app/src/main/java/com/mpc/propass/SplashActivity.kt)
* **Layout:** [`app/src/main/res/layout/activity_splash.xml`](file:///home/divy/AndroidProjects/ProPass/app/src/main/res/layout/activity_splash.xml)
* **Components:** Animated 88dp rounded ProPass logo, app title with Inter Bold typography, tagline, and custom smooth animated horizontal loader.
* **Interactions:** Automatically transitions to `LoginActivity` after 3000ms.

### 2. Login Screen
* **File:** [`app/src/main/java/com/mpc/propass/LoginActivity.kt`](file:///home/divy/AndroidProjects/ProPass/app/src/main/java/com/mpc/propass/LoginActivity.kt)
* **Layout:** [`app/src/main/res/layout/activity_login.xml`](file:///home/divy/AndroidProjects/ProPass/app/src/main/res/layout/activity_login.xml)
* **Components:** Floating frosted icon container, email & password input fields with error states, password visibility toggle, primary "Login" button with progress state, and "Continue with Google" button.
* **Interactions:** Validates email and password; launches `OnboardingProfileCreationActivity` upon login.

### 3. Onboarding — Profile Creation
* **File:** [`app/src/main/java/com/mpc/propass/OnboardingProfileCreationActivity.kt`](file:///home/divy/AndroidProjects/ProPass/app/src/main/java/com/mpc/propass/OnboardingProfileCreationActivity.kt)
* **Layout:** [`app/src/main/res/layout/activity_onboarding_profile_creation.xml`](file:///home/divy/AndroidProjects/ProPass/app/src/main/res/layout/activity_onboarding_profile_creation.xml)
* **Components:** Top progress indicator (Slide 1/2 active), floating profile preview card with avatar and verified badge, skeleton information bars, "Next" button, and "Skip" action.
* **Interactions:** "Next" navigates to `OnboardingSmartScanActivity`; "Skip" jumps directly to `HomeDashboardActivity`.

### 4. Onboarding — Smart Scan
* **File:** [`app/src/main/java/com/mpc/propass/OnboardingSmartScanActivity.kt`](file:///home/divy/AndroidProjects/ProPass/app/src/main/java/com/mpc/propass/OnboardingSmartScanActivity.kt)
* **Layout:** [`app/src/main/res/layout/activity_onboarding_smart_scan.xml`](file:///home/divy/AndroidProjects/ProPass/app/src/main/res/layout/activity_onboarding_smart_scan.xml)
* **Components:** Top progress indicator (Slide 2/2 active), interactive QR document scan illustration with cyan/blue laser beam, and primary "Get Started" button.
* **Interactions:** "Get Started" launches `SmartFormRegistrationActivity`.

### 5. Smart Form Registration
* **File:** [`app/src/main/java/com/mpc/propass/SmartFormRegistrationActivity.kt`](file:///home/divy/AndroidProjects/ProPass/app/src/main/java/com/mpc/propass/SmartFormRegistrationActivity.kt)
* **Layout:** [`app/src/main/res/layout/activity_smart_form_registration.xml`](file:///home/divy/AndroidProjects/ProPass/app/src/main/res/layout/activity_smart_form_registration.xml)
* **Components:** Translucent frosted top bar with back navigation, dynamic event header (Overline, Title, Subtitle), auto-filled verified identity card (Full Name, Email, Institution) with in-place edit dialogs, purpose of visit selector dialog, numeric duration input, optional vehicle registration input, and sticky "Review & Submit" button.
* **Interactions:** Performs local validation on all required fields; highlights errors with red borders and inline messages; launches `ReviewRegistrationActivity` with `RegistrationData`.

### 6. Home Dashboard
* **File:** [`app/src/main/java/com/mpc/propass/HomeDashboardActivity.kt`](file:///home/divy/AndroidProjects/ProPass/app/src/main/java/com/mpc/propass/HomeDashboardActivity.kt)
* **Layout:** [`app/src/main/res/layout/activity_home_dashboard.xml`](file:///home/divy/AndroidProjects/ProPass/app/src/main/res/layout/activity_home_dashboard.xml)
* **Custom Views:** [`CircularProgressView.kt`](file:///home/divy/AndroidProjects/ProPass/app/src/main/java/com/mpc/propass/CircularProgressView.kt) (smooth animated 85% completion ring).
* **Components:** Royal blue ProPass Hero Card (`Sarah Jenkins`, `Senior UX Researcher`, QR thumbnail), Quick Actions grid ("Scan QR", "My Pass"), Profile Completion card, Recent Activity log, and 4-tab bottom navigation with center elevated Scan FAB.
* **Interactions:** Scan actions open `ScanQRActivity`; My Pass actions open `DigitalPassActivity`.

### 7. Digital Pass
* **File:** [`app/src/main/java/com/mpc/propass/DigitalPassActivity.kt`](file:///home/divy/AndroidProjects/ProPass/app/src/main/java/com/mpc/propass/DigitalPassActivity.kt)
* **Layout:** [`app/src/main/res/layout/activity_digital_pass.xml`](file:///home/divy/AndroidProjects/ProPass/app/src/main/res/layout/activity_digital_pass.xml)
* **Components:** High-fidelity digital pass container (`Elena Rodriguez`, `Lead Product Designer`, `Acme Corp`), verified badge, circular contact buttons (Email, Call, LinkedIn), 220dp QR code element, action buttons ("Share Link", "Save Image", "To Wallet"), and bottom navigation.
* **Interactions:** Contact buttons show mock feedback; "To Wallet" triggers simulated Google Wallet confirmation; Scan FAB opens `ScanQRActivity`.

### 8. Scan QR
* **File:** [`app/src/main/java/com/mpc/propass/ScanQRActivity.kt`](file:///home/divy/AndroidProjects/ProPass/app/src/main/java/com/mpc/propass/ScanQRActivity.kt)
* **Layout:** [`app/src/main/res/layout/activity_scan_qr.xml`](file:///home/divy/AndroidProjects/ProPass/app/src/main/res/layout/activity_scan_qr.xml)
* **Components:** Full-screen CameraX `PreviewView`, dark overlay mask, centered 256dp viewfinder with 4 blue L-brackets, 2000ms vertical laser scanning animation, floating hint capsule, flashlight control, center tactile shutter button, and gallery picker icon.
* **Interactions:** Real camera preview, real hardware torch control, Google ML Kit QR decoding, ProPass URL validation, and navigation to Smart Form.

### 9. Review Registration
* **File:** [`app/src/main/java/com/mpc/propass/ReviewRegistrationActivity.kt`](file:///home/divy/AndroidProjects/ProPass/app/src/main/java/com/mpc/propass/ReviewRegistrationActivity.kt)
* **Layout:** [`app/src/main/res/layout/activity_review_registration.xml`](file:///home/divy/AndroidProjects/ProPass/app/src/main/res/layout/activity_review_registration.xml)
* **Components:** Top app bar, Event Overview card, Verified Identity Details section, Visit Details section (Purpose, Duration, Vehicle), outlined "Back / Edit" button, and primary "Submit Registration" button.
* **Interactions:** "Back / Edit" returns to `SmartFormRegistrationActivity` with entered data intact; "Submit Registration" triggers simulated submission and navigates to `RegistrationSuccessActivity`.

### 10. Registration Success
* **File:** [`app/src/main/java/com/mpc/propass/RegistrationSuccessActivity.kt`](file:///home/divy/AndroidProjects/ProPass/app/src/main/java/com/mpc/propass/RegistrationSuccessActivity.kt)
* **Layout:** [`app/src/main/res/layout/activity_registration_success.xml`](file:///home/divy/AndroidProjects/ProPass/app/src/main/res/layout/activity_registration_success.xml)
* **Components:** Animated 88dp green check badge with overshoot scale effect, "Registration Complete" headline, registered event banner card with attendee name & purpose, "View My Pass" button, and "Go Home" button.
* **Interactions:** "View My Pass" opens `DigitalPassActivity`; "Go Home" opens `HomeDashboardActivity` with backstack clearance.

---

## 4. Scan QR Architecture

The `ScanQRActivity` integrates Android CameraX with Google ML Kit Barcode Scanning:

```
[ Camera Sensor ]
        │
        ▼
[ CameraX PreviewView (FILL_CENTER) ] ── (Underneath UI Overlay)
        │
        ▼
[ CameraX ImageAnalysis (KEEP_ONLY_LATEST) ]
        │
        ▼
[ Google ML Kit Barcode Scanner (FORMAT_QR_CODE) ]
        │
        ├── [ Raw Decoded String ] ── Logcat: "QR detected: <value>"
        │
        ▼
[ parseProPassEventId(url) ]
        ├── INVALID ──> Logcat: "QR rejected: Not a ProPass event QR"
        │               Debounced Toast: "Not a ProPass event QR"
        │               (Keeps CameraX scanning uninterrupted)
        │
        └── VALID ────> Logcat: "ProPass event ID: <eventId>"
                        Haptic Feedback (LONG_PRESS)
                        isQrDetected = true (Guard against multiple firings)
                        cameraProvider.unbindAll()
                        Intent ➔ SmartFormRegistrationActivity (EXTRA_EVENT_ID)
```

### Key Technical Specs:
* **CameraX:** `androidx.camera:camera-core:1.3.4`, `camera-camera2:1.3.4`, `camera-lifecycle:1.3.4`, `camera-view:1.3.4`.
* **ML Kit:** `com.google.mlkit:barcode-scanning:17.3.0`.
* **Runtime Permission:** Uses `ActivityResultContracts.RequestPermission()` for `Manifest.permission.CAMERA`. If denied, gracefully informs the user without crashing.
* **Physical Torch:** Controls the device flash via `camera.cameraControl.enableTorch(isFlashlightOn)` checked against `camera.cameraInfo.hasFlashUnit()`. Amber UI indicator glows when active.
* **ProPass QR Format:** Must match `^(?:https?)://(?:www\.)?propass\.id/event/([a-zA-Z0-9_-]+)/?$`.
* **Rejection:** Random browser URLs, Wi-Fi codes, vCards, phone numbers, and non-ProPass URLs are rejected without navigating or halting the scanner.

---

## 5. ProPass QR → Smart Form Flow

When a valid ProPass QR code is scanned:
1. `ScanQRActivity` extracts the lowercase `eventId` slug.
2. An intent with `EXTRA_EVENT_ID` is passed to `SmartFormRegistrationActivity`.
3. `SmartFormRegistrationActivity.setupEventHeader()` dynamically populates the event header:

| `eventId` Slug | Event Overline | Event Title | Event Subtitle |
| :--- | :--- | :--- | :--- |
| `techconf-2024` | `EVENT REGISTRATION` | `TechConf 2024` | Complete your registration to secure your spot. |
| `google-office-visit` | `VISITOR PASS` | `Google Office Visit` | Check in for your scheduled campus visit. |
| `android-conf-2026` | `CONFERENCE PASS` | `Android Conf 2026` | Secure your badge for Android developer sessions. |
| *`<any-custom-slug>`* | `EVENT REGISTRATION` | Formats hyphen/underscore slug into Title Case (e.g. `summit-2025` ➔ `Summit 2025`) | Complete your registration to secure your spot. |

---

## 6. Smart Form Validation Rules

`SmartFormRegistrationActivity` validates the local form before advancing:

1. **Full Name:** Required, non-empty. Auto-filled with mock profile (`Alex Morgan`). Tapping the row opens an edit dialog.
2. **Email Address:** Required, non-empty, must match `Patterns.EMAIL_ADDRESS`. Auto-filled (`alex.morgan@example.com`). Tapping row opens edit dialog.
3. **Institution:** Required, non-empty. Auto-filled (`University of Technology`). Tapping row opens edit dialog.
4. **Purpose of Visit:** Required. Must be selected from `General Attendee`, `Speaker`, `Sponsor/Exhibitor`, `Media/Press`.
5. **Expected Duration:** Required. Must be a numeric integer between `1` and `5` days.
6. **Vehicle Number:** Optional. Formatted to uppercase automatically (`InputFilter.AllCaps()`).

**Error Feedback:**
* Inline red error text (`@color/error`, `#BA1A1A`) appears below the invalid field.
* Fields switch to red border drawables (`bg_autofill_row_error.xml` or `bg_field_error.xml`).
* Haptic rejection feedback triggers.
* Errors automatically dismiss when the user edits or fixes the field.

---

## 7. Review & Registration Flow

When form validation passes, the application instantiates a [`RegistrationData`](file:///home/divy/AndroidProjects/ProPass/app/src/main/java/com/mpc/propass/RegistrationData.kt) model:

```kotlin
data class RegistrationData(
    val eventId: String,
    val eventName: String,
    val fullName: String,
    val email: String,
    val institution: String,
    val purpose: String,
    val durationDays: Int,
    val vehicleNumber: String? = null
) : Serializable
```

* **Review Screen (`ReviewRegistrationActivity`):** Displays all fields in structured Material cards.
* **Back / Edit:** Invokes `finish()`, returning the user to `SmartFormRegistrationActivity` with their entered fields preserved in memory.
* **Submit Registration:** Simulates submission progress and opens `RegistrationSuccessActivity`.

> [!NOTE]
> Registration submission is currently **LOCAL/MOCK**. No remote network request is made.

---

## 8. Digital Pass Actions (Mock vs. Integrated)

In [`DigitalPassActivity`](file:///home/divy/AndroidProjects/ProPass/app/src/main/java/com/mpc/propass/DigitalPassActivity.kt):
* **UI Rendering:** Fully integrated native XML layout with QR code container and verified status.
* **Email / Call / LinkedIn:** Mock UI touch feedback and Toast prompts.
* **Share Link / Save Image:** Mock UI touch feedback.
* **To Wallet:** Displays simulated Google Wallet addition confirmation Toast.
* **Scan QR / Bottom Nav:** Fully wired navigation to `ScanQRActivity`, `HomeDashboardActivity`.

---

## 9. Backend Status

> [!IMPORTANT]
> **BACKEND HAS NOT BEEN IMPLEMENTED YET.**
> There is currently **no backend server, no database, no REST API client (Retrofit/Ktor), no Firebase, and no cloud authentication**.

All data currently displayed in the app is local mock data.

### What Needs to Be Implemented in the Backend Phase:
1. **User / Profile Management:** User accounts, credentials, contact details, organization affiliations.
2. **Authentication Service:** JWT / OAuth authentication (e.g. Google Sign-In backend verification).
3. **Event Catalog Service:** Event definitions, registration limits, venue rules, dates.
4. **QR Code Generator & Verifier:** Signed/encrypted QR tokens for events and user passes.
5. **Registration Service:** Endpoint to receive, validate, and store event registrations.
6. **Digital Pass Service:** Syncing pass credentials, pass tiers, and wallet pass generation (PKPass / Google Wallet API).
7. **Android API Client:** Retrofit/OkHttp network layer with repository pattern, Coroutines/Flow, and offline caching.

---

## 10. Recommended Backend Direction

For the upcoming backend development phase, the recommended architecture is:

* **Runtime & Framework:** **Node.js with TypeScript** using **Express**, **Fastify**, or **NestJS**.
* **Database:** **PostgreSQL** (relational data for users, events, registrations, organizations) with **Prisma ORM** or **Drizzle ORM**.
* **Authentication:** JWT / Bearer tokens + OAuth2 (Google Sign-In).
* **API Paradigm:** RESTful JSON APIs (OpenAPI / Swagger documentation).
* **Android Networking:** **Retrofit 2** + **Moshi/Kotlinx Serialization** + **OkHttp 3** (with logging and auth interceptors).

*(This stack is planned and subject to final confirmation before backend development begins).*

---

## 11. Project Dependencies

From [`gradle/libs.versions.toml`](file:///home/divy/AndroidProjects/ProPass/gradle/libs.versions.toml) and [`app/build.gradle.kts`](file:///home/divy/AndroidProjects/ProPass/app/build.gradle.kts):

| Dependency | Version | Purpose |
| :--- | :--- | :--- |
| **Android Gradle Plugin (AGP)** | `8.3.2` / `9.3.2` | Android build system |
| **Kotlin** | Compatible with AGP / Java 11 | Programming language |
| **AndroidX Core KTX** | `1.10.1` | Kotlin extensions for Android Core |
| **AndroidX AppCompat** | `1.6.1` | Backward-compatible Android components |
| **Material Components** | `1.10.0` | Material Design 3 UI components |
| **AndroidX CameraX Core** | `1.3.4` | CameraX core camera controller |
| **AndroidX CameraX Camera2** | `1.3.4` | Camera2 implementation for CameraX |
| **AndroidX CameraX Lifecycle** | `1.3.4` | Lifecycle-aware camera binding |
| **AndroidX CameraX View** | `1.3.4` | `PreviewView` viewfinder component |
| **Google ML Kit Barcode Scanning** | `17.3.0` | On-device QR barcode scanning |
| **JUnit** | `4.13.2` | Local unit testing |
| **AndroidX Test JUnit / Espresso** | `1.1.5` / `3.5.1` | Instrumented Android UI testing |

---

## 12. Important Project Files

### Kotlin Sources (`app/src/main/java/com/mpc/propass/`):
* `SplashActivity.kt` — App launch screen & animated loader.
* `LoginActivity.kt` — Email/password login & Google sign-in UI.
* `OnboardingProfileCreationActivity.kt` — Profile onboarding flow (step 1).
* `OnboardingSmartScanActivity.kt` — Smart scan explanation flow (step 2).
* `SmartFormRegistrationActivity.kt` — Auto-fill registration form with local validation.
* `HomeDashboardActivity.kt` — Main dashboard with pass preview, recent activity & bottom nav.
* `DigitalPassActivity.kt` — Full-screen digital pass credential & quick actions.
* `ScanQRActivity.kt` — CameraX + ML Kit live QR scanner with ProPass URL validation.
* `ReviewRegistrationActivity.kt` — Registration summary review screen before submission.
* `RegistrationSuccessActivity.kt` — Animated confirmation screen after submission.
* `CircularProgressView.kt` — Custom Canvas view for smooth circular profile progress.
* `RegistrationData.kt` — Serializable data model for event registration details.
* `network/config/NetworkConfig.kt` — Centralized base URL configuration, timeouts, and emulator/hardware device routing.
* `network/model/ApiResponse.kt` — Generic Fastify API response envelope (`success`, `data`, `message`, `error`, `statusCode`, `timestamp`).
* `network/model/ApiError.kt` — Structured API error and validation error details model.
* `network/model/AuthModels.kt` — Moshi DTOs for authentication (`LoginRequest`, `RegisterRequest`, `RefreshTokenRequest`, `AuthUserDto`, `AuthTokensDto`, `AuthResponseData`, `RefreshResponseData`, `MeResponseData`).
* `network/interceptor/TokenProvider.kt` — Abstraction for providing authentication tokens (`TokenProvider`, `InMemoryTokenProvider`, `NoOpTokenProvider`).
* `network/interceptor/DataStoreTokenProvider.kt` — Non-blocking `TokenProvider` delegating to `TokenStorage` for OkHttp `AuthInterceptor`.
* `network/interceptor/AuthInterceptor.kt` — OkHttp interceptor injecting `Authorization: Bearer <token>` and `Accept: application/json`.
* `network/api/ProPassApiService.kt` — Retrofit 2 service interface mapping all Phase 1 and Phase 2 backend REST endpoints with typed auth models.
* `network/NetworkClient.kt` — Factory and singleton building Moshi, OkHttpClient with auth/logging interceptors, and Retrofit 2.
* `data/local/TokenStorage.kt` — Token storage interface & `DataStoreTokenStorage` using Jetpack Preferences DataStore with volatile cache.
* `data/repository/AuthRepository.kt` — Authentication repository (`login`, `register`, `refreshToken`, `logout`, `checkSession`, `hasActiveSession`) with Moshi error handling.
* `ProPassApplication.kt` — Application entrypoint initializing `TokenStorage`, registering `DataStoreTokenProvider` with `NetworkClient`, and exposing repository dependencies.

### Backend Sources (`backend/`):
* `src/server.ts` — Fastify server startup entrypoint.
* `src/app.ts` — Fastify application factory with Helmet, CORS, health, auth, user, dashboard, event, registration, and pass routes.
* `src/config/env.ts` — Zod environment variable parser (Port, Database, JWT config).
* `src/config/prisma.ts` — Singleton PrismaClient client connection.
* `src/controllers/health.controller.ts` — System health and database connectivity controller.
* `src/controllers/auth.controller.ts` — Authentication controller (Register, Login, Refresh, Logout, Me).
* `src/controllers/user.controller.ts` — User Profile & Dashboard controller (GetProfile, UpdateProfile, GetDashboard).
* `src/controllers/event.controller.ts` — Event & QR validation controller (GetEvent, ValidateQr).
* `src/controllers/registration.controller.ts` — Event registration controller (CreateRegistration, GetMyRegistrations).
* `src/controllers/pass.controller.ts` — Digital Pass controller (GetMyPass).
* `src/services/auth.service.ts` — Authentication service with Argon2 hashing and token rotation.
* `src/services/user.service.ts` — User Profile management, completion score calculation, and aggregated Dashboard service.
* `src/services/event.service.ts` — ProPass QR regex parser, event metadata retrieval, and active event validator.
* `src/services/registration.service.ts` — Event registration persistence, max duration validation, duplicate prevention, and snapshot management.
* `src/services/pass.service.ts` — Digital Pass retrieval, active/expiration calculation, and profile association service.
* `src/middleware/auth.middleware.ts` — Fastify JWT authentication guard decorator (`authenticate`).
* `src/models/auth.schema.ts` — Zod validation schemas for registration, login, and refresh.
* `src/models/user.schema.ts` — Zod validation schemas for profile updates.
* `src/models/event.schema.ts` — Zod validation schemas for QR validation (`validateQrSchema`) and event params (`eventParamsSchema`).
* `src/models/registration.schema.ts` — Zod validation schema for event registrations (`createRegistrationSchema`).
* `src/models/pass.schema.ts` — Digital Pass DTO definitions (`DigitalPassDto`, `PassHolderDto`, `MyPassResponse`).
* `src/utils/crypto.ts` — Argon2id password hashing, verification, secure token generation, and SHA-256 token hashing.
* `src/utils/jwt.ts` — JWT access token generation and verification.
* `src/routes/health.routes.ts` — Health check endpoint routes (`/health`, `/api/health`).
* `src/routes/auth.routes.ts` — Authentication routes (`/api/v1/auth/register`, `/login`, `/refresh`, `/logout`, `/me`).
* `src/routes/user.routes.ts` — User profile routes (`/api/v1/users/profile`).
* `src/routes/dashboard.routes.ts` — Home dashboard route (`/api/v1/dashboard`).
* `src/routes/event.routes.ts` — Event routes (`/api/v1/events/:eventId`, `/api/v1/events/validate-qr`).
* `src/routes/registration.routes.ts` — Registration routes (`/api/v1/registrations`, `/api/v1/registrations/my`).
* `src/routes/pass.routes.ts` — Digital Pass routes (`/api/v1/passes/me`).
* `prisma/schema.prisma` — PostgreSQL database schema (User, Profile, DigitalPass, Event, Registration, RefreshToken).
* `prisma/seed.ts` — Database seeder (TechConf 2024, Google Office Visit, Android Conf 2026, and demo accounts).
* `tests/health.test.ts` — Vitest integration tests for API foundation (2 tests passed).
* `tests/auth.test.ts` — Vitest test suite for Authentication (14 test cases).
* `tests/user.test.ts` — Vitest test suite for User Profile & Dashboard (8 test cases).
* `tests/event.test.ts` — Vitest test suite for Event catalog & QR validation (18 test cases).
* `tests/registration.test.ts` — Vitest test suite for Event Registration (20 test cases).
* `tests/pass.test.ts` — Vitest test suite for Digital Pass (10 test cases).
* `docker-compose.yml` — PostgreSQL 16 Alpine container with persistent volume.
* `Dockerfile` — Multi-stage production container build for Node.js backend.

### Layout XML Files (`app/src/main/res/layout/`):
* `activity_splash.xml`
* `activity_login.xml`
* `activity_onboarding_profile_creation.xml`
* `activity_onboarding_smart_scan.xml`
* `activity_smart_form_registration.xml`
* `activity_home_dashboard.xml`
* `activity_digital_pass.xml`
* `activity_scan_qr.xml`
* `activity_review_registration.xml`
* `activity_registration_success.xml`
* `item_dropdown_popup.xml`

### Key Resource Files:
* `app/src/main/res/values/colors.xml` — ProPass color tokens (Primary `#0032D7`, Surface Container `#F0F4F9`, Background `#F8F9FA`, Error `#BA1A1A`, etc.).
* `app/src/main/res/values/strings.xml` — All localized user-facing UI and error strings.
* `app/src/main/res/font/` — Inter font family (Regular, Medium, SemiBold, Bold).
* `app/src/main/AndroidManifest.xml` — Activity declarations, camera permissions, and feature tags.

### Unit Tests:
* `app/src/test/java/com/mpc/propass/ProPassQrValidationTest.kt` — Android unit tests for ProPass QR regex validation and URL parsing.
* `app/src/test/java/com/mpc/propass/ExampleUnitTest.kt` — Basic Android host unit test.
* `app/src/test/java/com/mpc/propass/network/NetworkConfigTest.kt` — Tests for base URL normalization, default emulator host, and timeout configurations.
* `app/src/test/java/com/mpc/propass/network/AuthInterceptorTest.kt` — Tests for Bearer token injection, Accept headers, and explicit header preservation.
* `app/src/test/java/com/mpc/propass/network/ApiResponseParsingTest.kt` — Tests for Moshi serialization and deserialization of standard Fastify envelopes and error models.
* `app/src/test/java/com/mpc/propass/network/NetworkClientIntegrationTest.kt` — MockWebServer end-to-end integration tests for Retrofit + OkHttp + Moshi.
* `app/src/test/java/com/mpc/propass/data/local/TokenStorageTest.kt` — Unit tests for DataStore token persistence and in-memory cache operations.
* `app/src/test/java/com/mpc/propass/data/local/FakeTokenStorage.kt` — In-memory test double for `TokenStorage`.
* `app/src/test/java/com/mpc/propass/network/interceptor/DataStoreTokenProviderTest.kt` — Unit tests for non-blocking token provider delegation.
* `app/src/test/java/com/mpc/propass/data/repository/AuthRepositoryTest.kt` — MockWebServer unit tests for login, register, token refresh, logout, and session restoration.
* `backend/tests/health.test.ts` — Backend Fastify health check test suite (2 tests passed).
* `backend/tests/auth.test.ts` — Backend authentication test suite (14 tests passed).
* `backend/tests/user.test.ts` — Backend user profile & dashboard test suite (8 tests passed).
* `backend/tests/event.test.ts` — Backend event catalog & QR validation test suite (18 tests passed).
* `backend/tests/registration.test.ts` — Backend event registration test suite (20 tests passed).
* `backend/tests/pass.test.ts` — Backend digital pass test suite (10 tests passed).

---

## 13. Git & Repository Status

* **Repository:** `ProPass`
* **Remote:** `git@github.com:Divy2095/ProPass.git`
* **Current Branch:** `main`
* **Important `.gitignore` Rules:**
  * Standard Android Gradle build artifacts (`/build`, `.gradle`, `*.iml`, `local.properties`).
  * `/.agents/` is explicitly gitignored because it previously contained GCP API secrets.
  * Backend local artifacts (`backend/node_modules/`, `backend/dist/`, `backend/.env`, `backend/coverage/`).

> [!CAUTION]
> **CRITICAL SECURITY RULE:** Never commit API keys, GCP credentials, service account JSON files, or secrets into this repository or this file.

---

## 14. Backend Quick Start Guide (For Developers)

To run the backend development environment:

```bash
# 1. Navigate to backend
cd backend

# 2. Configure environment (if not already present)
cp .env.example .env

# 3. Start PostgreSQL container
docker compose up -d

# 4. Install dependencies
npm install

# 5. Run Prisma migrations & seed database
npx prisma migrate dev
npm run prisma:seed

# 6. Run test suite
npm test

# 7. Start development server
npm run dev
# (Server listens on http://localhost:3000, verify via http://localhost:3000/health)
```

---

## 15. Known Limitations (Current Phase)

1. **Android Authentication & Token Persistence (Phase 3B):** Jetpack Preferences DataStore token persistence (`DataStoreTokenStorage`), non-blocking `DataStoreTokenProvider`, `AuthRepository`, session restoration in `SplashActivity`, and real login in `LoginActivity` are completed. User profile, dashboard, events, registration, and digital pass UI screens remain on local/mock data until Phase 3C/3D/3E.
2. **Android Network Integration:** User profile, dashboard, events, registration, and digital pass UI screens currently run on local/mock data until Phase 3C/3D/3E/4 integration.
3. **Mock Actions:** External wallet export, social sharing, and contact buttons on Android generate UI Toast feedback.

---

## 16. Verification Summary

All listed features have been executed and verified on physical hardware & development environment:

* [x] **Android Gradle Build:** `./gradlew assembleDebug` builds with 0 errors (`BUILD SUCCESSFUL`).
* [x] **Android Unit Tests:** 29/29 unit tests passing across 9 test suites (`./gradlew testDebugUnitTest`).
* [x] **Android Physical Device Features:** Live CameraX viewfinder, physical torch, ML Kit QR detection, local QR validation, Smart Form validation, Review & Success flows verified on device `KVAMAAXSPNOV7PXC`.
* [x] **Backend Foundation (Phase 1):**
  * Node.js v22 + TypeScript + Fastify + Prisma initialized.
  * PostgreSQL 16 container running and healthy via Docker Compose.
  * Prisma schema valid, migration applied, seed data populated.
  * Fastify `GET /health` responds `200 OK` with live `database: "connected"`.
* [x] **Backend Authentication (Phase 2A):**
  * Argon2id password hashing and validation implemented.
  * JWT access token (15m) and secure persistent refresh token (7d SHA-256 hashed) rotation implemented.
  * `POST /api/v1/auth/register` (201 Created), `POST /api/v1/auth/login` (200 OK), `POST /api/v1/auth/refresh` (200 OK), `POST /api/v1/auth/logout` (200 OK), `GET /api/v1/auth/me` (200 OK).
* [x] **Backend User & Profile API (Phase 2B):**
  * `GET /api/v1/users/profile` (200 OK) retrieves authenticated user profile and safe account details.
  * `PUT /api/v1/users/profile` (200 OK) updates profile fields and dynamically calculates `completionScore` on the backend.
  * `GET /api/v1/dashboard` (200 OK) aggregates live PostgreSQL data: greeting, user info, profile details, digital pass summary, and recent event activities.
* [x] **Backend Event & QR Validation API (Phase 2C):**
  * `GET /api/v1/events/:eventId` (200 OK) retrieves event metadata by slug or UUID.
  * `POST /api/v1/events/validate-qr` (200 OK) parses ProPass QR URL format (`https://propass.id/event/<eventId>`), verifies active event in PostgreSQL, and returns event data for registration forms.
* [x] **Backend Registration API (Phase 2D):**
  * `POST /api/v1/registrations` (201 Created) persists event registrations in PostgreSQL, enforces maximum event duration limits, prevents duplicate registrations (409 Conflict), and stores registration snapshot data.
  * `GET /api/v1/registrations/my` (200 OK) returns authenticated user's registration history with attached event details.
* [x] **Backend Digital Pass API (Phase 2E):**
  * `GET /api/v1/passes/me` (200 OK) retrieves authenticated user's active Digital Pass with calculated expiration status, pass tier, QR payload, and verified holder profile details.
  * 72/72 Vitest automated tests passed across all 6 backend test suites (100% pass rate).
* [x] **Android Networking Foundation (Phase 3A):**
  * Retrofit 2.11.0, OkHttp 4.12.0, Moshi 1.15.2, and OkHttp Logging Interceptor integrated.
  * `NetworkConfig` established for centralized base URL management (`10.0.2.2:3000` for emulator, `127.0.0.1:3000` via `adb reverse`, configurable for local LAN).
  * `ApiResponse<T>` and `ApiError` envelopes matching Fastify contract.
  * `TokenProvider` abstraction and `AuthInterceptor` for Bearer token injection.
  * `ProPassApiService` mapping Fastify endpoints.
  * MockWebServer integration tests verifying JSON deserialization and header injection.
* [x] **Android Token Storage & Authentication Integration (Phase 3B):**
  * `androidx.datastore:datastore-preferences:1.1.1` and `lifecycle-runtime-ktx:2.8.4` integrated.
  * Typed Moshi auth models: `LoginRequest`, `RegisterRequest`, `RefreshTokenRequest`, `AuthUserDto`, `AuthTokensDto`, `AuthResponseData`, `RefreshResponseData`, `MeResponseData`.
  * `DataStoreTokenStorage` provides persistent token storage with in-memory volatile cache for non-blocking synchronous reads needed by OkHttp's `AuthInterceptor`.
  * `DataStoreTokenProvider` implements `TokenProvider` delegating to `TokenStorage`.
  * `AuthRepository` (`AuthRepositoryImpl`) implements login, register, token refresh, logout (with unconditional local storage cleanup), `checkSession()`, and `hasActiveSession()` with Moshi error extraction.
  * Custom `ProPassApplication` class initializes storage, registers provider with `NetworkClient`, and provides repository singleton.
  * `LoginActivity` wired to real backend login with loading state on login button, error state display, and navigation to `HomeDashboardActivity`.
  * `SplashActivity` checks session via `AuthRepository.hasActiveSession()` and routes to `HomeDashboardActivity` if authenticated, or `LoginActivity` if not.
  * 29/29 unit tests passing across 9 test suites (`./gradlew testDebugUnitTest`).
  * Full Android debug build clean: `./gradlew assembleDebug` (`BUILD SUCCESSFUL`).

---

## 17. Development Roadmap

* [x] **Phase 1: Backend Foundation & Database Setup** (Completed)
* [x] **Phase 2A: Authentication System** (Completed)
* [x] **Phase 2B: User & Profile API** (Completed)
* [x] **Phase 2C: Event & QR Validation API** (Completed)
* [x] **Phase 2D: Registration API** (Completed)
* [x] **Phase 2E: Digital Pass API** (Completed)
* [x] **Phase 3A: Android Networking Foundation** (Completed)
* [x] **Phase 3B: Token Storage & Authentication Integration** (Completed)
* [ ] **Phase 3C: User & Dashboard Integration**
* [ ] **Phase 3D: Event & QR Scanning Integration**
* [ ] **Phase 3E: Registration & Digital Pass Integration**
* [ ] **Phase 4: Frontend Screen Integration & Mock Replacement**
* [ ] **Phase 5: End-to-End Testing & Physical Device Verification**

---

## 18. Current Stopping Point

> **Phase 3B Complete:** Token storage & authentication integration established with Jetpack Preferences DataStore, in-memory caching for non-blocking OkHttp interceptor reads, typed Moshi auth DTOs, full `AuthRepository`, `ProPassApplication`, session restoration routing in `SplashActivity`, and real login integration in `LoginActivity`. Android build clean (0 errors, 29/29 unit tests passing across 9 suites). Ready for Phase 3C upon user approval.  
> *Updated on: September 4, 2026*






