package dev.jdtech.jellyfin.work

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDataSyncTest {

    @Test
    fun `offline to online triggers sync`() {
        assertTrue(UserDataSync.shouldSyncOnTransition(previousEffectiveOffline = true, newEffectiveOffline = false))
    }

    @Test
    fun `staying offline does not trigger sync`() {
        assertFalse(UserDataSync.shouldSyncOnTransition(previousEffectiveOffline = true, newEffectiveOffline = true))
    }

    @Test
    fun `staying online does not trigger sync`() {
        assertFalse(UserDataSync.shouldSyncOnTransition(previousEffectiveOffline = false, newEffectiveOffline = false))
    }

    @Test
    fun `online to offline does not trigger sync`() {
        assertFalse(UserDataSync.shouldSyncOnTransition(previousEffectiveOffline = false, newEffectiveOffline = true))
    }
}
