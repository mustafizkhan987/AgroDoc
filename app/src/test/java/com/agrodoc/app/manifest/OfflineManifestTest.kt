package com.agrodoc.app.manifest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OfflineManifestTest {

    @Test
    fun offlineManifestMustNeverDeclareInternetPermission() {
        val offlineManifestFile = File("src/offline/AndroidManifest.xml")
        val mainManifestFile = File("src/main/AndroidManifest.xml")

        if (offlineManifestFile.exists()) {
            val content = offlineManifestFile.readText()
            assertFalse(
                "Offline manifest must NOT contain android.permission.INTERNET",
                content.contains("android.permission.INTERNET")
            )
        }

        if (mainManifestFile.exists()) {
            val content = mainManifestFile.readText()
            assertFalse(
                "Main manifest must NOT contain android.permission.INTERNET (must be in connected only)",
                content.contains("android.permission.INTERNET")
            )
        }
    }

    @Test
    fun connectedManifestMustDeclareInternetPermission() {
        val connectedManifestFile = File("src/connected/AndroidManifest.xml")
        if (connectedManifestFile.exists()) {
            val content = connectedManifestFile.readText()
            assertTrue(
                "Connected manifest MUST contain android.permission.INTERNET",
                content.contains("android.permission.INTERNET")
            )
        }
    }
}
