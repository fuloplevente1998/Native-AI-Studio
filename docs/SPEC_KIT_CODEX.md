# Spec Kit a TrainPilot Codex-munkamenetében

A külön bevezetési változás a hivatalos `specify-cli==1.2.6` verzióval készült,
Codex-integrációval és a csomagolt `bug` kiegészítővel. A natív app kódját nem módosítja.

## Használat

A repó gyökerében indított új Codex-menet a `.agents/skills/speckit-*/SKILL.md`
fájlokból kapja a parancsokat. A most futó beszélgetés skillkatalógusa nem feltétlenül
frissül automatikusan; új menetben ellenőrizd az elérhető `$speckit-*` skilleket.

Először olvasd el az aktuális AI Studio-átadót, folytatópromptot, #19 követőt és a
`.specify/memory/constitution.md` meglévő projektszabályait. Az implementált funkciót
ne kezdd újra pusztán egy nyitott készülékes checkbox miatt.

Hibajavítás Codex chatben, egyenként:

```text
$speckit-bug-assess "A 2740-es APK készülékes tesztjén megfigyelt konkrét hiba: [tünet, időpont, forrás, elvárt eredmény]. A meglévő natív motort és adatvédelmi kapukat őrizd meg." slug=daily-health-device
$speckit-bug-fix slug=daily-health-device
$speckit-bug-test slug=daily-health-device
```

A riportok `.specify/bugs/daily-health-device/` alatt maradnak. Csak tényleges
megfigyelést adj a felmérésnek; a fenti szöveg minta, nem rögzített új hibajelentés.
A `verified` minősítést mindig a jelentésben megadott ellenőrzési körrel együtt olvasd.
Az SDK vagy óra valódi működését a felhasználó készülékes tesztje igazolja.

Új, behatárolt funkciónál: `$speckit-specify`, szükség esetén `$speckit-clarify`,
majd `$speckit-plan`, `$speckit-tasks`, `$speckit-analyze`, `$speckit-implement`,
`$speckit-converge`. Az APK kiadási kapui és a GitHub Actions változatlanok.

## Első próba: a napi egészségadatok készülékes elfogadása

A #25 implementációját ne írd újra. Az új APK kézbesítése után ellenőrizendő:

- [ ] A mai provider-adatok és frissítési idő a szolgáltatóban látott nappal egyeznek.
- [ ] A befejezett délutáni alvás és az ébredés napjára eső éjszakai alvás látszik, átfedések nélkül.
- [ ] Korábbi mai pulzusmérések időponttal látszanak; a hiányzó nyugalmi pulzus nem mért nulla.
- [ ] Az aktív/teljes energia a lenyitásban helyes, összeadás és kitalált alapanyagcsere nélkül.
- [ ] A dátum melletti frissítés befejeződik; a hét napi bontás kompakt és teljes szélességű.
- [ ] Az órás elvetés és visszaigazolás másik aktív edzés vagy mentett history sérülése nélkül működik.

Ha egy pont nem teljesül, abból legyen az első konkrét bug-assess feladat.

## Reprodukálható eszköz és frissítés

```bash
uvx --from specify-cli==1.2.6 specify integration status
```

A generált integrációs skilleket ne szerkeszd kézzel. Frissítés külön ágon,
ellenőrzött új verzióval, manifest-/diff-ellenőrzéssel történjen. A constitution
projekt által karbantartott dokumentum. A sablonok önmagukban nem futtatnak
edzésmotort, Android-buildet vagy külső üzenetküldést.

Források: [Spec Kit](https://github.com/github/spec-kit),
[Codex-integráció](https://github.com/github/spec-kit/blob/main/docs/integrations/codex.md),
[meglévő projekt](https://github.com/github/spec-kit/blob/main/docs/guides/existing-projects.md).
