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
* `app/src/test/java/com/mpc/propass/ProPassQrValidationTest.kt` — Unit tests for ProPass QR regex validation and URL parsing.
* `app/src/test/java/com/mpc/propass/ExampleUnitTest.kt` — Basic host unit test.

---

## 13. Git & Repository Status

* **Repository:** `ProPass`
* **Remote:** `git@github.com:Divy2095/ProPass.git`
* **Current Branch:** `main`
* **Important `.gitignore` Rules:**
  * Standard Android Gradle build artifacts (`/build`, `.gradle`, `*.iml`, `local.properties`).
  * `/.agents/` is explicitly gitignored because it previously contained GCP API secrets.

> [!CAUTION]
> **CRITICAL SECURITY RULE:** Never commit API keys, GCP credentials, service account JSON files, or secrets into this repository or this file.

---

## 14. Known Limitations (Frontend Phase)

1. **No Network Layer:** Form submission, QR parsing, and event info are processed purely on-device with local mock models.
2. **Static Identity:** The auto-filled user profile is statically initialized (`Alex Morgan` / `Sarah Jenkins`) until the user profile API is connected.
3. **Mock Actions:** External wallet export, social sharing, and contact buttons generate UI Toast feedback rather than deep-linking to external service APIs.
4. **Offline Only:** Event IDs are matched against a hardcoded local map with a Title-Case slug fallback.

---

## 15. Physical Device Verification Summary

All listed features have been executed and verified on physical hardware (`KVAMAAXSPNOV7PXC`):

* [x] **Gradle Build & Assemble:** `./gradlew assembleDebug` builds in under 3s with 0 errors.
* [x] **Live Camera Viewfinder:** Rear camera stream renders smoothly through `PreviewView` with proper aspect ratio underneath the reticle frame.
* [x] **Physical Torch:** Real device LED toggles ON/OFF with matching amber UI feedback.
* [x] **Real-Time QR Detection:** Google ML Kit detects real QR codes from live camera feed.
* [x] **QR Format Validation:** Random URLs and invalid QR codes are rejected with a debounced Toast; valid `propass.id/event/<id>` codes immediately trigger haptic feedback and navigation.
* [x] **Smart Form Validation:** Prevents submission on empty required fields or invalid duration (> 5); highlights errors with red borders.
* [x] **Review & Edit Preservation:** Review screen displays exact form values; "Back / Edit" returns to the form with all entered values preserved.
* [x] **Success & Navigation:** Success badge animates; "View My Pass" and "Go Home" route to destinations with clean backstack handling.

---

## 16. Next Development Phase: Backend Implementation

The next major milestone is **Backend Implementation & Integration**. Recommended roadmap:

1. **Backend Initialization:** Initialize Node.js + TypeScript project repository.
2. **Database & Schema Setup:** Configure PostgreSQL with Prisma (User, Event, Registration, Pass models).
3. **Authentication Endpoints:** Implement `/api/auth/login`, `/api/auth/register`, `/api/auth/google`.
4. **Event & QR Endpoints:**
   * `GET /api/events/:eventId` — Returns event metadata.
   * `POST /api/events/:eventId/validate-qr` — Validates event QR tokens.
5. **Registration API:** `POST /api/registrations` — Submits and persists user event registrations.
6. **Digital Pass API:** `GET /api/pass/me` — Fetches current user's digital pass and credentials.
7. **Android Retrofit Layer:**
   * Add Retrofit 2, OkHttp 3, and Kotlinx Serialization to `build.gradle.kts`.
   * Create ApiService, Repository layer, and ViewModel architecture.
   * Replace local mock data with real asynchronous network calls.
8. **End-to-End Testing:** Verify live QR scanning ➔ backend validation ➔ live database persistence ➔ pass update on physical device.

---

## 17. Instructions for Future Antigravity Sessions

When continuing work on this project in a new Antigravity session:

1. **Read this `HANDOFF.md` first** before taking any action.
2. **Inspect the actual codebase** to confirm current file locations and dependencies.
3. **Preserve existing UI:** Do not redesign or break the established XML layouts, custom views, or color palette unless explicitly instructed.
4. **No Fake Backend:** When implementing the backend, build real endpoints, databases, and network clients—do not add temporary mock layers.
5. **Security First:** Never commit secrets, API keys, or GCP credentials to Git.
6. **Verify with Tests & Builds:** Run `./gradlew test` and `./gradlew assembleDebug` after making code changes.
7. **Keep Documentation Updated:** Update `HANDOFF.md` whenever new architecture, backend APIs, or major features are introduced.

---

## 18. Current Stopping Point

> **Frontend phase complete. Android application is functional and physically verified. Backend implementation is the next major phase.**  
> *Generated on: August 27, 2026*
