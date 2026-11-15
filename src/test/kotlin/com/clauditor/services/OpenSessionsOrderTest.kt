package com.clauditor.services

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenSessionsOrderTest {
    @Test fun followsTabOrder() =
        assertEquals(listOf("c", "a", "b", "z"), OpenSessionsPersistence.ordered(listOf("a", "b", "z", "c"), listOf("c", "a", "b")))
}
