# TrainPilot – négy külön fejlesztői chat és egy integrátor

**Most nincs további app-implementáció ezen a körön belül.** A már majdnem kész Coach-preview és piros/listabeli törlés #58-ban külön elkészül/ellenőrzés alatt. A többi munkát az alábbiak szerint add külön chateknek.

Minden feladat-MD önállóan tartalmazza a közös szabályokat. Az 01–04 egyszerre indulhat, külön ág/worktree és kizárólagos fájlok. Az 05 integrátor a négy átadás után köti be a közös fájlokat. Egyik fejlesztő sem írhatja a MainActivity / TrainPilotViewModel / HealthCoachAndSettingsScreens / TodayGrid / Home és globális Gradle/manifest fájlokat.

- [1. chat – Oktatóvideó az alkalmazáson belül](01-video-appon-belul.md) – https://github.com/fuloplevente1998/Native-AI-Studio/issues/59
- [2. chat – Két belső óraadat-blokk és Sync ikon](02-ora-adatblokkok.md) – https://github.com/fuloplevente1998/Native-AI-Studio/issues/60
- [3. chat – Pulzus és regeneráció adatútja, diagnózisa](03-pulzus-regeneracio.md) – https://github.com/fuloplevente1998/Native-AI-Studio/issues/61
- [4. chat – Nagy térkép: tényleges ablakméret és látható vezérlők](04-terkep-nagy-mod.md) – https://github.com/fuloplevente1998/Native-AI-Studio/issues/48
- [5. Integrátor](05-integrator.md)

[Fájltulajdon-tábla](FAJLTULAJDON.md) · [Közös szabályok](00-kozos-szabalyok.md)

Térképes chathez csatold az eredeti beszélgetés két **03:16-os** képét. A ZIP/MD-k nem tartalmaznak képernyőképet, személyes mentést vagy fotót. Egészségügyi okot nem bizonyít az üres kijelzés; a feladat adatút/engedély/SDK/UI vizsgálat és igazolható javítás.

A PR #58 után induló chatek a friss maint olvassák; a jelenleg kiadott APK továbbra is 2743, a #58 és a most kiosztott négy feladat önmagában nem új aláírt APK.
