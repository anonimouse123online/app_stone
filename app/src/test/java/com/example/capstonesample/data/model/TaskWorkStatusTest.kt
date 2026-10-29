package com.example.capstonesample.data.model

import com.example.capstonesample.data.api.SubtaskItem
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class TaskWorkStatusTest {
    private val gson = Gson()

    @Test
    fun statusRequestsExcludeProgressAndCompletion() {
        val task = JsonParser.parseString(gson.toJson(UpdateTaskWorkStatusRequest("ongoing"))).asJsonObject
        assertEquals(setOf("status"), task.keySet())
        assertEquals("ongoing", task["status"].asString)

        val subtask = JsonParser.parseString(gson.toJson(UpdateSubtaskWorkStatusRequest("step-a", "pending"))).asJsonObject
        assertEquals(setOf("subtask_id", "status"), subtask.keySet())
        assertEquals("step-a", subtask["subtask_id"].asString)
        assertEquals("pending", subtask["status"].asString)
    }

    @Test
    fun backendResponsesKeepIndependentStatusesAndPercentages() {
        val response = gson.fromJson("""
            {"success":true,"data":{"id":"task-a","task_name":"Foundation","status":"Pending","progress_pct":45,
              "subtasks":[{"id":"a","title":"Excavation","status":"ongoing","progress":60,"completed":false},
                          {"id":"b","title":"Rebar","status":"pending","progress":0,"completed":false}]}}
        """.trimIndent(), TaskUpdateResponse::class.java)
        assertTrue(response.success)
        val task = requireNotNull(response.data)
        assertEquals("Pending", task.status)
        assertEquals(45, task.progress)
        val subtasks = requireNotNull(task.subtasks)
        assertEquals(listOf("ongoing", "pending"), subtasks.map { it.status })
        assertEquals(listOf(60, 0), subtasks.map { it.displayedProgress })
    }

    @Test
    fun legacySubtasksKeepCheckboxProgressAndDefaultWorkStatus() {
        val pending = gson.fromJson("""{"id":"a","title":"Step","completed":false}""", SubtaskItem::class.java)
        val completed = gson.fromJson("""{"id":"b","title":"Done","completed":true}""", SubtaskItem::class.java)
        assertEquals("Pending", workStatusLabel(pending.status))
        assertEquals(0, pending.displayedProgress)
        assertEquals(100, completed.displayedProgress)
    }

    @Test
    fun ongoingLabelSupportsStoredValuesAndTerminalStatusesStayLocked() {
        for (value in listOf("ongoing", "In Progress", "in-progress")) assertEquals("Ongoing", workStatusLabel(value))
        for (value in listOf("Completed", "Done", "Approved", "Verified", "Closed", "Cancelled")) {
            assertTrue(isFinishedWorkStatus(value))
            assertEquals(value, workStatusLabel(value))
        }
        assertFalse(isFinishedWorkStatus("Pending"))
        assertFalse(isFinishedWorkStatus("Ongoing"))
    }
}
