# Heart-saver: on-device validation checklist
This engineering protocol is for an experimental camera PPG prototype, not a clinical validation procedure.
## Before each session
- Clean the rear camera lens and flash; let the phone return to room temperature.
- Sit quietly for five minutes, support the hand at heart level, and avoid talking or shivering.
- Use a separately validated pulse oximeter as a comparison reference.
- Record phone model, Android version, finger, room conditions, and reference-device model.
## Calibration and repeatability
1. Gently cover camera and flash. Do not press hard enough to blanch the fingertip.
2. Wait for acquisition and multi-window confirmation; do not record transient values.
3. Record app BPM, reference BPM, confidence, status, and stability.
4. Repeat three times on the same finger, then another finger.
5. Compare dim and bright ambient environments. Flash illumination should dominate; invalid optical levels must be rejected.
6. Try two comfortable fingertip pressures without blanching. Pressure changes should invalidate the window and require stabilization.
7. Do not tune to one phone or person alone; investigate systematic bias before changing thresholds.
## Motion and lifecycle stress tests
- Introduce a brief small movement, then hold still. The signal window must clear and optical checks must wait for recovery frames.
- Test repeated movement, partial coverage, loss/reacquisition, and sudden light changes.
- Start/stop repeatedly; background the app while measuring; verify camera and torch stop and stale BPM disappears.
- Relaunch and verify valid history persists.
## Engineering acceptance gates (not medical thresholds)
- No crash, camera leak, or torch left on after stop/background.
- No BPM during invalid optics or movement recovery.
- Exposure changes should not create a large BPM shift in equivalent synthetic signals.
- Stationary trials must be compared against an independent reference on-device before claiming accuracy.
- History retains at most 50 valid timestamp/BPM/confidence records. Camera frames are not stored or uploaded.
## Trial sheet
Capture trial ID, phone/finger, app BPM, reference BPM, absolute difference, confidence, stability, environment, pressure, and notes. Never use prototype readings for health decisions.
