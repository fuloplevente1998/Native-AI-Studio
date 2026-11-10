# TrainPilot Native — AI Studio UI/UX handoff (#219)

Date: 2026-11-10
Repository: https://github.com/fuloplevente1998/Native-AI-Studio
Issue: https://github.com/fuloplevente1998/Native-AI-Studio/issues/219
Target branch: **main only**

## AI Studio workflow
AI Studio can edit and push only to `main`. Do **not** create PRs or branches, and do not stop because PR creation is unavailable. Read the current `main`, `AGENTS.md`, this entire document and issue #219. Implement the changes in native Android Kotlin/Jetpack Compose, commit and push to `main`. Report actual commit SHA(s), files, tests, screenshots if possible, and any unresolved issues. Do **not** trigger signed APK builds; we will independently review and build the signed Android + Wear OS pair.

## Objective
Polish the established dark/gold TrainPilot UI from five real-device screenshots. Maintain the existing visual identity, all functionality, accessibility, stored data, 140 exercises, 13 built-in programs, Health and goals fixes #217/#218, phone↔Wear workout sync and rest timer.

## 1 — Programok: compact activation button and cards
- In the program list, shrink the visible `Aktiválás` button footprint (especially width), while preserving an accessible >=48dp tap target. If necessary, use a shorter text or compact icon+label with accessible description.
- Reallocate horizontal space to program title, training days, level and equipment; avoid excessive wrapping and truncated information.
- Reduce vertical padding/height of program cards without creating cramped text. Keep reorder controls, active-state indicator and selection actions intact.
- Validate long Hungarian names such as `Otthoni A/B – 2. szint` and long equipment strings. Do not hide key information just to reduce card height.

## 2 — Gyakorlatválasztó: remove small stick-figure icons
- Remove the small generic human/weightlifter illustration in every exercise picker row; do not leave empty placeholder width.
- Use recovered space to show longer exercise names and equipment labels. Preserve muscle badges and favorite, video, add and start actions.
- All 140 built-in exercises, custom exercises, search, filtering and scrolling must continue to work.
- Maintain accessible tap targets and readable typography; do not introduce horizontal overflow.

## 3 — Exercise details: remove unattractive anatomical-execution drawings
- Remove the entire `Anatómiai kivitelezés` generic stick-figure graphic section from the exercise details (e.g. Arnold-nyomás). Do not replace it with similarly crude icons or placeholders.
- Only introduce a replacement if there is an existing reusable, credible anatomical body illustration **consistent with the Fejlődés screen muscle-body artwork**, correctly mapped to the exercise and usable without misleading anatomy. Otherwise omit the illustration panel and let the information below move up.
- Keep video/open-video actions, favorites, exercise name, equipment, primary/secondary muscles, technique, muscle-load visualization, coaching instructions and all other details. Avoid duplicate video buttons if the layout can be simplified without removing functionality.
- No fake generated visual, incorrect muscle activation, or broken image dependencies.

## 4 — Kezdőlap and Fejlődés: viewport-first, safe adaptive layout
- On the user's phone, make the default/collapsed Home and Progress content as compact as reasonably possible so essential information fits on one screen **without scrolling**. Preserve hierarchy and readability; adapt cards, paddings, typography, chart sizing and spacing to available viewport height.
- **Critical safety/accessibility rule:** never disable vertical scrolling globally when content exceeds viewport. Expanded cards/accordions, small screens, large font settings, system insets and landscape must allow scrolling to all content. Use measured constraints or responsive breakpoints; no fixed clipping or hidden sections.
- Do not move core actions offscreen or compress text below usable size. When all content cannot fit legibly, prefer scrollability over a forced single screen.
- Ensure bottom of the Progress muscle-load card and Home goals card are reachable; maintain all progress calculations and click actions.

## Implementation checklist
1. Inspect actual Compose navigation/screen hierarchy and locate picker rows, detail dialog, program list cards, Home and Progress components.
2. Implement shared responsive spacing/components where practical; do not broadly rewrite working screens.
3. Verify dark theme, gold accents, 48dp minimum interaction areas, TalkBack semantics, font scaling and Hungarian text. Preserve EN/DE/RO labels where affected.
4. Test with the real-device screenshot reference dimensions (~709px wide image), a smaller viewport, large fonts, collapsed and expanded cards, portrait and landscape if supported. Do not assume screenshot pixel width equals dp.
5. Add focused UI/layout regression tests where feasible; run full app JVM tests, wear JVM tests, shared tests where configured, and `:app:assembleDebug :wear:assembleDebug`. Provide exact commands and counts.
6. Preserve stable exercise IDs, all program configurations, saved workouts, user preferences, permissions, data sync, and signing configuration.

## Acceptance criteria
- Program activation buttons visibly narrower; cards shorter; program information more legible.
- Picker rows have **no generic exercise stick figures**, with reclaimed text space.
- Exercise detail has **no crude anatomical-execution illustration panel**; all essential content retained.
- Default Home and Progress are compact; no clipped, inaccessible or hidden content on any supported screen size. Scrolling remains available when needed.
- Tests and debug builds pass, no regressions in existing features.
- All changes pushed directly to `main`, with SHA and concise implementation report. **No PR and no signed build from AI Studio.**

## Final response expected from AI Studio
Provide: SHA(s), list of changed files, concise before/after per item, test commands/results, any limitations, and a checklist of manual device tests. State clearly what was actually tested on-device vs simulated/compiled.

## Screenshot reference set — five user-supplied real-device images
The original screenshots are attached to the user conversation, but **are not yet uploaded into this GitHub repository**. Do not claim to have viewed them through GitHub until the actual JPG files have been uploaded. Intended destination: `docs/design-reference/issue-219/` with the following filenames (use the original screenshots, not generated substitutes):

| Filename | Screen | Visual observations |
| --- | --- | --- |
| `451.jpg` | Exercise picker, 140 exercises | Each row begins with a bulky stylized person icon; exercise name and equipment are truncated, while favorite, video, add, start controls consume width. Remove person icons and reclaim width. |
| `449.jpg` | Arnold-nyomás exercise details | Large “Anatómiai kivitelezés” panel shows simplistic orange/grey stick-figure drawings; remove this entire panel, keep accurate textual anatomy, video and muscle-load details. |
| `447.jpg` | Programs list | Large yellow “Aktiválás” buttons occupy too much width; long equipment labels wrap across several lines, making cards tall. |
| `445.jpg` | Home default state | Profile, active workout, suggestion, quick workout/watch, six metrics, and goals cards; lower goals section is cut by viewport. Make collapsed default state more compact while keeping scroll safety. |
| `443.jpg` | Progress default state | Top volume chart, metric cards, muscle-load illustration and suggestion; lower content extends beyond viewport. Compact default layout, retain scrolling for expansion and small screens. |

After the original JPGs are uploaded, AI Studio should inspect the image URLs `https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/main/docs/design-reference/issue-219/<filename>` for each of the five filenames above. **Until then, these are planned URLs, not verified live assets.**
