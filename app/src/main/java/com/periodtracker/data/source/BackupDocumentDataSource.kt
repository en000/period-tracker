package com.periodtracker.data.source

import android.content.ContentResolver
import android.net.Uri
import com.periodtracker.data.backup.BackupCipher
import java.io.ByteArrayOutputStream

class BackupDocumentDataSource(private val resolver: ContentResolver) {
    fun read(uri: Uri): ByteArray = requireNotNull(resolver.openInputStream(uri)) { "Cannot open this document." }.use { input ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(out.size() + count <= BackupCipher.MAX_BYTES) { "Backup file is too large." }
            out.write(buffer, 0, count)
        }
        out.toByteArray()
    }
    fun write(uri: Uri, bytes: ByteArray) {
        requireNotNull(resolver.openOutputStream(uri, "wt")) { "Cannot write this document." }.use {
            it.write(bytes); it.flush()
        }
    }
}
