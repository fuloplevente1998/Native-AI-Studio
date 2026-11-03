// Executes our observer/pause script; real YouTube playback still needs device acceptance.
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';
import { test } from 'node:test';
import assert from 'node:assert/strict';
const source = readFileSync(new URL('../app/src/main/java/com/example/ui/components/inappvideo/InAppVideoPolicy.kt', import.meta.url), 'utf8');
const template = source.match(/return """([\s\S]*?)"""\.trimIndent/)[1];
const html = template.replaceAll('$videoId', 'MeIiIdhvXT4');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
function player(initialize = true) {
  const events = [], commands = [];
  const document = { title: '', getElementById: () => ({ contentWindow: {
    postMessage: (message, origin) => commands.push({ message: JSON.parse(message), origin })
  } }) };
  let options, playCalls = 0, pauseCalls = 0;
  const context = { document, console: { log: event => events.push(event) }, YT: {
    Player: function (_, config) {
      options = config;
      this.playVideo = () => playCalls++;
      this.pauseVideo = () => pauseCalls++;
    },
  } };
  runInNewContext(script, context);
  if (initialize) context.onYouTubeIframeAPIReady();
  return { context, events, commands, get options() { return options; }, get playCalls() { return playCalls; }, get pauseCalls() { return pauseCalls; } };
}
test('uses legacy native iframe controls; loading/ready never requests media playback', () => {
  assert.match(html, /www\.youtube\.com\/embed\/MeIiIdhvXT4\?playsinline=1/);
  assert.match(html, /rel=0/);
  assert.match(html, /autoplay=0/);
  assert.match(html, /allowfullscreen/);
  assert.doesNotMatch(html, /id="cover"|tpPlayer\.playVideo\(/);
  const p = player();
  p.context.tpReport('frame-ready');
  p.options.events.onReady();
  assert.equal(p.playCalls, 0);
  assert.equal(p.events.at(-1), 'trainpilot-video:ready');
  p.options.events.onStateChange({ data: 3 });
  assert.equal(p.events.at(-1), 'trainpilot-video:starting');
  p.options.events.onStateChange({ data: 1 });
  assert.equal(p.events.at(-1), 'trainpilot-video:playing');
  p.context.tpPause();
  assert.equal(p.pauseCalls, 1);
});
for (const code of [100, 101, 150, 153]) {
  test(`error ${code} survives a late frame/API-ready or playing callback`, () => {
    const p = player();
    p.options.events.onError({ data: code });
    p.context.tpReport('frame-ready');
    p.options.events.onReady();
    p.options.events.onStateChange({ data: 1 });
    assert.equal(p.playCalls, 0);
    assert.equal(p.events.at(-1), `trainpilot-video:error:${code}`);
  });
}
test('actual blocked playback remains an explicit error', () => {
  const p = player();
  p.options.events.onReady();
  p.options.events.onAutoplayBlocked();
  p.options.events.onReady();
  assert.equal(p.events.at(-1), 'trainpilot-video:blocked');
});
test('lifecycle pause works before API initialization without issuing a play command', () => {
  const p = player(false);
  p.context.tpPause();
  assert.deepEqual(p.commands, [{ origin: 'https://www.youtube.com', message: { event: 'command', func: 'pauseVideo', args: [] } }]);
  p.context.tpReport('observer-unavailable');
  assert.equal(p.context.tpFailed, false);
});
