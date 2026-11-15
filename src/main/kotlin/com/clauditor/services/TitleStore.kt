package com.clauditor.services

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

/**
 * The title each session's tab and list row show, pinned so it changes only on a few events
 * (see ClaudeSessionEditor's title section): a worktree the session hasn't been in before, the
 * first file write, an explicit /rename, or a CLI title for clearly different work. Kept in the
 * IDE's system folder (OcPaths).
 */
object TitleStore {
    data class Entry(
        val title: String,
        /** Worktree the session was in when pinned ("" for the main checkout). */
        val worktree: String = "",
        /** The session has written or edited a file since the title was first pinned. */
        val workStarted: Boolean = false,
        /** The CLI's ai-title at pin time, so a NEW one from the CLI can be noticed. */
        val aiTitle: String? = null,
        /** The /rename title at pin time, likewise. */
        val renamed: String? = null,
        /** Every worktree the session has been in: going back to one (or to main) never retitles. Null in old entries. */
        val worktrees: List<String>? = null,
        /** The name the plugin last gave Claude itself (`/rename`): the session's cross-session address. Not a user rename. */
        val named: String? = null
    )

    private val file: Path by lazy { com.clauditor.util.OcPaths.dataDir.resolve("titles.json") }
    private val gson = Gson()
    private val entries: MutableMap<String, Entry> by lazy {
        val m = ConcurrentHashMap<String, Entry>()
        try {
            val type = object : TypeToken<Map<String, Entry>>() {}.type
            Files.newBufferedReader(file).use { gson.fromJson<Map<String, Entry>>(it, type) }?.let { m.putAll(it) }
        } catch (_: Exception) {}
        // Pinned before worktree names were titles: a session in a worktree is named after it, so
        // closed sessions read that way in the list and Resume menu too (open tabs did already).
        for ((id, e) in m) if (e.renamed == null && e.named == null && !e.worktree.isNullOrEmpty() && e.title != e.worktree) {
            m[id] = e.copy(title = e.worktree)
        }
        m
    }

    fun get(sessionId: String): Entry? = entries[sessionId]

    @Synchronized
    fun put(sessionId: String, entry: Entry) {
        if (entries[sessionId] == entry) return
        entries[sessionId] = entry
        try {
            Files.createDirectories(file.parent)
            val tmp = file.resolveSibling("titles.json.tmp")
            Files.newBufferedWriter(tmp).use { gson.toJson(HashMap(entries), it) }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {}
    }
}
