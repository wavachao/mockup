package com.wavachao.timeblock.data.model

/** A checklist line shown on the detail screen. Stored inline on its parent block. */
data class SubTask(
    val title: String,
    val done: Boolean = false,
)
