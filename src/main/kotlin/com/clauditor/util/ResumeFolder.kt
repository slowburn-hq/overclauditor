package com.clauditor.util

import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path

/**
 * Where a resumed session should start. A session filed under the main checkout may have moved
 * into one of the repo's worktrees mid-session (EnterWorktree), and Claude Code does not always
 * record that move as worktree state, so `--resume` from the main checkout put it back in main
 * (user, 2026-11-15: a session working in `hybrid-hud-mockups` came back in main after a
 * restart). The transcript's lines carry the folder each was written in: the newest one says
 * where the session was last working.
 */
object ResumeFolder {
    private const val TAIL_BYTES = 2L * 1024 * 1024
    private val CWD = Regex("\"cwd\":\"((?:[^\"\\\\]|\\\\.)*)\"")

    /** The worktree (its folder name under `.claude/worktrees`) the session was last working in, if it still exists. */
    fun lastWorktree(transcript: Path, basePath: String): String? {
        val cwd = lastCwd(transcript) ?: return null
        val name = worktreeOf(cwd, basePath) ?: return null
        return name.takeIf { Files.isDirectory(Path.of(ClaudePathEncoder.worktreeAbsolutePath(basePath, it))) }
    }

    /** The `cwd` of the newest line that has one, read from the file's tail only (transcripts reach hundreds of MB). */
    internal fun lastCwd(transcript: Path): String? = try {
        RandomAccessFile(transcript.toFile(), "r").use { f ->
            val start = maxOf(0L, f.length() - TAIL_BYTES)
            val buf = ByteArray((f.length() - start).toInt())
            f.seek(start)
            f.readFully(buf)
            CWD.findAll(String(buf, Charsets.UTF_8)).lastOrNull()?.groupValues?.get(1)?.let(::unescape)
        }
    } catch (_: Exception) { null }

    /** `<base>/.claude/worktrees/<name>[/...]` → `<name>`; anything else (main, elsewhere) → null. */
    internal fun worktreeOf(cwd: String, basePath: String): String? {
        val root = Path.of(basePath, ".claude", "worktrees").toAbsolutePath().normalize()
        val p = try { Path.of(cwd).toAbsolutePath().normalize() } catch (_: Exception) { return null }
        if (!p.startsWith(root) || p.nameCount <= root.nameCount) return null
        return p.getName(root.nameCount).toString()
    }

    private fun unescape(s: String): String =
        s.replace(Regex("\\\\(.)")) { m -> when (val c = m.groupValues[1]) { "n" -> "\n"; "t" -> "\t"; else -> c } }
}
