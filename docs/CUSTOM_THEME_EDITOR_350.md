> **Theme Test V2 visszajelzés (2026-11-24):** cím: Témák. A név kizárólag a Mentés gomb után megnyíló TrainPilot-dialógusban adható meg, meglévő témánál előtöltve. A nagy előnézet az alsó Előnézet gombból nyíló teljes ablakos, mintaadatokat használó Kezdőlap/Aktív edzés nézet. Nem alkalmazza vagy menti a témát, visszalépéskor megtartja a draftot. Az alábbi korábbi inline előnézet/névmező elrendezése elavult.

> **2026-11-24 készülékes visszajelzés:** az alábbi eredeti fedettségcsúszka kivezetve. A saját téma tömör; a tervezett Liquid Glass család külön dokumentumban: [LIQUID_GLASS_THEMES.md](LIQUID_GLASS_THEMES.md).

# #350 – Saját téma szerkesztő (bővített wireframe, egyeztetési változat)

A #350 jóváhagyott alsó paneljét bővíti az öt szerepre és a felületi fedettségre.
Nem végleges látványterv, nem állít készülékes elfogadást. A meglévő 32 téma és a
négy család változatlan; a saját témák külön listában jelennek meg.

```text
Beállítások / Témák
┌─────────────────────────────────┐
│ Matt · Kontrasztos · Glow · Világos │
│ Saját témák                     │
│ + Új téma                       │
│ [●] Saját 1    Alkalmaz · Szerk. │
└─────────────────────────────────┘

          Új / Saját téma (alsó panel)
┌─────────────────────────────────┐
│ ─────────       Új téma       X │
│ Előnézet   [Kezdőlap] [Edzés]   │
│ ┌─────────────────────────────┐ │
│ │ Kezdőlap · Edzés · Egészség  │ │
│ │ Aktív program   ┌───────┐   │ │
│ │ [Indítás]       │ ▂▅▃▇  │   │ │
│ │ [Súly] [Ism.] [RIR]         │ │
│ └─────────────────────────────┘ │
│ Név [ Saját témám            ]  │
│ Matt | Kontrasztos | Glow | Vil.│
│ Szerep                         │
│ [Háttér] [Kártya] [Kiemelés]    │
│ [Másodlagos] [Keret]            │
│ Színsáv / árnyalat              │
│ ░▒▓███████████████████████████  │
│ Telítettség      ─────●──────   │
│ Fényerő          ─────────●──   │
│ HEX [#FFD426]     [Színminta]   │
│ Gyors színek   ● ● ● ● ● ● →   │
│ Felületi fedettség    80%       │
│ 70% ─────────●────────── 90%   │
│ Kontraszt: 4,5:1 ✓              │
│ [Javasolt árnyalat] (ha hibás)  │
├─────────────────────────────────┤
│ [Mégse]              [Mentés]   │
└─────────────────────────────────┘
```

- A felső előnézet és a szerkesztett szín azonos paletta-feloldót használ.
  A panel tartalma görgethető; a Mégse és Mentés elérhető. A gyors színek az
  aktuális szerephez tartozó 32 beépített paletta színeit adják.
- Az alkalmazás külön művelet a mentéstől. A meglévő téma szerkesztése ugyanazt
  a stabil azonosítót tartja, aktív törléskor a beépített 1-es az alap.
- A HEX mező pontos, a csúszkák billentyűzettel és TalkBackkel is kezelhetők.
  A javasolt árnyalat nem írja felül észrevétlenül a felhasználó beállítását.
- A kontrasztot a ténylegesen kompozitált felületeken számoljuk; vegyes világos/sötét
  felületeknél a közös szövegszín elégtelenségét jelezzük, javítást ajánlunk.
- A fedettség százalékban a felületi festék alfája (70–90%); kikapcsolva 100%.
  Órán átlátszóság és blur nélkül. RenderEffect blur nem az első ütem része.
- A meglévő settings/DataStore, backup/restore és Wear témacsomag szerződését
  kell bővíteni, nem új párhuzamos állapottárat. A régi beépített kulcsok
  továbbra is ugyanoda mutatnak. A pontos integráció a következő lépés.
