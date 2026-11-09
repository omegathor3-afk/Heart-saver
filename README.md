# Heart-saver

Heart-saver is an experimental Android prototype for fingertip photoplethysmography (PPG) using the rear camera and torch.

## Measurement approach

The phone illuminates a fingertip with its camera flash. Camera frames are processed on-device to extract changes in reflected red-channel light associated with pulse-related blood-volume changes. A conservative signal estimator attempts to derive pulse rate (BPM) only when the signal is sufficiently periodic.

## Implemented so far

- [x] Native Android/Kotlin + Jetpack Compose project
- [x] CameraX rear-camera preview and torch activation
- [x] Live frame analysis of a central image region
- [x] Basic fingertip-presence heuristic and signal collection
- [x] Autocorrelation-based experimental BPM estimate
- [x] GitHub Actions debug APK build and unit-test workflow
- [x] Synthetic signal tests and invalid-input rejection
- [ ] Resampling and motion/pressure/lighting artifact rejection
- [ ] Stable multi-window BPM confirmation and pulse waveform display
- [ ] On-device validation against a reference pulse oximeter and ideally ECG
- [ ] Measurement history and export

## Build

Run `gradle assembleDebug` with JDK 17 and Gradle 8.9, or use the Android CI workflow.

## Important limitations

**This is not a medical device, pulse oximeter, or ECG.** It estimates pulse rate only; it does not measure SpO2, electrical cardiac activity, or diagnose arrhythmias or other conditions. The current image-based fingertip detector and signal pipeline are experimental and have not yet been validated on a physical phone. Results may be absent or inaccurate depending on camera hardware, flash, fingertip placement, movement, skin perfusion, pressure, and lighting.

## Privacy

Camera frames are processed in memory while a measurement is active. This implementation does not save or upload camera frames or health measurements.
