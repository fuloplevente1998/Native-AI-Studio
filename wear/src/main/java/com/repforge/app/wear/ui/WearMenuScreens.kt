package com.repforge.app.wear

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.json.JSONObject

@Composable
internal fun SaveSyncScreen(state: WearSaveSyncState?,onRetry: ()->Unit,onBack: ()->Unit) {
    RoundList(wearSaveTitle()) {
        centerItem { Text(state?.let { wearSaveLabel(it.phase) } ?: wearSaveEmpty(),color=Gold,
            fontSize=14.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp)) }
        if (wearSaveDetail(state) != (state?.let { wearSaveLabel(it.phase) } ?: wearSaveEmpty())) centerItem { Text(wearSaveDetail(state),color=Muted,fontSize=11.sp,textAlign=TextAlign.Center,
            modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp).testTag("wear-save-detail")) }
        if(state?.retryable==true)centerItem { Pill(wearSaveRetry(),tone=Secondary,onClick=onRetry) }
        centerItem { Pill(wearSaveBack(),tone=Secondary,onClick=onBack) }
    }
}

@Composable
internal fun MainMenuScreen(
    hasWorkout: Boolean,
    quickWorkout: Boolean,
    onOpenPhone: () -> Unit,
    onRoute: (String) -> Unit,
) {
    val status=LocalSaveSync.current
    val ownership=LocalOwnership193.current
    RoundList(WearMenu216.text("menu")) {
        if(status!=null&&status.phase==WearSavePhase.ERROR)saveSyncItem()
        if(ownership!=null&&WearOwnership193.pending(ownership))centerItem {
            Pill(WearOwnershipText193.text("title"),tone=Card) {onRoute("ownership193")}
        }
        for((key,destination) in WearMenu216.entries(hasWorkout,quickWorkout)) centerItem {
            Pill(WearMenu216.text(key),tone=if(key in listOf("resume","quick","add"))Gold else Card) {onRoute(destination)}
        }
        if(status!=null&&status.phase!=WearSavePhase.ERROR)saveSyncItem()
        if(hasWorkout&&ownership!=null&&!WearOwnership193.pending(ownership))centerItem {
            Pill(WearOwnershipText193.text("title"),tone=Card) {onRoute("ownership193")}
        }
        centerItem { Pill(WearMenu216.text("phone"),tone=Card,onClick=onOpenPhone) }
        if(hasWorkout)centerItem { Pill(WearMenu216.text("discard"),tone=Danger) {onRoute("discard")} }
    }
}

@Composable
internal fun OwnershipScreen193(
    workout: WearWorkout,connected: Boolean?,onRequest: (() -> Unit)?,onRetry: (() -> Unit)?,
    onResume: () -> Unit,onOpenPhone: () -> Unit,onBack: () -> Unit,
    onGps: (() -> Unit)? = null,
) {
    val pending=WearOwnership193.pending(workout)
    val watch=workout.timerOwner=="watch"
    RoundList(WearOwnershipText193.text("title")) {
        centerItem { Text(WearOwnershipText193.text(if(watch)"watch" else "phone"),color=White,fontSize=14.sp,textAlign=TextAlign.Center) }
        centerItem { Text(WearOwnershipText193.text(when {pending->"pending";watch&&workout.workoutPaused->"ready";watch->"return";else->"connected"}),
            color=Muted,fontSize=12.sp,textAlign=TextAlign.Center) }
        when {
            pending->centerItem { Pill(WearOwnershipText193.text("retry"),enabled=onRetry!=null) { onRetry?.invoke() } }
            !watch->centerItem { Pill(WearOwnershipText193.text("request"),enabled=connected!=false&&onRequest!=null) { onRequest?.invoke() } }
            workout.workoutPaused->centerItem { Pill(WearOwnershipText193.text("resume")) { onResume() } }
        }
        if(onGps!=null)centerItem { Pill(WearGpsText193.text("title"),tone=Secondary) { onGps() } }
        centerItem { Pill(WearOwnershipText193.text("openPhone"),tone=Secondary) { onOpenPhone() } }
        centerItem { Pill(WearOwnershipText193.text("back"),tone=Secondary) { onBack() } }
    }
}

@Composable
internal fun GpsStartScreen216(activityId: String,onStart: ()->Unit,onCancel: ()->Unit,
    language: String=java.util.Locale.getDefault().language) {
    val context=LocalContext.current
    var selected by remember(activityId) {mutableStateOf(WearMenu216.enabled(context,activityId))}
    var failed by remember {mutableStateOf(false)}
    RoundList(WearMenu216.text("gps",language)) {
        centerItem { Pill(WearMenu216.text("gps",language)+": "+WearMenu216.text(if(selected)"on" else "off",language),tone=Gold) {selected=!selected} }
        if(failed)centerItem {Text(WearGpsText193.text("storage",language),color=DangerText,fontSize=11.sp)}
        centerItem { Pill(WearMenu216.text("start",language)) {
            if(WearMenu216.choose(context,activityId,selected))onStart() else failed=true
        } }
        centerItem { Pill(WearMenu216.text("cancel",language),tone=Secondary,onClick=onCancel) }
        detailsItem(WearMenu216.text("remember",language), WearMenu216.text("consent",language))
    }
}

