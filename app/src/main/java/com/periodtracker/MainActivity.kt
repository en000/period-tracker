package com.periodtracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.periodtracker.ui.JournalApp
import com.periodtracker.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    private var notificationDate by mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        val dependencies = (application as JournalApplication).container
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                JournalViewModel::class.java -> JournalViewModel(dependencies)
                BackupViewModel::class.java -> BackupViewModel(dependencies)
                else -> error("Unknown ViewModel")
            } as T
        }
        setContent {
            JournalApp(viewModel(factory = factory), viewModel(factory = factory), notificationDate) { notificationDate = null }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleIntent(intent) }
    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra("open_today", false)) {
            notificationDate = java.time.LocalDate.now().toString(); intent.removeExtra("open_today")
        }
    }
}
