package com.example.capstonesample.data.model

import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueReportRequestTest {
    @Test
    fun issueSubmissionRetainsExistingJsonContractWithoutInventedImageFields() {
        val request = CreateIssueRequest(
            title = "Site safety concern",
            category = "Safety Hazard",
            priority = "Medium",
            location = "North access",
            description = "Inspection required."
        )
        val actual = JsonParser.parseString(Gson().toJson(request))
        val expected = JsonParser.parseString("""
            {"title":"Site safety concern","category":"Safety Hazard","priority":"Medium",
             "location":"North access","description":"Inspection required."}
        """.trimIndent())
        assertEquals(expected, actual)
    }

    @Test
    fun existingIssueResponseStillDeserializes() {
        val response = Gson().fromJson("""
            {"success":true,"message":"Issue reported successfully.",
             "data":{"id":"issue-1","project_code":"PROJECT-1","title":"Site safety concern",
             "category":"Safety Hazard","priority":"Medium","location":"North access",
             "description":"Inspection required.","status":"open"}}
        """.trimIndent(), CreateIssueResponse::class.java)
        assertTrue(response.success)
        assertEquals("issue-1", response.data?.id)
        assertEquals("PROJECT-1", response.data?.project_code)
        assertEquals("Inspection required.", response.data?.description)
        assertTrue(response.data?.images.orEmpty().isEmpty())
    }

    @Test
    fun multipartPreservesAllExistingFieldsAndOmitsNullOptionalValues() {
        val request = CreateIssueRequest("Title", "Safety Hazard", "Medium", "North access", "Description", "engineer-id")
        assertEquals(mapOf(
            "title" to "Title", "category" to "Safety Hazard", "priority" to "Medium",
            "location" to "North access", "description" to "Description", "assigned_to" to "engineer-id"
        ), request.toMultipartFields().mapValues { (_, value) -> bodyText(value) })
        assertEquals(setOf("title", "category", "priority", "description"),
            request.copy(location = null, assigned_to = null).toMultipartFields().keys)
    }

    @Test
    fun multipartContainsEachSelectedImageUsingExactImagesField() {
        val fields = CreateIssueRequest("Title", "Safety Hazard", "Medium", null, "Description").toMultipartFields()
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        fields.forEach { (name, body) -> builder.addFormDataPart(name, null, body) }
        val first = byteArrayOf(0, 1, 2, 3)
        val second = byteArrayOf(4, 5, 6, 7)
        builder.addPart(MultipartBody.Part.createFormData("images", "first.jpg", first.toRequestBody("image/jpeg".toMediaType())))
        builder.addPart(MultipartBody.Part.createFormData("images", "second.png", second.toRequestBody("image/png".toMediaType())))
        val body = builder.build()
        assertEquals(6, body.parts.size)
        val images = body.parts.takeLast(2)
        assertEquals("form-data; name=\"images\"; filename=\"first.jpg\"", images[0].headers?.get("Content-Disposition"))
        assertEquals("form-data; name=\"images\"; filename=\"second.png\"", images[1].headers?.get("Content-Disposition"))
        org.junit.Assert.assertArrayEquals(first, Buffer().also { images[0].body.writeTo(it) }.readByteArray())
        org.junit.Assert.assertArrayEquals(second, Buffer().also { images[1].body.writeTo(it) }.readByteArray())
    }

    @Test
    fun multipartIssueWithoutImagesStillContainsRequiredFormFields() {
        val fields = CreateIssueRequest("Title", "Safety Hazard", "Medium", null, "Description").toMultipartFields()
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        fields.forEach { (name, body) -> builder.addFormDataPart(name, null, body) }
        val encoded = bodyText(builder.build())
        assertTrue(encoded.contains("name=\"title\""))
        org.junit.Assert.assertFalse(encoded.contains("name=\"images\""))
    }

    @Test
    fun parsesCreatedIssueImagesWithoutLosingOriginalAssociation() {
        val response = Gson().fromJson("""
            {"success":true,"message":"Created","data":{"id":"issue-1",
             "images":[{"url":"/uploads/issues/first.jpg","filename":"first.jpg"},
                       {"url":"/uploads/issues/second.png","filename":"second.png"}]}}
        """.trimIndent(), CreateIssueResponse::class.java)
        assertEquals("issue-1", response.data?.id)
        assertEquals(listOf(IssueImage("/uploads/issues/first.jpg", "first.jpg"),
            IssueImage("/uploads/issues/second.png", "second.png")), response.data?.images)
    }

    private fun bodyText(body: RequestBody): String = Buffer().also { body.writeTo(it) }.readUtf8()
}