@Composable
internal fun GpsRouteScreen193(
    workout: WearWorkout,status: JSONObject?,onStart: (() -> Unit)?,onStop: (() -> Unit)?,onBack: () -> Unit,
    language: String=java.util.Locale.getDefault().language,
) {
    fun text(key: String)=WearGpsText193.text(key,language)
    val code=status?.optString("state")?.takeIf { it in listOf("idle","acquiring","recording","paused","stopped","permission","locationOff","storage","unavailable","interrupted","deleted") } ?: "idle"
    val allowed=WearGps193.owns(workout)&&WearGps193.distance(workout.exercise)&&workout.currentSet?.let { !it.done&&it.setId.isNotBlank() }==true
    val context=LocalContext.current
    var preferred by remember(workout.exercise?.id) {mutableStateOf(WearMenu216.enabled(context,workout.exercise?.id.orEmpty()))}
    RoundList(text("title")) {
        centerItem { Text(text(code),color=White,fontSize=13.sp,textAlign=TextAlign.Center) }
        if(!allowed)centerItem { Text(text("distanceOnly"),color=Muted,fontSize=12.sp,textAlign=TextAlign.Center) }
        if(status?.optBoolean("armed")==true)centerItem { Pill(text("stop"),enabled=onStop!=null,tone=Secondary) { onStop?.invoke() } }
        if(status?.optBoolean("running")!=true) {
            centerItem { Pill(text("start"),enabled=allowed&&onStart!=null) { onStart?.invoke() } }
        }
        if(WearMenu216.eligible(workout.exercise))centerItem {
            Pill(WearMenu216.text("next",language)+": "+WearMenu216.text(if(preferred)"on" else "off",language),tone=Secondary) {
                if(WearMenu216.choose(context,workout.exercise!!.id,!preferred))preferred=!preferred
            }
        }
        val count=status?.optInt("points") ?: 0
        centerItem { Text(text("points").replace("{count}",count.toString()),color=Muted,fontSize=12.sp,textAlign=TextAlign.Center) }
        centerItem { Pill(text("back"),tone=Secondary) { onBack() } }
        detailsItem(text("received").replace("{count}",(status?.optInt("receivedFixes") ?: 0).toString()),
            WearMenu216.text("aggregate",language), WearMenu216.text("consent",language), text("consent"))
    }
}

@Composable
internal fun SettingsScreen(
    enabled: Boolean,
    gps: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onGps: () -> Unit,
    onMetrics: () -> Unit,
    onOpenPhone: () -> Unit,
    backLabel: String,
    onBack: () -> Unit,
) {
    RoundList(wearText("Beállítások")) {
        centerItem { JournalText(if (enabled) wearText("Edzésmérés bekapcsolva") else wearText("Edzésmérés kikapcsolva"), Gold, 12) }
        centerItem {
            Pill(
                if (enabled) wearText("Mérés kikapcsolása") else wearText("Mérések engedélyezése"),
                tone = if (enabled) Secondary else Gold,
                onClick = if (enabled) onDisable else onEnable,
            )
        }
        centerItem {
            Pill(if (gps) wearText("GPS kikapcsolása") else wearText("GPS engedélyezése"), tone = Card, onClick = onGps)
        }
        centerItem { Pill(wearText("Órás mérések"), tone = Card, onClick = onMetrics) }
        centerItem { Pill(wearText("Telefon megnyitása"), tone = Secondary, onClick = onOpenPhone) }
        centerItem { Pill(backLabel, tone = Secondary, onClick = onBack) }
        detailsItem(wearText("A GPS a következő mozgásos edzés mérésétől használható."),
            wearText("A színek a telefonon választott témát követik."))
    }
}

/** Journal paragraphs keep the entire text and leave room for the curved screen edge. */
@Composable
internal fun JournalText(
    text: String,
    color: Color = Muted,
    size: Int = 11,
    weight: FontWeight = FontWeight.Normal,
) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth(.8f),
        color = color,
        fontSize = size.sp,
        lineHeight = (size + 2).sp,
        fontWeight = weight,
        textAlign = TextAlign.Center,
        softWrap = true,
    )
}

internal fun miniJournalDate(value: String): String =
    try {
        DateTimeFormatter.ofPattern("MM. dd. HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(value))
    } catch (_: Exception) {
        "—"
    }

internal fun miniNumber(value: Double?, decimals: Int = 0): String? {
    if (value == null || !value.isFinite()) return null
    return if (decimals == 0) kotlin.math.round(value).toInt().toString()
    else String.format(java.util.Locale.forLanguageTag("hu-HU"), "%.${decimals}f", value)
}
