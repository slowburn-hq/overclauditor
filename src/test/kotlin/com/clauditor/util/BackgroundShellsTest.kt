package com.clauditor.util

import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundShellsTest {
    @Test
    fun `claude's shells are told from MCP servers`() {
        // as recorded from Claude Code 2.1.326 running `sleep 20` in the background
        assertTrue(BackgroundShells.isClaudeShell("/bin/zsh -c source /Users/x/.claude/shell-snapshots/snapshot-zsh-1795005159325-vwnegs.sh 2>/dev/null || true && eval 'sleep 20'"))
        assertTrue(BackgroundShells.isClaudeShell("""C:\Program Files\Git\bin\bash.exe -c source C:\Users\x\.claude\shell-snapshots\snapshot-bash-1.sh"""))
        assertFalse(BackgroundShells.isClaudeShell("/opt/homebrew/bin/uv tool uvx blender-mcp"))
        assertFalse(BackgroundShells.isClaudeShell("/bin/sleep 20"))
        assertFalse(BackgroundShells.isClaudeShell(null))
    }

    @Test
    fun `a running shell is counted once, and not after it ends`() {
        if (com.clauditor.util.ProcessHelper.IS_WINDOWS) return
        val snap = Files.createDirectories(Files.createTempDirectory("t").resolve("shell-snapshots")).resolve("snapshot-bash-1.sh")
        Files.writeString(snap, "")
        val me = ProcessHandle.current()
        assertEquals(0, BackgroundShells.count(me))
        // `; true` keeps bash from exec'ing sleep in its place; the nested shell must not count twice
        val p = ProcessBuilder("/bin/bash", "-c", "source $snap; /bin/bash -c 'source $snap; sleep 30; true'; true").start()
        try {
            var n = 0
            repeat(40) { if (n == 0) { Thread.sleep(100); n = BackgroundShells.count(me) } }
            assertEquals(1, n)
        } finally {
            p.toHandle().descendants().forEach { it.destroyForcibly() }
            p.destroyForcibly(); p.waitFor(5, TimeUnit.SECONDS)
        }
        Thread.sleep(300)
        assertEquals(0, BackgroundShells.count(me))
    }
}
