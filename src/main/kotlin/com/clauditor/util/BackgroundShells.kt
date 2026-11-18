package com.clauditor.util

/**
 * Shells Claude Code has running under a session: every Bash command it starts (foreground,
 * `run_in_background`, a subagent's) is a child shell that sources its shell snapshot first.
 * The MCP servers beside them never do. Claude Code sends no hook when a background command
 * ends, and the status line doesn't report them, so the process tree is the record.
 */
object BackgroundShells {
    private val SNAPSHOT = Regex("""[\\/]shell-snapshots[\\/]snapshot-""")

    internal fun isClaudeShell(commandLine: String?): Boolean = commandLine != null && SNAPSHOT.containsMatchIn(commandLine)

    /**
     * The shells under [root] Claude Code started (a shell inside one is not counted again),
     * leaving out those whose command contains one of [waiters] (config `waiterCommands`).
     */
    fun count(root: ProcessHandle, waiters: List<String> = emptyList()): Int = try {
        root.descendants().filter { p ->
            val cmd = p.info().commandLine().orElse(null)
            isClaudeShell(cmd) && waiters.none { cmd!!.contains(it) } &&
                p.parent().map { !isClaudeShell(it.info().commandLine().orElse(null)) }.orElse(true)
        }.count().toInt()
    } catch (_: Exception) { 0 }
}
