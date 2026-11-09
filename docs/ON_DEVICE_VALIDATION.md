# Heart-saver: on-device validation checklist

This is an engineering test protocol for the experimental camera PPG prototype. It is not a clinical validation procedure.

## Before testing
- Use a phone with a rear camera and LED flash; grant camera permission.
- Clean the camera lens and flash. Let the phone return to room temperature.
- Sit down, rest for five minutes, and keep the hand supported at heart level.
- Do not test while walking, exercising, shivering, or immediately after exertion.
- Have a separately validated pulse oximeter available as a comparison reference.

## Repeatable measurement protocol
1. Record phone model, Android version, room conditions, and reference-device model.
2. Start Heart-saver and gently cover both rear camera and flash with the fingertip. Do not press hard enough to blanch the finger.
3. Keep the finger still for at least 20 seconds; wait for a stable estimate.
4. Record Heart-saver BPM, reference BPM, signal confidence/status, and whether the estimate remained stable.
5. Repeat three times using the same finger, then repeat with another finger.
6. Repeat a trial with deliberate small movement and verify the app withdraws/invalidates the estimate rather than presenting a confident false value.
7. Repeat in dim and bright ambient conditions. The flash should dominate illumination; invalid optical conditions should be rejected.
8. Stop the measurement and confirm the result is saved to local history. Relaunch the app and verify history persists.

## Suggested engineering acceptance gates (not medical thresholds)
- No crash, camera leak, or torch left on after stopping or backgrounding the app.
- No BPM shown when finger/optical quality checks fail.
- Stable stationary trials produce repeatable readings close to the reference; investigate systematic bias before tuning thresholds.
- Movement trials cause confidence to fall or the reading to be withdrawn.
- History stores only timestamp, BPM, and confidence; no image frames are stored or uploaded.

## Data sheet
For each trial capture: trial ID, phone/finger, Heart-saver BPM, reference BPM, absolute difference, confidence, stability, environment, and notes. Do not use these prototype results to make health decisions.
