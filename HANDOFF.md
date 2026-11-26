# TrainPilot Native – 2811 átadó

- Alap: main `32ea75e9`, #401, 1.5.4 (281000/281002). A Play-kiadási jegyzet alapja az 1.5.0 (280600/280602).
- `fix/workout-health-refresh`: közös tranzakciós védelem kézi és automatikus visszaolvasáshoz. Üres vagy gyengébb válasz esetén megmarad a mentett mérés.
- A Napló Forrás lapja a szünetekkel tagolt edzés részleges aktív/teljes energiáját a mentett összesítő projekcióból veszi. A HC nyers rekordlefedettség az aggregátum eredetétől külön marad.
- Célzott Room, Compose és coverage regressziók. Eszközön a Samsung Health és Health Connect késleltetett adatpublikációja még ellenőrizendő.
- 1.5.4. Telefon 281100, Wear OS 281102. Négy nyelvű Play szöveg a `docs/PLAY_LISTING.md` fájlban. Kiadás a zöld PR CI és main CI után.
