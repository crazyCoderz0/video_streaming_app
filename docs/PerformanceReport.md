# Performance Report

## Targets

| Metric | Target |
| --- | --- |
| Resolution | 1280 x 720 |
| Frame rate | 30 FPS |
| Latency | Under 500 ms on LAN |
| Android memory | Under 150 MB steady state |
| Transport | TCP for desktop receiver, RTSP/RTP bonus endpoint |

## Implemented Optimizations

- CameraX `STRATEGY_KEEP_ONLY_LATEST` prevents analysis backlog and ANRs.
- MediaCodec hardware H.264 encoder uses 2.5 Mbps bitrate and 1 second keyframe interval.
- TCP uses `tcpNoDelay` and buffered output sized for video frames.
- Desktop receiver accepts reconnects by keeping the server alive after client disconnects.
- Decoder runs off the JavaFX application thread; rendering is marshalled with `Platform.runLater`.
- Frame size is capped at 2 MiB to reduce denial-of-service risk.

## Validation Plan

1. Measure FPS from the receiver label for a 5 minute stream.
2. Measure Android memory in Android Studio Profiler.
3. Compare sender timestamp and receiver monotonic time for transport/decoder latency.
4. Disconnect Wi-Fi and reconnect to verify socket cleanup and receiver reuse.
5. Run VLC/FFplay compatibility checks against the RTSP endpoint.

## Known Production Notes

The custom RTSP implementation covers the assessment endpoint and common RTSP handshake. For a commercial product, replace it with a fully RFC-compliant RTSP stack or AndroidX Media3 once Media3 exposes a stable server API.

