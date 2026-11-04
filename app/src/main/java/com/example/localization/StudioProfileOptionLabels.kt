package com.example.localization

import com.example.model.AppLanguage

/** Presentation only: profile values and user-defined text keep their saved keys. */
internal object StudioProfileOptionLabels {
    private val labels = mapOf(
        "Izomépítés" to listOf("Muscle building", "Muskelaufbau", "Creștere musculară"),
        "Fogyás / Szálkásítás" to listOf("Weight loss / definition", "Abnehmen / Definition", "Slăbire / definire"),
        "Erőnövelés" to listOf("Strength gain", "Kraftsteigerung", "Creșterea forței"),
        "Általános fittség" to listOf("General fitness", "Allgemeine Fitness", "Fitness general"),
        "Állóképesség és tónus" to listOf("Endurance and toning", "Ausdauer und Straffung", "Rezistență și tonifiere"),
        "Kezdő" to listOf("Beginner", "Anfänger", "Începător"),
        "Újrakezdő" to listOf("Returning beginner", "Wiedereinsteiger", "Reîncepător"),
        "Középhaladó" to listOf("Intermediate", "Fortgeschritten", "Intermediar"),
        "Haladó" to listOf("Advanced", "Erfahren", "Avansat"),
        "Otthon" to listOf("Home", "Zu Hause", "Acasă"),
        "Edzőterem" to listOf("Gym", "Fitnessstudio", "Sală"),
        "Vegyes (Otthon + Terem)" to listOf("Mixed (home + gym)", "Gemischt (zu Hause + Studio)", "Mixt (acasă + sală)"),
        "Szabadtér / Street workout" to listOf("Outdoors / street workout", "Draußen / Street Workout", "În aer liber / street workout"),
        "Saját testsúly" to listOf("Body weight", "Körpergewicht", "Greutatea corpului"),
        "Egykezes kézisúlyzó (1 db)" to listOf("Single dumbbell (1)", "Eine Kurzhantel (1)", "O ganteră (1)"),
        "Kézisúlyzó pár (2 db)" to listOf("Dumbbell pair (2)", "Kurzhantelpaar (2)", "Pereche de gantere (2)"),
        "Kétkezes rúd (Súlyzórúd)" to listOf("Barbell", "Langhantel", "Haltera"),
        "Pad / Szék / Kanapé" to listOf("Bench / chair / sofa", "Bank / Stuhl / Sofa", "Bancă / scaun / canapea"),
        "Stabil támaszték / Fal / Szék" to listOf("Stable support / wall / chair", "Stabile Stütze / Wand / Stuhl", "Sprijin stabil / perete / scaun"),
        "Fellépő / Doboz / Lépcső" to listOf("Step / box / stairs", "Step / Kasten / Treppe", "Platformă / cutie / trepte"),
        "Húzódzkodó rúd" to listOf("Pull-up bar", "Klimmzugstange", "Bară de tracțiuni"),
        "Tolódzkodó korlát" to listOf("Dip bars", "Dip-Barren", "Bare paralele"),
        "Gumiszalag / Ellenállás-szalag" to listOf("Resistance band", "Widerstandsband", "Bandă elastică"),
        "Edzőtermi gépek / Csigás gépek" to listOf("Gym / cable machines", "Fitness- / Kabelzuggeräte", "Aparate de sală / cu cablu"),
        "Láb" to listOf("Legs", "Beine", "Picioare"),
        "Mell" to listOf("Chest", "Brust", "Piept"),
        "Hát" to listOf("Back", "Rücken", "Spate"),
        "Váll" to listOf("Shoulders", "Schultern", "Umeri"),
        "Kar" to listOf("Arms", "Arme", "Brațe"),
        "Törzs" to listOf("Core", "Rumpf", "Trunchi"),
        "Alsó hát" to listOf("Lower back", "Unterer Rücken", "Zona lombară"),
        "Térdkímélő" to listOf("Knee-friendly", "Knieschonend", "Protejarea genunchilor"),
        "Kézisúlyzó" to listOf("Dumbbell", "Kurzhantel", "Ganteră"),
        "Rúd" to listOf("Barbell", "Langhantel", "Haltera"),
        "Bolgár guggolás" to listOf("Bulgarian split squat", "Bulgarische Kniebeuge", "Genuflexiune bulgărească"),
        "Egykezes evezés" to listOf("One-arm row", "Einarmiges Rudern", "Ramat cu un braț"),
        "Fekvenyomás" to listOf("Bench press", "Bankdrücken", "Împins din culcat"),
        "Román felhúzás" to listOf("Romanian deadlift", "Rumänisches Kreuzheben", "Îndreptări românești"),
        "Húzódzkodás" to listOf("Pull-up", "Klimmzug", "Tracțiune"),
        "Tolódzkodás" to listOf("Dip", "Dip", "Flotare la paralele"),
        "Kitörés sétálva" to listOf("Walking lunge", "Ausfallschritte im Gehen", "Fandări din mers"),
        "Vállból nyomás" to listOf("Shoulder press", "Schulterdrücken", "Împins deasupra capului"),
        "Alap saját testsúlyos gyakorlatok (fekvőtámasz, plank, guggolás)" to listOf("Basic bodyweight movements (push-ups, plank, squats)", "Grundübungen mit Körpergewicht (Liegestütze, Plank, Kniebeugen)", "Exerciții cu greutatea corpului (flotări, plank, genuflexiuni)"),
        "1 db kézisúlyzós gyakorlatok (pl. goblet guggolás, egykezes evezés)" to listOf("Single-dumbbell movements (goblet squat, one-arm row)", "Übungen mit einer Kurzhantel (Goblet Squat, einarmiges Rudern)", "Exerciții cu o ganteră (genuflexiuni goblet, ramat cu un braț)"),
        "Páros egykezes súlyzós gyakorlatok (nyomások, kitörések, kar)" to listOf("Paired dumbbell movements (presses, lunges, arms)", "Übungen mit zwei Kurzhanteln (Drücken, Ausfallschritte, Arme)", "Exerciții cu două gantere (împins, fandări, brațe)"),
        "Kétkezes rudas alapgyakorlatok (guggolás, felhúzás, evezés, nyomás)" to listOf("Barbell compound movements (squat, deadlift, row, press)", "Langhantel-Grundübungen (Kniebeuge, Kreuzheben, Rudern, Drücken)", "Exerciții de bază cu haltera (genuflexiuni, îndreptări, ramat, împins)"),
        "Támasz pl. bolgár guggoláshoz, fekvenyomáshoz vagy csípőemeléshez" to listOf("Support for split squats, bench press or hip thrusts", "Stütze für bulgarische Kniebeugen, Bankdrücken oder Hüftheben", "Sprijin pentru genuflexiuni bulgărești, împins sau ridicări de bazin"),
        "Kéztámasz pl. egykezes evezéshez vagy vádliemeléshez" to listOf("Hand support for one-arm rows or calf raises", "Handstütze für einarmiges Rudern oder Wadenheben", "Sprijin pentru ramat cu un braț sau ridicări pe vârfuri"),
        "Magasítás fellépéshez (step-up) és felugráshoz" to listOf("Raised platform for step-ups and jumps", "Erhöhung für Step-ups und Sprünge", "Platformă pentru urcări și sărituri"),
        "Húzódzkodás, függeszkedés és lábemelések" to listOf("Pull-ups, hanging and leg raises", "Klimmzüge, Hängen und Beinheben", "Tracțiuni, atârnări și ridicări de picioare"),
        "Tolódzkodás mellre és tricepszre" to listOf("Dips for chest and triceps", "Dips für Brust und Trizeps", "Flotări la paralele pentru piept și triceps"),
        "Gumiszalagos evezések, lehúzások és izolációk" to listOf("Band rows, pulldowns and isolation movements", "Rudern, Latzug und Isolationsübungen mit Band", "Ramat, tracțiuni și exerciții de izolare cu bandă"),
        "Kettlebell swing, goblet guggolás és nyomás" to listOf("Kettlebell swings, goblet squats and presses", "Kettlebell Swings, Goblet Squats und Drücken", "Balansări cu kettlebell, genuflexiuni goblet și împins"),
        "Lábtoló, hátlehúzó, csigás és vezetett gépek" to listOf("Leg press, pulldown, cable and guided machines", "Beinpresse, Latzug, Kabelzug und geführte Geräte", "Presă pentru picioare, tracțiuni, cabluri și aparate ghidate"),
        "Erő & Hipertrófia" to listOf("Strength & Hypertrophy", "Kraft & Hypertrophie", "Forță & Hipertrofie"),
        "Funkcionális & Állóképesség" to listOf("Functional & Endurance", "Funktionell & Ausdauer", "Funcțional & Rezistență"),
        "Kezdő kímélő (Ízületbarát)" to listOf("Gentle Beginner (Joint-friendly)", "Sanft für Anfänger (Gelenkschonend)", "Începător blând (Protejarea articulațiilor)"),
        "Dupla progresszió (8-12 ism. -> +2.5 kg)" to listOf("Double progression (8-12 reps -> +2.5 kg)", "Doppelte Progression (8-12 Wdh. -> +2.5 kg)", "Progresie dublă (8-12 rep. -> +2.5 kg)"),
        "Lineáris (+1.25 kg hetente)" to listOf("Linear (+1.25 kg weekly)", "Linear (+1.25 kg wöchentlich)", "Liniară (+1.25 kg săptămânal)"),
        "RIR-alapú (1-2 RIR megtartása)" to listOf("RIR-based (maintain 1-2 RIR)", "RIR-basiert (1-2 RIR beibehalten)", "Bazată pe RIR (menținere 1-2 RIR)")
    )
    fun label(value: String, language: AppLanguage): String {
        if (language == AppLanguage.HU) return value
        val index = when (language) { AppLanguage.EN -> 0; AppLanguage.DE -> 1; AppLanguage.RO -> 2; else -> return value }
        return labels[value]?.get(index) ?: value
    }
    fun goal(value: String, language: AppLanguage): String = label(value, language)
    fun location(value: String, language: AppLanguage): String = label(value, language)
    fun experience(value: String, language: AppLanguage): String = label(value, language)
    fun trainingStyle(value: String, language: AppLanguage): String = label(value, language)
    fun progressionRule(value: String, language: AppLanguage): String = label(value, language)
    fun description(value: String, language: AppLanguage): String =
        value.split(" • ").joinToString(" • ") { label(it, language) }
}
