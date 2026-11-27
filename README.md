# SitePulse

SitePulse is an Android client for construction project and site-task workflows. This repository contains the mobile app; the service backend is maintained separately.

## Overview

The app connects project teams with project, task, field-evidence, and reporting workflows. The current implementation includes:

- Account registration, login, password reset, and optional biometric session access.
- Project discovery, joining, details, documents, issue reports, and status views.
- Task assignments, subtask progress, time logs, photos, image annotations, and report submission.
- Team conversations, message attachments, dashboard notifications, and profile settings.
- Location and weather information on the dashboard.
- On-device image detection and AI-assisted daily report generation.

These are capabilities present in the codebase; they are not claims of pilot results or production service availability.

## Technology and architecture

The project is a single Android application module built with Kotlin and Jetpack Compose. Retrofit, OkHttp, and Gson connect screens and data components to a separate REST backend. Room stores local user and task data. WorkManager runs background work, and Firebase Cloud Messaging provides push notifications. AndroidX Biometric supports biometric login.

The AI path uses LiteRT with a bundled YOLO model and LiteRT-LM with a Gemma model downloaded by the app. The app combines visual observations with task evidence and engineer-provided information. The model output should be reviewed by an engineer.

## Requirements

- Android Studio with the project Gradle wrapper and JDK 21.
- Android SDK Platform 36 for building.
- An Android device or emulator running API 24 or later.
- A reachable SitePulse backend for server-backed account, project, task, messaging, and reporting features.
- The app's Firebase client configuration at app/google-services.json for the Google Services Gradle plugin and messaging.
- Internet access and available device storage for the Gemma model download when using AI report generation.

The repository's native libraries are present under the x86_64 ABI directory. Validate AI support on each intended pilot device, especially ARM-based phones, before field use.

## Build and install

Open the repository root in Android Studio and allow Gradle sync to complete. Build or install a debug build from Android Studio, or use the Gradle wrapper:

```text
Windows:      .\gradlew.bat :app:assembleDebug
Windows:      .\gradlew.bat :app:installDebug
macOS/Linux:  ./gradlew :app:assembleDebug
macOS/Linux:  ./gradlew :app:installDebug
```

Run the app on the selected device from Android Studio, or install the generated debug build. Grant requested camera, location, and notification permissions as needed by the workflows.

## Configuration

The app does not currently read a .env file or custom environment variables. There are no supported application environment variables to set, so an .env.example is not applicable.

| Setting | Purpose |
| --- | --- |
| Application environment variables | None are currently read by the app or Gradle. |
| local.properties | Machine-local Android SDK path used by Gradle. Android Studio can generate this file; do not commit it. |
| app/google-services.json | Firebase client configuration used by the Google Services plugin and Firebase Messaging. Use the matching app configuration. Do not put service-account credentials in the Android app. |
| Backend address | The API client currently uses a value defined in the app rather than an environment variable. The checked-in development configuration points to a private-network HTTP service and will not be reachable from a typical pilot device. A reachable HTTPS backend must be configured for pilot use. |

## Run and use

Start the app and sign in or register against the configured backend. From the dashboard, users can open projects and assigned tasks, review project documents, update task and subtask progress, record time, attach field photos, annotate evidence, and submit reports. The app also includes conversations, profile settings, notifications, location, and weather views.

Some actions remain unfinished: the task-list camera and issue-report callbacks contain TODOs, dashboard notification items do not yet open a detail action, and background task synchronization is incomplete. Verify the intended workflow before relying on these actions in a pilot.

## AI models

- YOLO: yolo11n.tflite and its labels are bundled in the app assets for image detection.
- Gemma: gemma3-1b-it-int4.litertlm is downloaded into app-private storage for LiteRT-LM report generation. The model file is not included in Git. The in-app download requires network access and substantial device storage.

Model provenance, redistribution terms, and device performance should be confirmed for the selected pilot configuration. AI-generated reports are assistance for review, not an independent assessment.

## Development status and limitations

This is an active development build, not a production-ready release. The API address is a fixed development configuration rather than a supported environment setting. The background sync worker and several UI actions are incomplete. The repository does not establish ARM native-library coverage, a production backend setup, a CI workflow, or model/library licensing. The application ID remains com.example.capstonesample, and release optimization is disabled in the Gradle configuration.

Unit tests are in app/src/test; instrumentation tests are in app/src/androidTest. The repository includes test instructions and test sources, but no test result is implied by this README.

## Proposed pilot objectives

These are candidate objectives for discussion, not agreed commitments or reported outcomes:

1. Evaluate project and task workflows with representative site teams and devices.
2. Assess the usefulness and review effort of AI-assisted reports against engineer-reviewed records.
3. Measure device compatibility, model-download completion, storage use, and report-generation time.
4. Gather feedback on field evidence capture, offline behavior, notifications, and overall usability.
5. Confirm backend connectivity, data handling, and support procedures for the proposed pilot scope.
