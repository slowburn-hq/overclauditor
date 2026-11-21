package com.clauditor.util

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserModelDefaultTest {
    private val settings = """
        {
          "env": { "model": "inner" },
          "model": "sonnet",
          "modelSettings": { "opus": { "model": "x" } },
          "theme": "dark"
        }
    """.trimIndent() + "\n"

    @Test
    fun `the top-level model line is the only one changed`() {
        val out = UserModelDefault.withModel(settings, "opus")!!
        assertEquals(settings.replace("\"model\": \"sonnet\"", "\"model\": \"opus\""), out)
    }

    @Test
    fun `no saved default removes the line`() {
        val out = UserModelDefault.withModel(settings, null)!!
        assertNull(UserModelDefault.topLevelModel(out))
        assertEquals("dark", com.google.gson.JsonParser.parseString(out).asJsonObject.get("theme").asString)
        assertEquals("inner", com.google.gson.JsonParser.parseString(out).asJsonObject.getAsJsonObject("env").get("model").asString)
    }

    @Test
    fun `the last key removed takes the comma before it`() {
        val text = "{\n  \"theme\": \"dark\",\n  \"model\": \"sonnet\"\n}\n"
        assertEquals("{\n  \"theme\": \"dark\"\n}\n", UserModelDefault.withModel(text, null))
    }

    @Test
    fun `a file with no top-level model is left alone`() {
        assertNull(UserModelDefault.withModel("{ \"env\": { \"model\": \"x\" } }", "opus"))
    }

    @Test
    fun `restore writes the file back`() {
        val f = Files.createTempFile("settings", ".json")
        Files.writeString(f, settings.replace("sonnet", "haiku"))
        assertTrue(UserModelDefault.restore("sonnet", f))
        assertEquals(settings, Files.readString(f))
        assertEquals("sonnet", UserModelDefault.read(f))
        Files.delete(f)
    }
}
