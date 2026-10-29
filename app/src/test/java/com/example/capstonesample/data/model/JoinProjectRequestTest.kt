package com.example.capstonesample.data.model

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class JoinProjectRequestTest {
    @Test
    fun serializesExactInviteAcceptanceContractWithoutGlobalRole() {
        val request = JoinProjectRequest(
            inviteCode = "E9F7-DEB3",
            position = "Site Engineer"
        )

        val actual = JsonParser.parseString(Gson().toJson(request))
        val expected = JsonParser.parseString(
            """{"invite_code":"E9F7-DEB3","position":"Site Engineer"}"""
        )

        assertEquals(expected, actual)
    }

    @Test
    fun preservesEngineerEnteredFreeTextAndKeepsSeparateInviteValues() {
        val gson = Gson()
        val first = JoinProjectRequest("FIRST-CODE", "QA/QC Engineer")
        val second = JoinProjectRequest("SECOND-CODE", "Structural Inspection Engineer")

        val firstJson = JsonParser.parseString(gson.toJson(first)).asJsonObject
        val secondJson = JsonParser.parseString(gson.toJson(second)).asJsonObject

        assertEquals("FIRST-CODE", firstJson["invite_code"].asString)
        assertEquals("QA/QC Engineer", firstJson["position"].asString)
        assertEquals("SECOND-CODE", secondJson["invite_code"].asString)
        assertEquals("Structural Inspection Engineer", secondJson["position"].asString)
    }
}
