package com.example.markly

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
data class Bookmark(
    var id: Long = 0,
    var title: String = "",
    var url: String = "",
    var description: String = "",
    var category: String = "",
    var addedDate: String = getCurrentDate()
) : Parcelable {
    companion object {
        fun getCurrentDate(): String {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            return dateFormat.format(Date())
        }
    }
}