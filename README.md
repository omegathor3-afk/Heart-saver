# Heart-saver

Heart-saver is an Android app prototype for **fingertip photoplethysmography (PPG)** using the rear camera and torch.

## Measurement approach

The phone illuminates a fingertip with its camera flash. The camera observes tiny changes in reflected light as blood volume changes with each pulse. A signal-processing pipeline estimates pulse rate (BPM) only when signal quality is adequate.

**Important:** camera PPG estimates pulse rate; it is not an ECG, does not measure the heart's electrical activity, and must not be used to diagnose arrhythmias or other conditions. No accuracy guarantee is made. The first version is an experimental wellness prototype, not a medical device.

## Current status

- [x] Android/Compose project scaffold
- [x] CameraX dependencies and camera permission
- [x] GitHub Actions debug-build workflow
- [ ] Rear-camera + torch capture
- [ ] Finger-contact and signal-quality checks
- [ ] PPG filtering and pulse detection
- [ ] On-device validation against a reference device
- [ ] Measurement history and export

## Build

Run `gradle assembleDebug` with JDK 17 and Gradle 8.9, or use the Android CI workflow.

## Privacy

The intended design processes camera frames on-device. It should not upload camera frames or health measurements. Camera access is needed only during an active measurement.
