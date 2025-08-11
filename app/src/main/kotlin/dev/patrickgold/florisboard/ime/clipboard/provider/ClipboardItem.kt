package dev.patrickgold.florisboard.ime.clipboard.provider

import android.content.ClipData
import android.content.ClipDescription
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.UriSerializer
import org.florisboard.lib.android.stringRes
import org.florisboard.lib.kotlin.tryOrNull

enum class ItemType(val value: Int) {
    TEXT(1),
    IMAGE(2),
    VIDEO(3);

    companion object {
        fun fromInt(value : Int) : ItemType {
            return entries.first { it.value == value }
        }
    }
}

@Serializable
data class ClipboardItem @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("_id")
    val id: String? = null,
    val type: ItemType,
    val text: String?,
    @Serializable(with = UriSerializer::class)
    val uri: Uri?,
    val creationTimestampMs: Long,
    val isPinned: Boolean,
    val mimeTypes: Array<String>,
    @EncodeDefault
    val isSensitive: Boolean = false,
    @EncodeDefault
    val isRemoteDevice: Boolean = false,
) {
    companion object {
        private val TEXT_PLAIN = arrayOf("text/plain")
        private val MEDIA_PROJECTION = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)

        const val FLORIS_CLIP_LABEL = "florisboard/clipboard_item"

        fun text(text: String): ClipboardItem {
            return ClipboardItem(
                type = ItemType.TEXT,
                text = text,
                uri = null,
                creationTimestampMs = System.currentTimeMillis(),
                isPinned = false,
                mimeTypes = TEXT_PLAIN,
            )
        }

        fun fromClipData(context: Context, data: ClipData, cloneUri: Boolean) : ClipboardItem {
            val dataItem = data.getItemAt(0)
            val type = when {
                dataItem?.uri != null && data.description.hasMimeType("image/*") -> ItemType.IMAGE
                dataItem?.uri != null && data.description.hasMimeType("video/*") -> ItemType.VIDEO
                else -> ItemType.TEXT
            }

            val isSensitive = if (AndroidVersion.ATLEAST_API33_T) {
                data.description?.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) ?: false
            } else {
                false
            }

            val isRemoteDevice = if (AndroidVersion.ATLEAST_API34_U) {
                data.description?.extras?.getBoolean(ClipDescription.EXTRA_IS_REMOTE_DEVICE) ?: false
            } else {
                false
            }

            val uri = if (type == ItemType.IMAGE || type == ItemType.VIDEO) {
                dataItem.uri
            } else { null }

            val text = dataItem.text?.toString()
            val mimeTypes = when (type) {
                ItemType.TEXT -> TEXT_PLAIN
                ItemType.IMAGE, ItemType.VIDEO -> {
                    Array(data.description.mimeTypeCount) { data.description.getMimeType(it) }
                }
            }

            return ClipboardItem(null, type, text, uri, System.currentTimeMillis(), false, mimeTypes, isSensitive, isRemoteDevice)
        }
    }

    @Composable
    inline fun displayText(): String {
        val context = LocalContext.current
        return displayText(context)
    }

    fun displayText(context: Context): String {
        return if (isSensitive) {
            context.stringRes(R.string.clipboard__sensitive_clip_content)
        } else {
            stringRepresentation()
        }
    }

    infix fun isEqualTo(other: ClipData?): Boolean {
        if (other == null) return false
        return when (type) {
            ItemType.TEXT -> text == other.getItemAt(0).text
            ItemType.IMAGE, ItemType.VIDEO -> uri == other.getItemAt(0).uri
        }
    }

    fun toClipData(context: Context): ClipData {
        return when (type) {
            ItemType.TEXT -> {
                ClipData.newPlainText(FLORIS_CLIP_LABEL, text)
            }
            ItemType.IMAGE, ItemType.VIDEO -> {
                ClipData.newUri(context.contentResolver, FLORIS_CLIP_LABEL, uri)
            }
        }
    }

    fun close(context: Context) {
        if (type == ItemType.IMAGE) {
            tryOrNull { context.contentResolver.delete(this.uri!!, null, null) }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClipboardItem

        if (id != other.id) return false
        if (type != other.type) return false
        if (text != other.text) return false
        if (uri != other.uri) return false
        if (creationTimestampMs != other.creationTimestampMs) return false
        if (!mimeTypes.contentEquals(other.mimeTypes)) return false
        if (isSensitive != other.isSensitive) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + type.hashCode()
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (uri?.hashCode() ?: 0)
        result = 31 * result + creationTimestampMs.hashCode()
        result = 31 * result + mimeTypes.contentHashCode()
        result = 31 * result + isSensitive.hashCode()
        return result
    }

    fun stringRepresentation(): String {
        return when {
            text != null -> text
            uri != null -> "(Image) $uri"
            else -> "#ERROR"
        }
    }
}
