package com.clauditor.editor

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.RowIcon
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.Icon

class ClaudeSessionIconProvider : FileIconProvider {

    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        if (file !is ClaudeSessionVirtualFile) return null
        // Status lives in a fixed-size badge appended to the tab icon (never in the title
        // text) so the tab keeps a constant width as Claude's state cycles. The badge box
        // is always present — idle paints nothing — so even the badge column never resizes.
        val badge = badgeFor(file.statusGlyph(), error = file.errorState != null)
        // Tree: the session is working in a worktree now (an EnterWorktree mid-session counts).
        val inWorktree = file.currentWorktree?.isNotEmpty() ?: file.isWorktreeSession
        return if (inWorktree) RowIcon(CLAUDE_ICON, TAB_TREE_ICON, badge)
        else RowIcon(CLAUDE_ICON, badge)
    }

    companion object {
        private val CLAUDE_ICON = IconLoader.getIcon("/icons/claude.svg", ClaudeSessionIconProvider::class.java)
        val TREE_ICON: Icon = IconLoader.getIcon("/icons/worktree.svg", ClaudeSessionIconProvider::class.java)
        /** Half-size, 8px wide: the tab's "in a worktree" mark. */
        private val TAB_TREE_ICON: Icon = IconLoader.getIcon("/icons/worktree-tab.svg", ClaudeSessionIconProvider::class.java)

        // One shared icon instance per glyph (colors are read from the component at paint
        // time, so instances are safe to reuse across tabs). Empty string = idle placeholder.
        private val badgeCache = HashMap<String, StatusGlyphIcon>()
        private fun c(light: Int, dark: Int) = com.intellij.ui.JBColor(java.awt.Color(light), java.awt.Color(dark))
        private val ERROR_RED = c(0xD33F3F, 0xF2555A)
        /** Each state's colour; one not listed (↗ external, ○ waiting) takes the tab's text colour. */
        private val COLORS = mapOf(
            "⊘" to c(0xC75A1A, 0xF0883E),   // unresponsive: orange
            "⚠" to c(0xB88500, 0xF2C230),   // needs permission: amber
            "↻" to c(0x2F7FC1, 0x58A6FF),   // compacting: blue
            "⚙" to c(0xC2643F, 0xE0876A),   // a tool running: Claude's orange
            "●" to c(0xC2643F, 0xE0876A),   // thinking: Claude's orange
            "◐" to c(0x8250DF, 0xB392F0),   // agents still running: purple
            "✓" to c(0x2E8B3E, 0x56D364),   // done, not looked at: green
        )

        @Synchronized
        private fun badgeFor(glyph: String?, error: Boolean = false): StatusGlyphIcon =
            badgeCache.getOrPut((if (error) "!" else "") + (glyph ?: "")) {
                StatusGlyphIcon(glyph ?: "", if (error) ERROR_RED else COLORS[glyph])
            }
    }

    /** Paints a status glyph centered in a fixed 16px box; an empty glyph reserves the box but draws nothing. */
    private class StatusGlyphIcon(private val glyph: String, private val color: java.awt.Color? = null) : Icon {
        /** How much of the box the glyph's ink fills: round ones less, or a solid dot outweighs the rest. */
        private val ink = when (glyph) { "●" -> 0.6; "◐", "○" -> 0.72; else -> 0.82 }
        private val size = JBUIScale.scale(16)

        override fun getIconWidth(): Int = size
        override fun getIconHeight(): Int = size

        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            if (glyph.isEmpty()) return
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                g2.color = color ?: c?.foreground ?: UIUtil.getLabelForeground()
                // As big as the Claude icon beside it (user, 2026-11-18): scale the glyph so its INK,
                // not its font box, fills ~14 of the 16px, centred on the ink (⚙ and ● differ a lot)
                val base = (c?.font ?: UIUtil.getLabelFont()).deriveFont(size.toFloat())
                val box = base.createGlyphVector(g2.fontRenderContext, glyph).visualBounds
                val fit = if (box.width > 0 && box.height > 0) (size * ink / maxOf(box.width, box.height)).toFloat() else 1f
                g2.font = base.deriveFont(size * fit)
                val b = g2.font.createGlyphVector(g2.fontRenderContext, glyph).visualBounds
                g2.drawString(glyph, (x + (size - b.width) / 2 - b.x).toFloat(), (y + (size - b.height) / 2 - b.y).toFloat())
            } finally {
                g2.dispose()
            }
        }
    }
}
