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

    /** Set when this project starts closing: its tabs close next, and they were open at the close. */
    @Volatile var projectClosing = false

    /**
     * Drop a session from the reopen list: only when the USER closed its tab (the X, Cmd+W, a
     * Close menu item), i.e. the close runs inside a mouse or key event. Tabs closed by a plugin
     * update ([unloading]), Rider quitting or the project closing stay listed and reopen.
     */
    fun remove(sessionId: String) {
        if (unloading || projectClosing || project.isDisposed) return
        if (ApplicationManager.getApplication().isExitInProgress) return
        if (com.intellij.ide.IdeEventQueue.getInstance().trueCurrentEvent !is java.awt.event.InputEvent) return
        myState.sessionIds.remove(sessionId)
    }

    fun getAll(): List<String> = myState.sessionIds.toList()

    /** Marks [OpenSessionsPersistence.projectClosing] before the project's tabs close. */
    class OnProjectClose : com.intellij.openapi.project.ProjectCloseListener {
        override fun projectClosing(project: Project) {
            project.getServiceIfCreated(OpenSessionsPersistence::class.java)?.projectClosing = true
        }
    }

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
