package com.clauditor.editor

import com.clauditor.editor.ClaudeSessionEditor.Companion.enterWorktree
import com.clauditor.editor.ClaudeSessionEditor.Companion.reconcile
import com.clauditor.services.TitleStore.Entry
import kotlin.test.Test
import kotlin.test.assertEquals

/** The tab-title / session-name rules, one scenario each. */
class TitleRulesTest {
    private val inMain = Entry("Tripo model generation tool", worktree = "", worktrees = listOf(""))

    @Test fun `entering a new worktree names the session after it`() =
        assertEquals("comet-art", enterWorktree(inMain, "comet-art").title)

    @Test fun `merge and push round trips through main never retitle`() {
        var e = enterWorktree(inMain, "comet-art")
        e = enterWorktree(e, "")           // /merge steps into main
        assertEquals("comet-art", e.title)
        e = enterWorktree(e, "comet-art")  // and back
        assertEquals("comet-art", e.title)
        assertEquals(listOf("", "comet-art"), e.worktrees)
    }

    @Test fun `a second new worktree renames again`() =
        assertEquals("win-windowing", enterWorktree(enterWorktree(inMain, "comet-art"), "win-windowing").title)

    @Test fun `old entries without a worktree list count their pinned worktree as seen`() {
        val old = Entry("Comet art", worktree = "comet-art")  // worktrees null: written before the list existed
        assertEquals("Comet art", enterWorktree(enterWorktree(old, ""), "comet-art").title)
    }

    @Test fun `a user rename is never overridden by a worktree`() {
        val renamed = Entry("eidtor", worktree = "", renamed = "eidtor", worktrees = listOf(""))
        assertEquals("eidtor", enterWorktree(renamed, "comet-art").title)
    }

    @Test fun `the plugin's own rename is not taken for the user's`() {
        val named = Entry("comet-art", worktree = "comet-art", named = "comet-art", worktrees = listOf("", "comet-art"))
        val after = reconcile(named, renamedTitle = "comet-art", aiTitle = null, fallback = null)
        assertEquals(null, after.renamed)
        assertEquals("comet-art", after.title)
    }

    @Test fun `a user rename wins and locks`() {
        val named = Entry("comet-art", worktree = "comet-art", named = "comet-art")
        val after = reconcile(named, renamedTitle = "Comet redesign", aiTitle = null, fallback = null)
        assertEquals("Comet redesign", after.title)
        assertEquals("Comet redesign", after.renamed)
        assertEquals("Comet redesign", enterWorktree(after, "other-tree").title)
    }

    @Test fun `a worktree session pinned before this rule takes its worktree name once`() {
        val old = Entry("Plant race classes and concept art", worktree = "verge-concepts")
        assertEquals("verge-concepts", reconcile(old, null, null, null).title)
    }

    @Test fun `a drastic CLI title retitles only outside a worktree`() {
        assertEquals("Plant race classes and concept art",
            reconcile(inMain.copy(aiTitle = "x"), null, "Plant race classes and concept art", null).title)
        val inTree = Entry("comet-art", worktree = "comet-art", named = "comet-art")
        assertEquals("comet-art", reconcile(inTree, null, "Plant race classes and concept art", null).title)
    }

    @Test fun `a reworded CLI title keeps the title`() =
        assertEquals("Tripo model generation tool", reconcile(inMain.copy(aiTitle = "x"), null, "Tripo model generation fixes", null).title)

    @Test fun `an unfit pinned title is repaired from the fallback`() {
        val bad = Entry("Cannot build the status line: the images did not come through.", worktree = "")
        assertEquals("Tripo texture fixes for models", reconcile(bad, null, null, "Tripo texture fixes for models").title)
    }
}

class SessionNameTest {
    @Test fun `first takes the plain name`() = assertEquals("comet-art", ClaudeSessionEditor.uniqueName("comet-art", setOf("other")))
    @Test fun `second in the same worktree gets a number`() =
        assertEquals("comet-art-3", ClaudeSessionEditor.uniqueName("comet-art", setOf("comet-art", "comet-art-2")))
    @Test fun `a numbered name counts as named for its title`() {
        kotlin.test.assertTrue(ClaudeSessionEditor.isNameFor("comet-art-2", "comet-art"))
        kotlin.test.assertFalse(ClaudeSessionEditor.isNameFor("comet-art-redesign", "comet-art"))
        kotlin.test.assertFalse(ClaudeSessionEditor.isNameFor(null, "comet-art"))
    }
}

class WorktreeNameTest {
    @Test fun `the worktree folder names the session, not the branch`() =
        assertEquals("hull3d-test", ClaudeSessionEditor.worktreeOf("/Users/x/RiderProjects/EmberSkies/.claude/worktrees/hull3d-test/game"))
}
