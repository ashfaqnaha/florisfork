package dev.patrickgold.florisboard.ime.clipboard.provider

import kotlinx.serialization.Serializable

@Serializable
data class ClipboardFileInfo(
    val _id: String? = null,
    val displayName: String,
    val size: Long,
    val orientation: Int,
    val mimeTypes: Array<String>,
) {
    val id: Long
        get() = _id?.toLongOrNull() ?: 0L
}
