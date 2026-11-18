package com.clauditor.toolwindow

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class MessageFilterTest {
    @Test
    fun taggedMessagesAreHarnessRecords() {
        assertTrue(MessageHistoryPanel.isTagged("<task-notification>done</task-notification>"))
        assertTrue(MessageHistoryPanel.isTagged("  \n<command-name>/merge</command-name>"))
        assertFalse(MessageHistoryPanel.isTagged("fix the <div> layout"))
        assertFalse(MessageHistoryPanel.isTagged("merge it"))
    }
}
