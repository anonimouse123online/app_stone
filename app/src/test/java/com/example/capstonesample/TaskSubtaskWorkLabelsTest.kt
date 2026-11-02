package com.example.capstonesample

import com.example.capstonesample.data.api.SubtaskItem
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSubtaskWorkLabelsTest {

    @Test
    fun returnsOnlySubtasksWhenSubtasksExist() {
        val subtasks = listOf(
            SubtaskItem(id = "1", title = "Rebar Cutting", progress = 40),
            SubtaskItem(id = "2", title = "Formwork Assembly", progress = 10),
            SubtaskItem(id = "3", title = "Concrete Pouring", progress = 0)
        )

        val labels = subtaskWorkLabels(subtasks)

        assertEquals(listOf("Rebar Cutting", "Formwork Assembly", "Concrete Pouring"), labels)
    }

    @Test
    fun trimsTitlesFiltersBlankAndDeduplicates() {
        val subtasks = listOf(
            SubtaskItem(id = "1", title = "  Electrical Rough-in  "),
            SubtaskItem(id = "2", title = "   "),
            SubtaskItem(id = "3", title = "Electrical Rough-in"),
            SubtaskItem(id = "4", title = "Plumbing Rough-in")
        )

        val labels = subtaskWorkLabels(subtasks)

        assertEquals(listOf("Electrical Rough-in", "Plumbing Rough-in"), labels)
    }

    @Test
    fun fallsBackToGeneralWorkWhenSubtasksEmptyOrBlank() {
        assertEquals(listOf("General Work"), subtaskWorkLabels(emptyList()))
        assertEquals(
            listOf("General Work"),
            subtaskWorkLabels(listOf(SubtaskItem(id = "1", title = "   ")))
        )
    }

    @Test
    fun allSubtasksCompletedRejectsIncompleteSubtasks() {
        // Reproduces the exact case from user report:
        // sukda: 100%, kalota: 100%, tanawa: 60%
        val subtasks = listOf(
            SubtaskItem(id = "1", title = "sukda", progress = 100, completed = true),
            SubtaskItem(id = "2", title = "kalota", progress = 100, completed = true),
            SubtaskItem(id = "3", title = "tanawa", progress = 60, completed = false)
        )

        org.junit.Assert.assertFalse(allSubtasksCompleted(subtasks))
    }

    @Test
    fun allSubtasksCompletedAcceptsAllSubtasksAtHundredPercent() {
        val subtasks = listOf(
            SubtaskItem(id = "1", title = "sukda", progress = 100, completed = true),
            SubtaskItem(id = "2", title = "kalota", progress = 100, completed = false),
            SubtaskItem(id = "3", title = "tanawa", progress = 100, completed = true)
        )

        org.junit.Assert.assertTrue(allSubtasksCompleted(subtasks))
    }

    @Test
    fun allSubtasksCompletedAcceptsEmptySubtasks() {
        org.junit.Assert.assertTrue(allSubtasksCompleted(emptyList()))
    }
}
