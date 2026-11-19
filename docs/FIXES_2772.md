# 2772 – gyűjtött javítások

Kódolás csak „mehet” után, egy közös CI-körrel.

| # | Terület | Visszajelzés | Terv | Állapot |
|---|---------|--------------|------|---------|
| 1 | Programok – sorrend húzás | A kártya kerete nem követi a mozgatást, lemarad (üres keret marad a helyén) | Ok: a programkártyán a keret a mozgatás *előtt* van a modifier-láncban, ezért nem mozdul. Megoldás: `trainPilotReorderItem` a keret elé (mint a gyakorlatsoroknál). Teszt: húzás közben a keret a kártyával mozog | gyűjtve |
