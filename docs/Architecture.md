# Architecture

## System Diagram

```mermaid
flowchart LR
  Android["Android Camera App"] --> TCP["Length-prefixed TCP H.264"]
  Android --> RTSP["RTSP/RTP H.264 endpoint"]
  TCP --> Desktop["JavaFX Desktop Receiver"]
  RTSP --> Clients["VLC / FFplay / FFmpeg / GStreamer"]
```

## Android Class Diagram

```mermaid
classDiagram
  MainActivity --> MainViewModel
  MainViewModel --> StreamRepository
  StreamRepository <|.. StreamRepositoryImpl
  StreamRepositoryImpl --> SocketFrameSender
  StreamRepositoryImpl --> RtspServer
  CameraController --> H264Encoder
  H264Encoder --> EncodedFrame
```

## Desktop Class Diagram

```mermaid
classDiagram
  ReceiverApp --> ReceiverController
  ReceiverController --> SocketVideoServer
  ReceiverController --> FfmpegH264Decoder
  SocketVideoServer --> PacketProtocol
  PacketProtocol --> VideoPacket
  FfmpegH264Decoder --> MjpegReader
```

## Sequence Diagram

```mermaid
sequenceDiagram
  participant User
  participant Android
  participant Desktop
  participant Decoder
  User->>Desktop: Start receiver
  User->>Android: Connect and start stream
  Android->>Desktop: TCP connect
  Android->>Desktop: H.264 packet
  Desktop->>Decoder: Write H.264
  Decoder-->>Desktop: JPEG image frames
  Desktop-->>User: Render live feed, FPS, latency
```

## Streaming Flow

```mermaid
flowchart TD
  Preview["CameraX PreviewView"] --> Analysis["ImageAnalysis YUV_420_888"]
  Analysis --> Encoder["MediaCodec H.264 Encoder"]
  Encoder --> Sender["SocketFrameSender"]
  Sender --> Receiver["Desktop SocketVideoServer"]
  Receiver --> Ffmpeg["FFmpeg Decoder"]
  Ffmpeg --> JavaFX["JavaFX ImageView"]
```

## RTSP Flow

```mermaid
sequenceDiagram
  participant Client
  participant RtspServer
  participant Encoder
  Client->>RtspServer: OPTIONS
  Client->>RtspServer: DESCRIBE
  Client->>RtspServer: SETUP client_port
  Client->>RtspServer: PLAY
  Encoder-->>RtspServer: EncodedFrame
  RtspServer-->>Client: RTP payload type 96
```

## Network Protocol

```text
int32  magic 0x4C565331
uint8  version 1
uint8  flags bit0=keyframe
int64  timestampUs
int32  payloadLength, max 2 MiB
byte[] H.264 access unit
```

Security controls:

- IP and port validation on Android.
- Max frame size enforcement on both sender and receiver.
- Malformed packets close only the offending socket.
- Sockets and codecs are closed during lifecycle transitions.

