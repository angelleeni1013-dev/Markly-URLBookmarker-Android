package com.example.markly

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class BookmarkAdapter(
    private val context: Context,
    private val bookmarks: ArrayList<Bookmark>
) : BaseAdapter() {

    // Listener interfaces for actions
    interface OnBookmarkActionListener {
        fun onEdit(bookmark: Bookmark)
        fun onDelete(bookmark: Bookmark)
    }

    private var actionListener: OnBookmarkActionListener? = null

    fun setOnBookmarkActionListener(listener: OnBookmarkActionListener) {
        this.actionListener = listener
    }

    private class ViewHolder(view: View) {
        val tvTitle: TextView = view.findViewById(R.id.tvBookmarkTitle)
        val tvUrl: TextView = view.findViewById(R.id.tvBookmarkUrl)
        val tvDescription: TextView = view.findViewById(R.id.tvBookmarkDescription)
        val tvCategory: TextView = view.findViewById(R.id.tvBookmarkCategory)
        val tvDate: TextView = view.findViewById(R.id.tvBookmarkDate)
        val ivCopy: ImageView = view.findViewById(R.id.ivCopy)
        val ivEdit: ImageView = view.findViewById(R.id.ivEdit)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
    }

    override fun getCount(): Int = bookmarks.size

    override fun getItem(position: Int): Bookmark = bookmarks[position]

    override fun getItemId(position: Int): Long = bookmarks[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(
                R.layout.item_bookmark,
                parent,
                false
            )
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val bookmark = bookmarks[position]

        // Set data
        holder.tvTitle.text = bookmark.title
        holder.tvUrl.text = bookmark.url
        holder.tvDescription.text = bookmark.description
        holder.tvCategory.text = bookmark.category.uppercase()
        holder.tvDate.text = bookmark.addedDate

        // Set category color
        val categoryColor = when (bookmark.category) {
            "Social Media" -> Color.parseColor("#1976D2")
            "Entertainment" -> Color.parseColor("#C2185B")
            "Games" -> Color.parseColor("#7B1FA2")
            "Programming" -> Color.parseColor("#388E3C")
            else -> Color.parseColor("#4285F4")
        }
        holder.tvCategory.setBackgroundColor(categoryColor)

        // Make URL clickable
        holder.tvUrl.setOnClickListener {
            openUrl(bookmark.url)
        }

        // Copy button click
        holder.ivCopy.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Bookmark URL", bookmark.url)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // Edit button click
        holder.ivEdit.setOnClickListener {
            actionListener?.onEdit(bookmark)
        }

        // Delete button click
        holder.ivDelete.setOnClickListener {
            actionListener?.onDelete(bookmark)
        }
        return view
    }

    private fun openUrl(url: String) {
        try {
            var validUrl = url.trim()

            // Add https:// if URL doesn't have a protocol
            if (!validUrl.startsWith("http://") && !validUrl.startsWith("https://")) {
                validUrl = "https://$validUrl"
            }

            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(validUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open URL: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}