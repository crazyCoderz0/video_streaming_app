# Setup Guide

## Prerequisites

- Android Studio Ladybug or newer.
- Android SDK 35.
- Java 17.
- Gradle 8.10+ installed, or open the project in Android Studio and let it use the bundled Gradle runtime.
- FFmpeg installed on the desktop and available on `PATH`.
- Android device and desktop on the same local network.

## Android Build

```bash
gradle :Android:app:assembleDebug
```

Install:

```bash
adb install Android/app/build/outputs/apk/debug/app-debug.apk
```

## Desktop Build

```bash
cd DesktopReceiver
gradle run
```

## RTSP Testing

Replace `<device-ip>` with the phone's LAN IPv4 address shown in the app status text.

```bash
ffplay -fflags nobuffer -flags low_delay rtsp://<device-ip>:8554/live
ffmpeg -i rtsp://<device-ip>:8554/live -f null -
vlc rtsp://<device-ip>:8554/live
gst-launch-1.0 rtspsrc location=rtsp://<device-ip>:8554/live latency=0 ! decodebin ! autovideosink
```

## Firewall

Allow inbound TCP on the desktop receiver port, normally `9000`. RTSP clients connect to Android TCP port `8554` plus negotiated UDP RTP ports.
