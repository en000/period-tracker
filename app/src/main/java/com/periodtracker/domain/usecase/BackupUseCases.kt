package com.periodtracker.domain.usecase

import com.periodtracker.domain.*

interface BackupRepository {
    suspend fun export(document: String, password: CharArray)
    suspend fun preview(document: String, password: CharArray): JournalSnapshot
    suspend fun restore(snapshot: JournalSnapshot)
}
class ExportBackup(private val repository: BackupRepository) {
    suspend operator fun invoke(document: String, password: CharArray) = repository.export(document, password)
}
class PreviewBackup(private val repository: BackupRepository) {
    suspend operator fun invoke(document: String, password: CharArray) = repository.preview(document, password)
}
class RestoreBackup(private val repository: BackupRepository) {
    suspend operator fun invoke(snapshot: JournalSnapshot) = repository.restore(snapshot)
}
