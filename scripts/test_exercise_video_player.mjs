// Execute the actual inline player script without a network or fake media success.
// These tests verify our controls/events; they do not prove real YouTube playback.
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';
import { test } from 'node:test';
import assert from 'node:assert/strict';
const source = readFileSync(new URL('../app/src/main/java/com/example/ui/components/inappvideo/InAppVideoPolicy.kt', import.meta.url), 'utf8');
const origin = source.match(/EXERCISE_VIDEO_ORIGIN = "([^"]+)"/)[1];
const template = source.match(/return """([\s\S]*?)"""\.trimIndent/)[1];
const html = template.replaceAll('$videoId', 'MeIiIdhvXT4').replaceAll('$EXERCISE_VIDEO_ORIGIN', origin);
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
function player() {
  const elements = {
    play: { disabled: true, textContent: 'Betöltés…', addEventListener(_, action) { this.click = action; } },
    cover: { hidden: false },
  };
  const document = { title: '', getElementById: id => elements[id] };
  const events = [];
  let options, playCalls = 0, pauseCalls = 0;
  const context = { document, console: { log: event => events.push(event) }, YT: {
    Player: function (_, config) {
      options = config;
      this.playVideo = () => playCalls++;
      this.pauseVideo = () => pauseCalls++;
    },
  } };
  runInNewContext(script, context);
  context.onYouTubeIframeAPIReady();
  return { context, elements, events, get options() { return options; }, get playCalls() { return playCalls; }, get pauseCalls() { return pauseCalls; } };
}
test('ready offers a visible play gesture; only a click asks YouTube to start', () => {
  const p = player();
  assert.equal(p.options.videoId, 'MeIiIdhvXT4');
  assert.equal(p.options.playerVars.origin, origin);
  assert.equal(p.options.host, 'https://www.youtube-nocookie.com');
  p.options.events.onReady();
  assert.equal(p.playCalls, 0);
  assert.equal(p.elements.play.disabled, false);
  assert.match(p.elements.play.textContent, /Lejátszás/);
  assert.equal(p.elements.cover.hidden, false);
  p.elements.play.click();
  assert.equal(p.playCalls, 1);
  assert.equal(p.events.at(-1), 'trainpilot-video:starting');
  assert.equal(p.elements.cover.hidden, false);
  p.options.events.onStateChange({ data: 1 });
  assert.equal(p.elements.cover.hidden, true);
  assert.equal(p.events.at(-1), 'trainpilot-video:playing');
  p.context.tpPause();
  assert.equal(p.pauseCalls, 1);
});
for (const code of [100, 101, 150, 153]) {
  test(`error ${code} survives a late ready/playing callback`, () => {
    const p = player();
    p.options.events.onError({ data: code });
    p.options.events.onReady();
    p.options.events.onStateChange({ data: 1 });
    p.elements.play.click();
    assert.equal(p.playCalls, 0);
    assert.equal(p.elements.cover.hidden, false);
    assert.equal(p.elements.play.disabled, true);
    assert.equal(p.events.at(-1), `trainpilot-video:error:${code}`);
  });
}
test('blocked playback is reported instead of being called ready', () => {
  const p = player();
  p.options.events.onReady();
  p.elements.play.click();
  p.options.events.onAutoplayBlocked();
  p.options.events.onReady();
  assert.equal(p.events.at(-1), 'trainpilot-video:blocked');
  assert.equal(p.elements.play.disabled, true);
});

test('pause before the first frame lets the user retry the visible play button', () => {
  const p = player();
  p.options.events.onReady();
  p.elements.play.click();
  p.options.events.onStateChange({ data: 2 });
  assert.equal(p.elements.cover.hidden, false);
  assert.equal(p.elements.play.disabled, false);
  p.elements.play.click();
  assert.equal(p.playCalls, 2);
});
