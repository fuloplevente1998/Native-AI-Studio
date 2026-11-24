package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.*
import com.repforge.app.data.db.*
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.shared.LegacySyncIdentity18
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import com.example.ui.viewmodel.clearBannerMessage
import com.example.ui.viewmodel.clearCalendarPlans
import com.example.ui.viewmodel.deleteCalendarEvent
import com.example.ui.viewmodel.deleteEditorDay
import com.example.ui.viewmodel.deleteEditorProgram
import com.example.ui.viewmodel.modifyCalendarEvent
import com.example.ui.viewmodel.openProgramEditor
import com.example.ui.viewmodel.requestCalendarClear
import com.example.ui.viewmodel.updatePlanningParameters

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class ProgramCalendarRemovalRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private val parent = SupervisorJob()
    private val scope = CoroutineScope(parent + Dispatchers.Main.immediate)

    @Before fun prepare() {
        val field=PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible=true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close();field.set(null,null)
        db=Room.inMemoryDatabaseBuilder(context,TrainPilotDatabase::class.java).build()
        settings=TrainPilotSettingsStore(context,publishTheme={})
        native {
            settings.resetAllSettingsForEraseLocal()
            TrainPilotDatabase.ensureBuiltinCatalogSeeded(db,context)
            val source=requireNotNull(db.programDao().getProgramWithDaysById("home-basic"))
            db.programDao().saveProgramWithDays(source.program.copy(id="own",builtin=false,
                rawJson=JSONObject(source.program.rawJson).put("id","own").put("builtin",false).toString()),
                source.days.map { it.copy(compositeId="own:${it.dayId}",programId="own") })
            settings.setActiveProgramId("home-basic")
        }
    }
    @After fun close() { parent.cancel();await { parent.isCompleted };db.close() }

    @Test fun publicEditorDeletesInactiveProgramWithoutLosingHistoryOrCompletedCalendar() {
        val history=history("completed")
        val calendar=row("completed","completed")
        native { db.workoutHistoryDao().insertWorkout(history);db.calendarScheduleDao().upsertEntry(calendar) }
        val vm=model();vm.openProgramEditor("own");mutate(vm) { vm.deleteEditorProgram() }
        assertNull(native { db.programDao().getProgramWithDaysById("own") })
        assertEquals(history,native { db.workoutHistoryDao().getWorkoutWithDetailsById("history")!!.workout })
        assertEquals(calendar,native { db.calendarScheduleDao().getEntry(calendar.id) })
        assertEquals("home-basic",native { settings.current().activeProgramId })
        assertTrue(vm.uiState.value.programs.none { it.id=="own" })
    }

    @Test fun publicEditorCanRemoveAHistoricalDayWhileTheSavedWorkoutKeepsItsDayName() {
        val history=history("completed")
        native { db.workoutHistoryDao().insertWorkout(history);db.calendarScheduleDao().upsertEntry(row("completed","completed")) }
        val vm=model();vm.openProgramEditor("own",0);mutate(vm) { vm.deleteEditorDay() }
        val program=native { db.programDao().getProgramWithDaysById("own")!! }
        assertTrue(program.days.none { it.dayId=="A" });assertTrue(program.days.any { it.dayId=="B" })
        assertEquals(history,native { db.workoutHistoryDao().getWorkoutWithDetailsById("history")!!.workout })
    }

    @Test fun activeBuiltinAndLivePlansStayProtectedThenPublicCalendarClearAllowsDeletion() {
        var opened=native { db.programDao().getProgramWithDaysById("own")!! }
        native { settings.setActiveProgramId("own") }
        assertTrue(native { runCatching { NativeProgramEditor.delete(context,db,opened,settings) }.isFailure })
        native { settings.setActiveProgramId("home-basic") }
        val builtin=native { db.programDao().getProgramWithDaysById("home-basic")!! }
        assertTrue(native { runCatching { NativeProgramEditor.delete(context,db,builtin,settings) }.isFailure })
        native { db.calendarScheduleDao().upsertEntry(row("planned"));writeDoc(listOf("planned")) }
        val vm=model();vm.openProgramEditor("own");mutate(vm) { vm.deleteEditorProgram() }
        assertNotNull(native { db.programDao().getProgramWithDaysById("own") })
        assertTrue(vm.uiState.value.statusBannerMessage!!.contains("tervezett"))
        vm.requestCalendarClear();await { !vm.uiState.value.calendarClearBusy }
        assertTrue(vm.uiState.value.showCalendarClearDialog)
        vm.clearCalendarPlans();await { !vm.uiState.value.calendarClearBusy }
        assertTrue(vm.uiState.value.calendarEvents.isEmpty());assertTrue(vm.uiState.value.calendarUndoStack.isEmpty())
        assertEquals("skipped",native { db.calendarScheduleDao().getEntry("planned")!!.status })
        mutate(vm) { vm.deleteEditorProgram() }
        assertNull(native { db.programDao().getProgramWithDaysById("own") })
    }

    @Test fun bulkClearIsDurablePreservesHistoryAndProducesCloudDeletesWithoutStaleResurrection() {
        val rows=listOf(row("planned"),row("missed","skipped"),row("completed","completed"),row("linked"))
        val history=history("linked")
        native {
            db.calendarScheduleDao().upsertEntries(rows);db.workoutHistoryDao().insertWorkout(history)
            writeDoc(rows.map { it.id });db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("sentinel","{\"keep\":true}",1))
            db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("plannerSettings","{\"mode\":\"weekly\",\"nativePlannerReceipt\":\"old\",\"nativePlannerIds\":{}}",2))
        }
        val before=native { NativeCalendarClear.preview(db) }
        val ticket=NativeDataReplacement.continuationForCurrentData(context)
        val result=native { NativeCalendarClear.commit(context,db,before,ticket) }
        assertEquals(setOf("planned","missed"),result.ids);assertEquals(2,result.count)
        val after=native { db.calendarScheduleDao().getAllEntries() }
        for(id in result.ids) {
            val value=after.single { it.id==id };assertEquals("skipped",value.status)
            assertTrue(JSONObject(value.rawJson).getBoolean("cancelled"));assertEquals("preserve",JSONObject(value.rawJson).getString("note"))
            assertTrue(value.updatedAt>rows.single { it.id==id }.updatedAt)
        }
        assertEquals(rows.single { it.id=="completed" },after.single { it.id=="completed" })
        assertEquals(rows.single { it.id=="linked" },after.single { it.id=="linked" })
        assertEquals(history,native { db.workoutHistoryDao().getWorkoutWithDetailsById("history")!!.workout })
        val document=native { db.keyValueDocumentDao().getDocument("native:calendarState")!! }
        assertEquals(0,JSONObject(document.json).getJSONArray("undoStack").length())
        assertEquals(2,JSONObject(document.json).getJSONArray("events").length())
        assertEquals("{\"keep\":true}",native { db.keyValueDocumentDao().getDocument("sentinel")!!.json })
        assertFalse(JSONObject(native { db.keyValueDocumentDao().getDocument("plannerSettings")!!.json }).has("nativePlannerReceipt"))
        val operations=NativeCalendarSyncPlan.operations(after).toList()
        assertTrue(operations.single { it.id==rows[0].googleEventId }.delete)
        assertFalse(operations.single { it.id==rows[2].googleEventId }.delete)
        val local=JSONObject().put("scheduled",JSONArray(after.map { JSONObject(it.rawJson).put("id",it.id).put("status",it.status).put("updatedAt",it.updatedAt) }))
        val remote=JSONObject().put("scheduled",JSONArray(rows.map { JSONObject(it.rawJson) }))
        val merged=GoogleSyncRepository(context).mergeSync18(local,listOf(remote),null,emptyList(),emptySet(),emptySet(),false)
        val mergedRows=merged.getJSONArray("scheduled")
        val plan=(0 until mergedRows.length()).map { mergedRows.getJSONObject(it) }.single { it.getString("id")=="planned" }
        assertTrue(plan.getBoolean("cancelled"));assertEquals("skipped",plan.getString("status"))
    }

    @Test fun changedPreviewReplacementAndRunningWorkoutRejectClearWithoutPartialWrites() {
        native { db.calendarScheduleDao().upsertEntry(row("planned"));writeDoc(listOf("planned")) }
        val ticket=NativeDataReplacement.continuationForCurrentData(context)
        val preview=native { NativeCalendarClear.preview(db) }
        native { db.calendarScheduleDao().upsertEntry(row("planned").copy(updatedAt=51)) }
        val current=native { NativeCalendarClear.preview(db) }
        assertTrue(native { runCatching { NativeCalendarClear.commit(context,db,preview,ticket) }.isFailure })
        assertEquals(current,native { NativeCalendarClear.preview(db) })
        native { NativeDataReplacement.exclusively(context) {} }
        assertTrue(native { runCatching { NativeCalendarClear.commit(context,db,current,ticket) }.isFailure })
        val fresh=NativeDataReplacement.continuationForCurrentData(context)
        native { NativeWorkoutSession.startQuickExercises(context,listOf("plank"),"Running",false,db) }
        assertTrue(native { runCatching { NativeCalendarClear.commit(context,db,current,fresh) }.isFailure })
        assertEquals(current,native { NativeCalendarClear.preview(db) })
    }

    @Test fun documentWriteFailureRollsBackEveryCancellationRow() {
        native { db.calendarScheduleDao().upsertEntry(row("planned"));writeDoc(listOf("planned")) }
        val preview=native { NativeCalendarClear.preview(db) }
        native { db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_clear BEFORE INSERT ON kv_documents BEGIN SELECT RAISE(ABORT, 'fixture write failure'); END") }
        val ticket=NativeDataReplacement.continuationForCurrentData(context)
        assertTrue(native { runCatching { NativeCalendarClear.commit(context,db,preview,ticket) }.isFailure })
        assertEquals(preview,native { NativeCalendarClear.preview(db) })
    }

    @Test fun editingOneSchedulePreservesOtherRowsAndRejectsAStaleProjectionAfterRestore() {
        val edited = row("planned")
        val untouched = row("other")
        native {
            db.calendarScheduleDao().upsertEntries(listOf(edited, untouched))
            val events = JSONArray(listOf(edited, untouched).map(NativeCalendarCleanup202611::toCalendarStateEvent))
            db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity(NativeCalendarBackup.STATE_KEY,
                JSONObject().put("events", events).toString(), 50))
        }
        val vm = model()
        vm.modifyCalendarEvent(edited.id, "Updated", "A", "19:30", 60)
        await { native { (db.calendarScheduleDao().getEntry(edited.id)?.updatedAt ?: 0) > edited.updatedAt } }
        assertEquals(untouched, native { db.calendarScheduleDao().getEntry(untouched.id) })

        native { NativeDataReplacement.exclusively(context) {} }
        vm.deleteCalendarEvent(untouched.id)
        assertTrue(vm.uiState.value.calendarEvents.any { it.id == untouched.id })
        assertEquals(untouched, native { db.calendarScheduleDao().getEntry(untouched.id) })
    }

    @Test fun studioPlannerPreferencesSurviveViewModelRebinding() {
        val vm = model()
        vm.updatePlanningParameters("Minden nap", "Alap B", "2026. 12. 08.", "16:45", 75, 5)
        await { native { db.keyValueDocumentDao().getDocument(NativeCalendarBackup.STUDIO_SETTINGS_KEY) != null } }
        val restored = model().uiState.value
        assertEquals("Minden nap", restored.planningCadenceHu)
        assertEquals("Alap B", restored.planningFirstDayHu)
        assertEquals("2026. 12. 08.", restored.planningStartDate)
        assertEquals("16:45", restored.planningTime)
        assertEquals(75, restored.planningDurationMin)
        assertEquals(5, restored.planningWeeksCount)
    }

    private fun row(id:String,status:String="planned"):CalendarScheduleEntity {
        val hour=when(id){"planned"->18;"missed"->17;"completed"->16;"linked"->15;else->14}
        val start="2026-12-04T${hour}:00:00Z"
        return CalendarScheduleEntity(id,LegacySyncIdentity18.calendarEventId(id,"2026-12-04","own","A"),
            "2026-12-04",start,"2026-12-04T${hour+1}:00:00Z","own","Saved own program","A","Saved A day",status,50,
            JSONObject().put("id",id).put("status",status).put("start",start).put("updatedAt",50).put("note","preserve").toString())
    }
    private suspend fun writeDoc(ids:List<String>) {
        val events=JSONArray(ids.map { JSONObject().put("id",it).put("dayOfMonth",4).put("dateIso","2026-12-04").put("timeLabel","18:00")
            .put("titleHu","Saved own program").put("programId","own").put("dayId","A").put("status",if(it=="completed")"COMPLETED" else "PLANNED") })
        db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("native:calendarState",JSONObject().put("events",events)
            .put("undoStack",JSONArray().put(events)).toString(),50))
    }
    private fun history(schedule:String)=WorkoutHistoryEntity(id="history",workoutKey="history",syncId="history",
        started="2026-11-16T18:00:00Z",finished="2026-11-16T19:00:00Z",startedEpochMs=1794852000000,finishedEpochMs=1794855600000,durationSeconds=3600,
        programId="own",programName="Saved own program",dayId="A",dayName="Saved A day",scheduleId=schedule,quickWorkout=false,
        originDevice="phone",ownershipProtocol193=0,ownerEpoch193=0,effortFeedback=null,feedbackSkipped=false,feedbackUpdatedAt=0,
        totalVolumeKg=100.0,completedSetsCount=1,totalSetsCount=1,totalRepsCount=10,hasPr226=false,prRecordsJson226="[]",
        wearMetricsJson=null,health240Json=null,timeLogs162Json=null,activeIntervalsJson=null,editedAt=null,
        rawJson="{\"id\":\"history\",\"programName\":\"Saved own program\",\"exercises\":[]}")
    private fun model()=TrainPilotViewModel().also { vm->vm.bindNativeEnvironment(context,settings,db,scope);await { !vm.uiState.value.savedRecordsLoading&&vm.uiState.value.programs.any { it.id=="own" } } }
    private fun mutate(vm:TrainPilotViewModel,action:()->Unit) { vm.clearBannerMessage();action();await { !vm.uiState.value.libraryMutationBusy&&vm.uiState.value.statusBannerMessage!=null } }
    private fun await(condition:()->Boolean) { val end=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(!condition()){ shadowOf(Looper.getMainLooper()).idle();check(System.nanoTime()<end){"Program/calendar operation timed out"};Thread.sleep(5) } }
    private fun <T> native(block:suspend()->T):T {
        val worker=Executors.newSingleThreadExecutor();val task=worker.submit<T>{runBlocking { block() }}
        val end=System.nanoTime()+TimeUnit.SECONDS.toNanos(30)
        try { while(true) { shadowOf(Looper.getMainLooper()).idle();try{return task.get(10,TimeUnit.MILLISECONDS)}catch(_:TimeoutException){check(System.nanoTime()<end)} } }
        finally { task.cancel(true);worker.shutdownNow() }
    }
}
