# A 2743 utáni Coach- és törlési UI-javítás

A felhasználó kérésére csak a már elkezdett Coach/törlés részt fejezzük be ebben a PR-ban. A videó, órás adatblokk, pulzus/regeneráció és a nagy térkép további munkája külön, fájltulajdon szerint kiosztandó; azok itt nem implementáltak.

- #56: a kompakt Home Coach-kártyán rövid, a jelenleg elérhető score-hoz igazodó szöveg. Adathiánynál ezt jelzi, nem talál ki új pontszámot. A meglévő részletes Coach-navigáció megmarad, a Home elférése regresszióval ellenőrzendő.
- #57: a részlet törlésgombja és az új listabeli törlés piros körvonalat/töltést és kontrasztos ikont/feliratot kap. Mindkét belépési pont ugyanazt a megerősítést és JournalViewModel/NativeHistoryDeletion műveletet használja. A listabeli gomb nem nyitja meg a részletet; a megerősítés megvárja az azonosított Room-rekordot. Mégse nem módosít, siker után a tényleges törlési callback frissíti a listát/statisztikát.

Natív tárolótörlés, tombstone, fájltakarítás, ownership/finalization/restore védelem változatlan. Nincs verzióléptetés vagy új APK-kiadás ebben a részfeladatban; a későbbi integrátor kezeli a következő közös kiadást. Actions eredmény a tényleges futás után rögzítendő, fizikai elfogadás külön.
