package com.periodtracker.ui.viewmodel

import java.io.IOException
import javax.crypto.AEADBadTagException
import org.json.JSONException

/** Never include exception messages that may contain provider URIs or user data. */
fun backupFailureMessage(error: Exception): String = when (error) {
    is AEADBadTagException -> "The passphrase is incorrect or the backup has been altered. Current records were not changed."
    is SecurityException -> "Access to the selected document was denied. Choose the file again and allow access."
    is IOException -> "The document could not be read or written. Check available storage and try again."
    is IllegalArgumentException, is JSONException -> "This backup is invalid or uses an unsupported format. Current records were not changed."
    else -> "The backup operation could not be completed. Please try again."
}
