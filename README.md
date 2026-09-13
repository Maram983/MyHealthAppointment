# MyHealth Appointment

A native Android appointment-booking demo for patients to browse departments, choose doctors, book available time slots, and manage appointments.

## Features

- Patient registration and email/password login
- Department and doctor browsing
- Doctor-specific availability and time-slot booking
- Appointment history with status badges
- Rescheduling and cancellation
- Supabase-backed data storage
- Input validation for registration and login flows

## Tech Stack

- Java
- Android SDK (compile SDK 36, minimum SDK 26)
- Gradle
- Supabase REST API
- OkHttp
- Gson
- AndroidX, Material Components, View Binding

## Setup

### Requirements

- Android Studio
- Android SDK and emulator or Android device
- A Supabase project
- Network access from the emulator or device

### Configure Supabase

1. Create a Supabase project.
2. Open the Supabase SQL Editor.
3. Run [`supabase/schema.sql`](supabase/schema.sql).
4. Copy [`local.properties.example`](local.properties.example) to `local.properties`.
5. Set the Android SDK path, Supabase project URL, and anon public key in `local.properties`:

```properties
sdk.dir=C:\\Users\\YOUR_WINDOWS_USERNAME\\AppData\\Local\\Android\\Sdk
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=YOUR_ANON_PUBLIC_KEY
```

The real `local.properties` file is ignored by Git and should never be committed.

### Run the app

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
6. Verify the resulting rows in the Supabase Table Editor.

## Project Structure

- `app/src/main/java/` - Android activities, models, and Supabase data access
- `app/src/main/res/` - layouts, colors, themes, and drawable resources
- `supabase/schema.sql` - database schema and demo seed data
- `SETUP.txt` - detailed setup notes
- `DEMO_NOTES.txt` - demo script and test checklist

## Security Notice

This repository is an educational/demo project, not a production healthcare system. Do not use real patient information or production credentials.

The included SQL schema intentionally uses simplified demo settings, including disabled row-level security and direct password storage. A production implementation must use a proper authentication provider, password hashing, least-privilege database policies, input validation, audit logging, and appropriate healthcare privacy and security controls.
