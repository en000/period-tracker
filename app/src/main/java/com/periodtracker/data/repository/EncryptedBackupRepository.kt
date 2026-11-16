package com.periodtracker.data.repository

import android.net.Uri
import com.periodtracker.data.backup.*
import com.periodtracker.data.source.BackupDocumentDataSource
import com.periodtracker.domain.*
import com.periodtracker.domain.usecase.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class EncryptedBackupRepository(private val journal: JournalRepository,
    private val documents: BackupDocumentDataSource, private val cipher: BackupCipher = BackupCipher()) : BackupRepository {
    override suspend fun export(document: String, password: CharArray) = withContext(Dispatchers.IO) {
        try {
            val plain = JournalJson.encode(journal.snapshot())
            val encrypted = try { cipher.encrypt(plain, password) } finally { plain.fill(0) }
            try { documents.write(Uri.parse(document), encrypted) } finally { encrypted.fill(0) }
        } finally { password.fill('\u0000') }
    }
    override suspend fun preview(document: String, password: CharArray): JournalSnapshot = withContext(Dispatchers.IO) {
        try {
            val encrypted = documents.read(Uri.parse(document))
            val plain = try { cipher.decrypt(encrypted, password) } finally { encrypted.fill(0) }
            try { JournalJson.decode(plain, LocalDate.now()) } finally { plain.fill(0) }
        } finally { password.fill('\u0000') }
    }
    override suspend fun restore(snapshot: JournalSnapshot) = journal.replace(snapshot)
}
