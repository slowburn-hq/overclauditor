package com.clauditor.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class FilePasteTest {
    @Test
    fun escapesWhatAShellWould() {
        // a macOS screenshot name: plain spaces escaped, the narrow no-break space before PM kept
        assertEquals(
            "/Users/x/Screenshots/Screenshot\\ 2026-11-16\\ at\\ 4.40.15 PM.png",
            FilePaste.escape("/Users/x/Screenshots/Screenshot 2026-11-16 at 4.40.15 PM.png")
        )
        assertEquals("/a/it\\'s\\ \\(1\\)\\&\\$.png", FilePaste.escape("/a/it's (1)&$.png"))
        assertEquals("/plain/path.png", FilePaste.escape("/plain/path.png"))
    }
}
