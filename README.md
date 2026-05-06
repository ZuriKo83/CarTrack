# CarTrack

CarTrack is an Android driving record application designed to track vehicle speed and route information during a trip.

The app uses GPS location data to monitor driving status and is structured to keep tracking active in the background through an Android foreground service.

## Features

- Track vehicle location during driving
- Display current speed
- Record driving route data
- Support background location tracking
- Use a foreground service for continuous tracking
- Detect Bluetooth connection events
- Prepare local data persistence with Room
- Handle Android runtime permissions for location and notifications

## Tech Stack

- Kotlin
- Android
- Jetpack Compose
- Material3
- Google Play Services Location
- Room Database
- Gradle Kotlin DSL

## Project Structure

```text
CarTrack/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

## Permissions

The app uses the following Android permissions for driving tracking, background execution, Bluetooth detection, and notifications.

```xml
ACCESS_FINE_LOCATION
ACCESS_COARSE_LOCATION
ACCESS_BACKGROUND_LOCATION
FOREGROUND_SERVICE
FOREGROUND_SERVICE_LOCATION
BLUETOOTH_CONNECT
POST_NOTIFICATIONS
```

On Android 10 or higher, background location permission must be granted separately for continuous tracking to work properly when the app is not in the foreground.

## How to Run

### 1. Clone the Repository

```bash
git clone https://github.com/ZuriKo83/CarTrack.git
cd CarTrack
```

### 2. Open in Android Studio

Open the `CarTrack` directory in Android Studio and run Gradle Sync.

### 3. Build the App

```bash
./gradlew assembleDebug
```

On Windows, use:

```bat
gradlew.bat assembleDebug
```

### 4. Run the App

Run the app on a real Android device or an emulator from Android Studio.

A real device is recommended because route and speed tracking depend on accurate GPS data.

## How It Works

CarTrack requests the required location permissions from the user. Once tracking starts, the app receives location updates and processes speed and route data.

Background tracking is handled through `TrackingService`, which runs as a foreground service. Bluetooth connection events can be detected through `CarBluetoothReceiver`.

## Development Environment

- Android Studio
- Kotlin
- Gradle
- minSdk: 26
- targetSdk: 36
- compileSdk: 36
