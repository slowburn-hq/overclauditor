package com.clauditor.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files

class ResumeFolderTest {
    private val base = "/Users/x/RiderProjects/Game"

    @Test
    fun worktreeOfNamesTheWorktreeFolder() {
        assertEquals("hud", ResumeFolder.worktreeOf("$base/.claude/worktrees/hud", base))
        assertEquals("hud", ResumeFolder.worktreeOf("$base/.claude/worktrees/hud/assets/art", base))
        assertNull(ResumeFolder.worktreeOf(base, base))
        assertNull(ResumeFolder.worktreeOf("$base/.claude/worktrees", base))
        assertNull(ResumeFolder.worktreeOf("/elsewhere", base))
    }

    @Test
    fun lastCwdIsTheNewestLine() {
        val f = Files.createTempFile("transcript", ".jsonl")
        Files.writeString(f, """
            {"type":"user","cwd":"$base","message":{}}
            {"type":"assistant","cwd":"$base/.claude/worktrees/hud","message":{}}
            {"type":"ai-title","aiTitle":"x"}
        """.trimIndent())
        assertEquals("$base/.claude/worktrees/hud", ResumeFolder.lastCwd(f))
        Files.delete(f)
    }

    @Test
    fun aSessionBackInMainStaysInMain() {
        val f = Files.createTempFile("transcript", ".jsonl")
        Files.writeString(f, """
            {"type":"user","cwd":"$base/.claude/worktrees/hud"}
            {"type":"user","cwd":"$base"}
        """.trimIndent())
        assertNull(ResumeFolder.lastWorktree(f, base))
        Files.delete(f)
    }

    @Test
    fun aWorktreeThatIsGoneIsNotUsed() {
        val f = Files.createTempFile("transcript", ".jsonl")
        Files.writeString(f, """{"type":"user","cwd":"$base/.claude/worktrees/hud"}""")
        assertNull(ResumeFolder.lastWorktree(f, base))   // the folder does not exist
        Files.delete(f)
    }

    @Test
    fun anExistingWorktreeIsUsed() {
        val root = Files.createTempDirectory("repo")
        Files.createDirectories(root.resolve(".claude/worktrees/hud"))
        val f = Files.createTempFile("transcript", ".jsonl")
        Files.writeString(f, """{"type":"user","cwd":"$root/.claude/worktrees/hud/sub"}""")
        assertEquals("hud", ResumeFolder.lastWorktree(f, root.toString()))
        Files.delete(f)
    }
}
