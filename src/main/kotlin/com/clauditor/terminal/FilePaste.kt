package com.clauditor.terminal

import com.intellij.openapi.Disposable
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.terminal.JBTerminalPanel
import com.intellij.terminal.JBTerminalSystemSettingsProviderBase
import com.intellij.terminal.JBTerminalWidget
import com.jediterm.terminal.TerminalCopyPasteHandler
import com.jediterm.terminal.model.StyleState
import com.jediterm.terminal.model.TerminalTextBuffer
import com.jediterm.terminal.ui.settings.SettingsProvider
import java.awt.datatransfer.DataFlavor
import java.io.File

/**
 * Files copied in Finder paste as their PATHS, the way a drag into iTerm types them, so Claude
 * Code attaches each image (`[Image #1] [Image #2]`). The terminal's own paste took the
 * clipboard's text, which for copied files is only their names.
 * Only IMAGES keep the escaped form Claude Code attaches; any other file (a zip, a model) pastes
 * in double quotes, which reads as the path it is (user, 2026-11-18).
 */
object FilePaste {
    private const val SPECIAL = " \\'\"()[]{}&;|<>*?$`!#~"

    /** A path as a shell types it: every special character backslash-escaped. */
    fun escape(path: String): String = buildString {
        for (c in path) { if (c in SPECIAL) append('\\'); append(c) }
    }

    private val IMAGES = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "tif", "tiff", "heic")

    /** A path in double quotes, with what a shell still reads inside them escaped. */
    fun quote(path: String): String = "\"" + path.replace(Regex("([\"\\\\$`])"), "\\\\$1") + "\""

    /** How one copied file pastes: an image escaped (so it attaches), anything else quoted. */
    fun pasted(path: String): String =
        if (path.substringAfterLast('.', "").lowercase() in IMAGES) escape(path) else quote(path)

    /** The copied files as pasted paths, space-separated; null when no files are on the clipboard. */
    fun clipboardFiles(): String? {
        val contents = CopyPasteManager.getInstance().contents ?: return null
        if (!contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return null
        val files = try {
            (contents.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)?.filterIsInstance<File>()
        } catch (_: Exception) { null }
        if (files.isNullOrEmpty()) return null
        return files.joinToString(" ") { pasted(it.absolutePath) }
    }

    fun handler(inner: TerminalCopyPasteHandler) = object : TerminalCopyPasteHandler {
        override fun setContents(text: String, useSystemSelectionClipboardIfAvailable: Boolean) =
            inner.setContents(text, useSystemSelectionClipboardIfAvailable)
        override fun getContents(useSystemSelectionClipboardIfAvailable: Boolean): String? =
            (if (!useSystemSelectionClipboardIfAvailable) clipboardFiles() else null)
                ?: inner.getContents(useSystemSelectionClipboardIfAvailable)
    }
}

/** The session terminal: a [JBTerminalWidget] whose paste carries copied files as paths ([FilePaste]). */
class SessionTerminalWidget(
    project: Project,
    settings: JBTerminalSystemSettingsProviderBase,
    parent: Disposable
) : JBTerminalWidget(project, settings, parent) {
    override fun createTerminalPanel(
        settingsProvider: SettingsProvider,
        styleState: StyleState,
        textBuffer: TerminalTextBuffer
    ): JBTerminalPanel = object : JBTerminalPanel(settingsProvider as JBTerminalSystemSettingsProviderBase, textBuffer, styleState) {
        override fun createCopyPasteHandler(): TerminalCopyPasteHandler = FilePaste.handler(super.createCopyPasteHandler())
    }
}
