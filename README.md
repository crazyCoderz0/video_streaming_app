# Real-Time Android Video Streaming System

Production-oriented assessment solution for streaming a rear Android camera feed to a desktop receiver over a local network.

## Features

- Android app in Kotlin with Clean Architecture, MVVM, Hilt, StateFlow, Coroutines, CameraX, MediaCodec H.264, Material 3 XML UI, and Timber logging.
- Java 17 desktop receiver with JavaFX UI, reconnect-safe TCP socket server, FFmpeg H.264 decoding, OpenCV bootstrap, FPS, latency, status, and logs.
- Custom length-prefixed TCP protocol for low-latency LAN transport.
- Bonus Android RTSP endpoint at `rtsp://<device-ip>:8554/live`.
- Unit tests for validation and packet protocol boundaries.

## Repository Structure

```text
Android/
  app/src/main/java/com/example/streamer/
    camera/      CameraX binding
    data/        Repository implementation
    di/          Hilt modules
    domain/      State and contracts
    encoder/     MediaCodec H.264 encoder
    presentation/MVVM activity and view model
    streaming/   TCP sender and RTSP server
    utils/       validation and network helpers
DesktopReceiver/
  src/main/java/com/example/receiver/
    controller/  MVC coordinator
    decoder/     FFmpeg H.264 decoder
    network/     TCP receiver and protocol
    ui/          JavaFX app
    utils/       FPS, logging, OpenCV bootstrap
docs/
```

## Prerequisites

Install these before running the project:

- Android Studio Ladybug or newer.
- Android SDK 35.
- Java 17.
- Gradle 8.10+ available on `PATH`, or use Android Studio's bundled Gradle runner.
- FFmpeg available on the desktop `PATH`.
- A real Android device with a rear camera.
- Android device and desktop connected to the same local Wi-Fi network.

Check FFmpeg:

```bash
ffmpeg -version
```

Find your desktop IP address:

macOS/Linux:

```bash
ifconfig
```

Windows:

```powershell
ipconfig
```

Use the desktop LAN IPv4 address, usually like `192.168.x.x` or `10.x.x.x`.

## Build

Android:

```bash
gradle :Android:app:assembleDebug
gradle :Android:app:testDebugUnitTest
```

Desktop:

```bash
cd DesktopReceiver
gradle run
gradle test
```

Install `ffmpeg` on the desktop host before running the receiver.

## How To Use The App

### 1. Start The Desktop Receiver

From the project root:

```bash
./gradlew -p DesktopReceiver run
```

In the desktop app:

1. The receiver auto-starts on port `9000`.
2. Confirm the status changes to `Listening on 9000`.
3. If your operating system asks for firewall permission, allow incoming local network connections.
4. You can still click `Stop Receiver` and `Start Receiver` manually if you want to restart the listener.

### 2. Install The Android App

Build the debug APK:

```bash
gradle :Android:app:assembleDebug
```

Install it on a connected Android device:

```bash
adb install Android/app/build/outputs/apk/debug/app-debug.apk
```

You can also open the project in Android Studio, select the `Android:app` run configuration, choose a physical device, and click Run.

### 3. Start Streaming From Android

On the Android device:

1. Open `LAN Camera Streamer`.
2. Grant camera permission when prompted.
3. Enter the desktop computer IP address in `Receiver IP address`.
4. Enter the desktop receiver port, for example `9000`.
5. Tap `Connect`.
6. Wait for the status to show `Connected`.
7. Tap `Start Stream`.
8. The desktop receiver should display the live camera feed, FPS, latency, status, and logs.

To stop streaming:

1. Tap `Stop Stream` on Android.
2. Tap `Disconnect` on Android.
3. Click `Stop Receiver` on desktop when finished.

## RTSP Usage

RTSP clients can also connect directly:

```bash
ffplay -fflags nobuffer -flags low_delay rtsp://<device-ip>:8554/live
ffmpeg -i rtsp://<device-ip>:8554/live -f null -
vlc rtsp://<device-ip>:8554/live
gst-launch-1.0 rtspsrc location=rtsp://<device-ip>:8554/live latency=0 ! decodebin ! autovideosink
```

Replace `<device-ip>` with the Android phone's LAN IP address. The Android app shows the RTSP endpoint in the status line after startup.

Important: start the Android stream before opening VLC/FFplay, because the RTSP server forwards the active H.264 encoder output.

## Common Workflow

Use this order for the least friction:

1. Connect desktop and phone to the same Wi-Fi.
2. Start `DesktopReceiver`.
3. Launch the Android app.
4. Enter desktop IP and port.
5. Tap `Connect`.
6. Tap `Start Stream`.
7. Watch the live feed on desktop.

## Troubleshooting

- Desktop shows no frames: verify phone and desktop are on the same subnet and that the firewall allows the selected TCP port.
- Android connection fails: enter the desktop IP, not the phone IP.
- Decoder fails: install FFmpeg and ensure `ffmpeg` is on `PATH`.
- RTSP player connects but shows no video: start streaming in the Android app so encoded frames are available.
- Android asks for camera permission again: grant it from Android Settings > Apps > LAN Camera Streamer > Permissions.
- Desktop receiver port is already in use: choose another port in the desktop UI and enter the same port on Android.
- Phone cannot reach desktop: disable VPNs, confirm both devices are on the same network, and allow local firewall access.

Screenshots placeholders:

- `docs/screenshots/android-main.png`
- `docs/screenshots/desktop-receiver.png`
# video_streaming_app
