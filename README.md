# MyHealth Appointment

A native Android appointment-booking application for browsing departments, choosing doctors, booking available time slots, and managing appointments.

## Features

- Patient registration and email/password login
- Department and doctor browsing
- Doctor-specific availability and time-slot booking
- Appointment history with status badges
- Rescheduling and cancellation
- Input validation for registration and login flows

## Tech Stack

- Java
- Android SDK (compile SDK 36, minimum SDK 26)
- Gradle
- OkHttp
- Gson
- AndroidX, Material Components, View Binding

## Run Locally

### Requirements

- Android Studio
- Android SDK and emulator or Android device
- Network access from the emulator or device

To configure your private development environment, use the local project settings and credentials separately from this repository.

### Start the app

1. Open the project in Android Studio.
2. Sync the project with Gradle files.
3. Start an emulator or connect an Android device.
4. Run the `app` configuration.

You can also build a debug APK from the project root:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

## Demo Flow

1. Register a patient.
2. Open the home screen and browse departments.
3. Select a doctor and an available time slot.
4. Enter an appointment reason and confirm the booking.
5. View the appointment, then try rescheduling or cancelling it.

## Project Structure

- `app/src/main/java/` - Android activities, models, and data access
- `app/src/main/res/` - layouts, colors, themes, and drawable resources
