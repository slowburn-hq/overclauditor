package com.clauditor.services

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs the real hook script with the inputs Claude Code 2.1.326 sends (recorded with a dump hook):
 * a subagent's hooks carry its agent_id, the main agent's don't.
 */
class NotifyScriptTest {
    private val dir: Path = Files.createTempDirectory("notify")
    private val notify: Path = dir.resolve("clauditor-notify-s")
    private val script: Path = dir.resolve("n.sh").also { Files.writeString(it, ClaudeStatusService.notifyScriptText()) }

    private fun hook(json: String): String {
        val gitBash = java.io.File("C:\\Program Files\\Git\\bin\\bash.exe")
        val bash = if (com.clauditor.util.ProcessHelper.IS_WINDOWS && gitBash.isFile) gitBash.path else "bash"
        val pb = ProcessBuilder(bash, script.toString().replace('\\', '/'))
        pb.environment()["CLAUDITOR_NOTIFY_FILE"] = notify.toString().replace('\\', '/')
        val p = pb.start()
        p.outputStream.use { it.write(json.toByteArray()) }
        val out = p.inputStream.bufferedReader().readText()
        assertTrue(p.waitFor(10, TimeUnit.SECONDS))
        assertEquals(0, p.exitValue())
        assertEquals("", out, "a hook's stdout goes into Claude's context: it must print nothing")
        return state()
    }

    private fun state() = if (Files.exists(notify)) Files.readString(notify).trim() else ""
    private fun agents() = dir.resolve("clauditor-notify-s.agents").toFile().list()?.size ?: 0

    @Test
    fun `a turn with a background agent`() {
        assertEquals("clear", hook("""{"hook_event_name":"UserPromptSubmit","prompt":"x"}"""))
        assertEquals("tool:Agent", hook("""{"hook_event_name":"PreToolUse","tool_name":"Agent"}"""))
        hook("""{"hook_event_name":"SubagentStart","agent_id":"a1","agent_type":"general-purpose"}""")
        assertEquals(1, agents())
        // the subagent's own tools leave the main agent's state alone
        assertEquals("tool:Agent", hook("""{"hook_event_name":"PreToolUse","agent_id":"a1","tool_name":"Bash"}"""))
        assertEquals("tool:Agent", hook("""{"hook_event_name":"PostToolUse","agent_id":"a1","tool_name":"Bash"}"""))
        assertEquals("clear", hook("""{"hook_event_name":"PostToolUse","tool_name":"Agent"}"""))
        // the main turn ends while the agent works on: done, one agent running (the tab shows ◐)
        assertEquals("done", hook("""{"hook_event_name":"Stop","stop_hook_active":false}"""))
        assertEquals(1, agents())
        hook("""{"hook_event_name":"SubagentStop","agent_id":"a1","agent_type":"general-purpose"}""")
        assertEquals(0, agents())
        assertEquals("done", state())
    }

    @Test
    fun `a subagent's tool does not clear a permission prompt`() {
        hook("""{"hook_event_name":"Notification","notification_type":"permission_prompt"}""")
        assertEquals("permission_prompt", hook("""{"hook_event_name":"PostToolUse","agent_id":"a2","tool_name":"Read"}"""))
        assertEquals("tool:Bash", hook("""{"hook_event_name":"PreToolUse","tool_name":"Bash"}"""))
    }

    @Test
    fun `waiting after done replaces it`() {
        hook("""{"hook_event_name":"Stop"}""")
        assertEquals("idle_prompt", hook("""{"hook_event_name":"Notification","notification_type":"idle_prompt"}"""))
    }
}
