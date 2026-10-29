package com.example.capstonesample.data.model

import com.example.capstonesample.data.api.TaskResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskAssignmentDetailsTest {
    @Test
    fun structuredMaterialsAndEquipmentUseActualBackendFields() {
        val task = Gson().fromJson("""
            {"id":"task-a","task_name":"Foundation","status":"In Progress","progress_pct":45,
             "allocated_materials":[
               {"name":"Portland Cement","category":"Material","quantity":20,"unit":"bags","supplier":"ABC Construction Supply"},
               {"name":"Steel Rebar","category":"Material","quantity":30,"unit":"pcs"}],
             "resources":[{"id":"equipment-a","name":"Concrete Mixer","category":"Equipment","quantity":1,"unit":"unit"}]}
        """.trimIndent(), TaskResponse::class.java)
        val entries = taskAssignedResources(task.allocatedMaterials, task.resources, task.materialsRequired)
        assertEquals(listOf("Portland Cement", "Steel Rebar", "Concrete Mixer"), entries.map { it.name })
        assertEquals("ABC Construction Supply", entries[0].supplier)
        assertEquals("20 bags", resourceQuantityLabel(entries[0]))
        assertEquals("30 pcs", resourceQuantityLabel(entries[1]))
        assertEquals("Equipment", entries[2].category)
        assertEquals("1 unit", resourceQuantityLabel(entries[2]))
        assertEquals("In Progress", task.status)
        assertEquals(45, task.progress)
    }

    @Test
    fun matchingInventoryMetadataDoesNotDuplicateMaterialsOrReplaceAllocationQuantity() {
        val allocated = listOf(TaskAssignedResource(name = "Cement", quantity = 20.0, unit = "bags"))
        val resource = TaskAssignedResource(name = "cement", category = "Material", quantity = 500.0, supplier = "ABC")
        val entries = taskAssignedResources(allocated, listOf(resource), "20 bags Cement")
        assertEquals(1, entries.size)
        assertEquals("20 bags", resourceQuantityLabel(entries.single()))
        assertEquals("Material", entries.single().category)
        assertEquals("ABC", entries.single().supplier)
        val unknownQuantity = taskAssignedResources(listOf(TaskAssignedResource(name = "Cement")), listOf(resource), null)
        assertNull(unknownQuantity.single().quantity)
    }

    @Test
    fun missingStructuredAssignmentsUseLegacyTextOrEmptyState() {
        assertTrue(taskAssignedResources(null, null, null).isEmpty())
        assertTrue(taskAssignedResources(emptyList(), emptyList(), " \n ").isEmpty())
        assertEquals(listOf("20 bags Cement", "1 unit Mixer"),
            taskAssignedResources(null, null, "20 bags Cement, 1 unit Mixer").map { it.name })
        assertEquals("2.5 hours", resourceQuantityLabel(TaskAssignedResource(quantity = 2.5, unit = "hours")))
        assertNull(resourceQuantityLabel(TaskAssignedResource(name = "Rebar")))
    }

    @Test
    fun taskResponseIncludesAllMaterialAndEquipmentDescriptionsAndInstructions() {
        val task = Gson().fromJson("""
            {"id":"task-a","task_name":"Foundation","status":"In Progress","progress_pct":45,
             "materials_required":"20 bags Portland Cement, 30 pcs Steel Rebar, 1 unit Concrete Mixer",
             "site_instructions":"Follow the approved structural drawings. Verify dimensions before concrete pouring."}
        """.trimIndent(), TaskResponse::class.java)

        assertEquals(listOf("20 bags Portland Cement", "30 pcs Steel Rebar", "1 unit Concrete Mixer"),
            materialRequirementEntries(task.materialsRequired))
        assertEquals("Follow the approved structural drawings. Verify dimensions before concrete pouring.", task.siteInstructions)
        assertEquals("In Progress", task.status)
        assertEquals(45, task.progress)
    }

    @Test
    fun absentNullAndBlankAssignmentFieldsRemainSafe() {
        val missing = Gson().fromJson("""{"id":"a","task_name":"Task","status":"Pending","progress_pct":0}""", TaskResponse::class.java)
        val empty = Gson().fromJson("""{"id":"b","task_name":"Task","materials_required":null,"site_instructions":null}""", TaskResponse::class.java)
        for (task in listOf(missing, empty)) {
            assertTrue(materialRequirementEntries(task.materialsRequired).isEmpty())
            assertNull(task.siteInstructions)
        }
        for (value in listOf(null, "", "  \n\r  ", ", ; \n")) {
            assertTrue(materialRequirementEntries(value).isEmpty())
        }
    }

    @Test
    fun multilineResourcesPreserveProvidedDetailsAndNumericCommas() {
        val resources = "1,000 bags Cement\r\nBackhoe Excavator — 3 days; Steel Rebar, Concrete Mixer"
        assertEquals(listOf("1,000 bags Cement", "Backhoe Excavator — 3 days", "Steel Rebar", "Concrete Mixer"),
            materialRequirementEntries(resources))
        assertEquals(listOf("Concrete Mixer — Equipment — 1 unit — Supplier: ABC Construction Supply"),
            materialRequirementEntries("Concrete Mixer — Equipment — 1 unit — Supplier: ABC Construction Supply"))
    }

    @Test
    fun instructionsKeepParagraphsAndDoNotAffectWorkStatusOrProgress() {
        val task = Gson().fromJson("""
            {"id":"task-a","task_name":"Foundation","status":"pending","progress_pct":45,
             "materials_required":"","site_instructions":"Verify dimensions.\nFollow approved drawings."}
        """.trimIndent(), TaskResponse::class.java)
        assertEquals("Verify dimensions.\nFollow approved drawings.", task.siteInstructions)
        assertTrue(materialRequirementEntries(task.materialsRequired).isEmpty())
        assertEquals("pending", task.status)
        assertEquals(45, task.progress)
    }
}
