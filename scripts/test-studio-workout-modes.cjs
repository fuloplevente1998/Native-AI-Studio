'use strict';
// Fast, dependency-free wiring regression for the Studio #197 mode editor.
// Run: node scripts/test-studio-workout-modes.cjs
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const file = p => fs.readFileSync(path.join(root, p), 'utf8');
const main = file('app/src/main/java/com/example/MainActivity.kt');
const settings = file('app/src/main/java/com/example/ui/screens/HealthCoachAndSettingsScreens.kt');
const dialog = file('app/src/main/java/com/example/ui/screens/StudioWorkoutModesDialog.kt');
const model = file('app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt');
const resolver = file('app/src/main/java/com/repforge/app/data/store/NativeTrainingModes.kt');

const has = (code, text, context) => assert.ok(code.includes(text), context + ': missing ' + text);
assert.equal((main.match(/StudioWorkoutModesDialog\(/g) || []).length, 1);
has(main, 'onToggleProgressionModal = { viewModel.toggleProgressionModal(it) }', 'Settings entry');
has(main, 'onOpenProgressionModal = { viewModel.toggleProgressionModal(true) }', 'Workout entry');
has(main, 'if (uiState.currentSubScreen == SubScreen.SETTINGS_MAIN) "global" else "program"', 'Scope');
assert.ok(!settings.includes('if (uiState.showProgressionModal)'), 'Settings must not create a second dialog');
assert.ok(!main.includes('updateProgressionSettings('), 'Legacy string-mapping writer must be disconnected');
for (const id of ['normal', 'hit']) has(dialog, '"studio_modes_style_' + id + '"', 'Style');
for (const id of ['double', 'linear', 'rir']) has(dialog, '"studio_modes_progression_$code"', 'Progression option');
assert.ok(!dialog.includes('5×5') && !dialog.includes('Kezdő kímélő profil'), 'Unsupported options hidden');
for (const key of ['NativeTrainingModeChange.approve', 'mode.prepareInherit(', 'mode.prepare(', 'NativeTrainingModeChange.undo']) {
    has(model, key, 'Canonical persistence');
}
has(model, 'progressionModeReview = StudioTrainingModeReview(', 'Prepared comparison');
has(model, 'NativeTrainingModeComparison.read(', 'Actual program comparison');
has(model, 'NativeTrainingModeComparison.isCurrent(', 'Program snapshot revalidation');
has(dialog, 'studio_modes_comparison_$index', 'Per-exercise before/after display');
has(model, 'studioModeGeneration != token', 'Stale preview guard');
has(model, 'studioTimerWriteMutex.withLock', 'Serialized timers');
has(model, 'workSeconds = saved.workSeconds', 'Preserve work timer');
has(model, 'rounds = saved.rounds', 'Preserve interval rounds');
has(settings, 'checked = uiState.vibrationEnabled', 'Persisted haptic state');
has(settings, 'onCheckedChange = onToggleVibration', 'Persisted haptic callback');
has(resolver, 'fun resolveSavedJson(json: String?, programId: String?)', 'Program override resolver');
console.log('PASS: one shared Studio mode dialog, canonical IDs and snapshot approval, scoped modes, durable timer controls');
