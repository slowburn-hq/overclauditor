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

    @Test
    fun onlyImagesEscapeEverythingElseIsQuoted() {
        assertEquals("/d/a\\ b.PNG", FilePaste.pasted("/d/a b.PNG"))
        assertEquals("\"/d/pink flower 3d model.zip\"", FilePaste.pasted("/d/pink flower 3d model.zip"))
        assertEquals("\"/d/say \\\"hi\\\" \\$5.zip\"", FilePaste.pasted("/d/say \"hi\" $5.zip"))
        assertEquals("\"/d/folder\"", FilePaste.pasted("/d/folder"))
    }
}
