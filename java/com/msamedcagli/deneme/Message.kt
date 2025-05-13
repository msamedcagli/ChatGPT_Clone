package com.msamedcagli.deneme

import android.net.Uri

data class Message(
    val role: String,
    val content: String,
    val imageUri: Uri? = null
)
