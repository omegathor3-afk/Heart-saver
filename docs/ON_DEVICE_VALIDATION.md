# Heart-saver: on-device validation checklist

This is an engineering test protocol for an experimental camera-PPG prototype, not clinical validation. No physical-device test is claimed until a tester records the trial results below.

## Before each session
- Record phone model, Android version, app build/commit, finger, room conditions, and reference-device model.
- Clean the rear camera lens and flash; let the phone return to room temperature.
- Sit quietly for five minutes, support the hand at heart level, and avoid talking or shivering.
- Use a separately validated pulse oximeter as a comparison reference.

## Calibration and repeatability
1. Gently cover camera and flash. Do not press hard enough to blanch the fingertip.
2. Wait for acquisition and multi-window confirmation; do not record transient values.
3. Record app BPM, reference BPM, correlation confidence, status, and stability.
4. Repeat three times on the same finger, then repeat on another finger.
5. Compare dim and bright ambient environments. Flash illumination should dominate; invalid optical levels must be rejected.
6. Try two comfortable fingertip pressures without blanching. Pressure changes should invalidate the window and require stabilization.
7. Repeat one stationary trial after removing and replacing the fingertip.
8. Do not tune to one phone or person alone; investigate systematic bias before changing thresholds.

## Motion and lifecycle stress tests
- Introduce a brief small movement, then hold still. The signal window must clear and optical checks must wait for recovery frames.
- Test repeated movement, partial coverage, loss/reacquisition, and sudden light changes.
- Start/stop repeatedly; background the app while measuring; verify camera and torch stop and stale BPM disappears.
- Relaunch and verify valid history persists.
- Export CSV and confirm timestamps, BPM, and correlation-confidence columns are present and chronological.

## Trial sheet
For every attempt capture: trial ID, app version/commit, phone model, Android version, finger, app BPM, reference BPM, absolute difference, correlation confidence, stability, environment, pressure, and notes.

Suggested repeatability summary:
- Mean absolute error against the independent reference.
- Median absolute error and maximum absolute error.
- Within-condition spread across repeated measurements.
- Count of rejected/failed acquisitions and the reason shown by the app.

Do not define medical pass/fail thresholds from this prototype. These figures are engineering observations only and must not be used for health decisions.

## Engineering acceptance gates
- No crash, camera leak, or torch left on after stop/background.
- No BPM during invalid optics or movement recovery.
- Exposure changes should not create a large BPM shift in equivalent synthetic signals.
- Stationary trials must be compared against an independent reference on-device before claiming accuracy.
- History retains at most 50 valid timestamp/BPM/confidence records; CSV export contains only those fields.
- Camera frames are processed in memory and are not stored or uploaded.
