package com.clauditor.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project

@State(
    name = "ClauditorOpenSessions",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
@Service(Service.Level.PROJECT)
class OpenSessionsPersistence(private val project: Project) : PersistentStateComponent<OpenSessionsPersistence.State> {

    class State {
        @JvmField
        var sessionIds: MutableList<String> = mutableListOf()
    }

    private var myState = State()

    /** Saved in the tabs' current order (drags included), so a restart reopens them as they were. */
    override fun getState(): State {
        val order = tabOrder()
        if (order.isNotEmpty()) myState.sessionIds = ordered(myState.sessionIds, order).toMutableList()
        return myState
    }

    private fun tabOrder(): List<String> = try {
        val ex = com.intellij.openapi.fileEditor.ex.FileEditorManagerEx.getInstanceEx(project)
        ex.windows.flatMap { w -> w.fileList }
            .filterIsInstance<com.clauditor.editor.ClaudeSessionVirtualFile>()
            .mapNotNull { it.sessionId }
    } catch (_: Exception) { emptyList() }

    override fun loadState(state: State) {
        myState = state
    }

    fun add(sessionId: String) {
        if (sessionId !in myState.sessionIds) {
            myState.sessionIds.add(sessionId)
        }
    }

    /**
     * A tab the user closed. Not one closed by a plugin update or uninstall ([unloading]) or by
     * the IDE exiting: those tabs were open at the restart and reopen after it (an update closed
     * them all, then the unload was refused and the IDE restarted with none to restore).
     */
    fun remove(sessionId: String) {
        if (unloading || ApplicationManager.getApplication().isExitInProgress || project.isDisposed) return
        myState.sessionIds.remove(sessionId)
    }

    fun getAll(): List<String> = myState.sessionIds.toList()

    companion object {
        /** Set before this plugin unloads (an update). Never cleared: the unload discards these classes, or is refused and the IDE restarts. */
        @Volatile @JvmStatic var unloading = false

        /** [ids] sorted by their position in [tabOrder]; ids with no open tab keep their place at the end. */
        internal fun ordered(ids: List<String>, tabOrder: List<String>): List<String> {
            val pos = tabOrder.withIndex().associate { it.value to it.index }
            return ids.sortedBy { pos[it] ?: Int.MAX_VALUE }
        }

        fun getInstance(project: Project): OpenSessionsPersistence =
            project.getService(OpenSessionsPersistence::class.java)
    }
}
