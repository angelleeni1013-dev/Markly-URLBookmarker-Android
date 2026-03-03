package com.example.markly

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var listView: ListView
    private lateinit var spinnerFilter: Spinner
    private lateinit var etSearch: EditText
    private lateinit var adapter: BookmarkAdapter
    private lateinit var bookmarkList: ArrayList<Bookmark>
    private lateinit var filteredList: ArrayList<Bookmark>
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sharedPreferences: SharedPreferences
    private var currentFilter = "All"
    private var currentSearchQuery = ""

    companion object {
        const val PREFS_NAME = "MarklyPrefs"
        const val PREF_SPLASH_ENABLED = "splash_enabled"
        const val IMPORT_FILE_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Remove default title (we have custom layout with logo)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Init views
        listView = findViewById(R.id.lvBookmarks)
        spinnerFilter = findViewById(R.id.spinnerCategoryFilter)
        etSearch = findViewById(R.id.etSearch)
        val fab = findViewById<FloatingActionButton>(R.id.fabAddBookmark)

        // Database
        dbHelper = DatabaseHelper(this)

        // Lists
        bookmarkList = ArrayList()
        filteredList = ArrayList()

        // Adapter with action listener
        adapter = BookmarkAdapter(this, filteredList)
        adapter.setOnBookmarkActionListener(object : BookmarkAdapter.OnBookmarkActionListener {
            override fun onEdit(bookmark: Bookmark) {
                showEditBookmark(bookmark)
            }

            override fun onDelete(bookmark: Bookmark) {
                showDeleteConfirmation(bookmark)
            }
        })
        listView.adapter = adapter

        // Setup search
        setupSearch()

        // Category filter
        setupCategoryFilter()

        // Load bookmarks
        loadBookmarks()

        // FAB to Add New Bookmark - Show Bottom Sheet
        fab.setOnClickListener {
            showAddBookmark()
        }

        // Click a bookmark to view details
        listView.setOnItemClickListener { _, _, position, _ ->
            val bookmark = filteredList[position]
            showBookmarkDetailsDialog(bookmark)
        }
    }

    private var currentDialog: AlertDialog? = null

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)

        // If a dialog is showing, dismiss and reopen it
        currentDialog?.let {
            if (it.isShowing) {
                it.dismiss()
                // You can optionally reopen the dialog here
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBookmarks()
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // Force icons to show in overflow menu
        if (menu is androidx.appcompat.view.menu.MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        return true
    }

    // In MainActivity.kt, update the onOptionsItemSelected method:

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_export -> {
                exportBookmarks()
                true
            }

            R.id.menu_import -> {
                importBookmarks()
                true
            }

            R.id.menu_delete_all -> {
                showDeleteAllConfirmation()
                true
            }

            R.id.menu_splash_screen -> {
                showSplashToggleDialog()
                true
            }

            R.id.menu_about -> {
                showAboutDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun showDeleteAllConfirmation() {
        val bookmarkCount = bookmarkList.size

        if (bookmarkCount == 0) {
            Toast.makeText(this, "No bookmarks to delete", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete All Bookmarks")
            .setMessage("Are you sure you want to delete ALL $bookmarkCount bookmarks?\n\nThis action cannot be undone!")
            .setPositiveButton("Delete All") { _, _ ->
                val result = dbHelper.deleteAllBookmarks()
                if (result > 0) {
                    loadBookmarks()
                    Toast.makeText(
                        this,
                        "All bookmarks deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to delete bookmarks",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString()
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupCategoryFilter() {
        val categories = arrayOf(
            "All",
            "Social Media",
            "Entertainment",
            "Games",
            "Programming"
        )

        val spinnerAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            categories
        )
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        spinnerFilter.adapter = spinnerAdapter
        spinnerFilter.setSelection(0)

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                currentFilter = categories[position]
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun applyFilters() {
        filteredList.clear()

        var tempList = bookmarkList.toList()

        // Apply category filter
        if (currentFilter != "All") {
            tempList = tempList.filter { it.category == currentFilter }
        }

        // Apply search filter
        if (currentSearchQuery.isNotEmpty()) {
            tempList = tempList.filter { bookmark ->
                bookmark.title.contains(currentSearchQuery, ignoreCase = true) ||
                        bookmark.url.contains(currentSearchQuery, ignoreCase = true) ||
                        bookmark.description.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        filteredList.addAll(tempList)
        adapter.notifyDataSetChanged()
    }

    private fun loadBookmarks() {
        bookmarkList.clear()

        val dbBookmarks = dbHelper.getAllBookmarks()
        bookmarkList.addAll(dbBookmarks)

        applyFilters()
    }

    // ==================== ADD BOOKMARK ====================

    private fun showAddBookmark() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_bookmark, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        currentDialog = dialog

        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etUrl = dialogView.findViewById<EditText>(R.id.etUrl)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val layoutDatePicker = dialogView.findViewById<LinearLayout>(R.id.layoutDatePicker)
        val tvSelectedDate = dialogView.findViewById<TextView>(R.id.tvSelectedDate)
        val btnCreate = dialogView.findViewById<Button>(R.id.btnCreate)

        val categories = arrayOf(
            "Select Category",
            "Social Media",
            "Entertainment",
            "Games",
            "Programming"
        )
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        var selectedDate = dateFormat.format(calendar.time)
        tvSelectedDate.text = selectedDate

        layoutDatePicker.setOnClickListener {
            val datePickerDialog = android.app.DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val date = Calendar.getInstance()
                    date.set(year, month, dayOfMonth)
                    selectedDate = dateFormat.format(date.time)
                    tvSelectedDate.text = selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        btnCreate.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val url = etUrl.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val categoryPosition = spinnerCategory.selectedItemPosition

            if (title.isEmpty()) {
                etTitle.error = "Title is required"
                etTitle.requestFocus()
                return@setOnClickListener
            }

            if (url.isEmpty()) {
                etUrl.error = "URL is required"
                etUrl.requestFocus()
                return@setOnClickListener
            }

            if (!android.util.Patterns.WEB_URL.matcher(url).matches()) {
                etUrl.error = "Please enter a valid URL"
                etUrl.requestFocus()
                return@setOnClickListener
            }

            if (categoryPosition == 0) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = spinnerCategory.selectedItem.toString()

            val bookmark = Bookmark(
                title = title,
                url = url,
                description = description,
                category = category,
                addedDate = selectedDate
            )

            val result = dbHelper.addBookmark(bookmark)
            if (result > 0) {
                Toast.makeText(this, "Bookmark added successfully", Toast.LENGTH_SHORT).show()
                loadBookmarks()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Failed to add bookmark", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    // ==================== EDIT BOOKMARK ====================

    private fun showEditBookmark(bookmark: Bookmark) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_bookmark, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        currentDialog = dialog

        val etTitle = dialogView.findViewById<EditText>(R.id.etEditTitle)
        val etUrl = dialogView.findViewById<EditText>(R.id.etEditUrl)
        val etDescription = dialogView.findViewById<EditText>(R.id.etEditDescription)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerEditCategory)
        val layoutDatePicker = dialogView.findViewById<LinearLayout>(R.id.layoutEditDatePicker)
        val tvSelectedDate = dialogView.findViewById<TextView>(R.id.tvEditSelectedDate)
        val btnUpdate = dialogView.findViewById<Button>(R.id.btnUpdate)

        etTitle.setText(bookmark.title)
        etUrl.setText(bookmark.url)
        etDescription.setText(bookmark.description)
        tvSelectedDate.text = bookmark.addedDate

        val categories = arrayOf(
            "Select Category",
            "Social Media",
            "Entertainment",
            "Games",
            "Programming"
        )
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        val categoryIndex = categories.indexOf(bookmark.category)
        if (categoryIndex >= 0) {
            spinnerCategory.setSelection(categoryIndex)
        }

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        var selectedDate = bookmark.addedDate

        layoutDatePicker.setOnClickListener {
            val datePickerDialog = android.app.DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val date = Calendar.getInstance()
                    date.set(year, month, dayOfMonth)
                    selectedDate = dateFormat.format(date.time)
                    tvSelectedDate.text = selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        btnUpdate.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val url = etUrl.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val categoryPosition = spinnerCategory.selectedItemPosition

            if (title.isEmpty()) {
                etTitle.error = "Title is required"
                etTitle.requestFocus()
                return@setOnClickListener
            }

            if (url.isEmpty()) {
                etUrl.error = "URL is required"
                etUrl.requestFocus()
                return@setOnClickListener
            }

            if (!android.util.Patterns.WEB_URL.matcher(url).matches()) {
                etUrl.error = "Please enter a valid URL"
                etUrl.requestFocus()
                return@setOnClickListener
            }

            if (categoryPosition == 0) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = spinnerCategory.selectedItem.toString()

            val updatedBookmark = bookmark.copy(
                title = title,
                url = url,
                description = description,
                category = category,
                addedDate = selectedDate
            )

            val result = dbHelper.updateBookmark(updatedBookmark)
            if (result > 0) {
                Toast.makeText(this, "Bookmark updated successfully", Toast.LENGTH_SHORT).show()
                loadBookmarks()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Failed to update bookmark", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    // ==================== EXPORT & IMPORT ====================

    private fun exportBookmarks() {
        try {
            val bookmarks = dbHelper.getAllBookmarks()

            if (bookmarks.isEmpty()) {
                Toast.makeText(this, "No bookmarks to export", Toast.LENGTH_SHORT).show()
                return
            }

            val jsonArray = JSONArray()
            bookmarks.forEach { bookmark ->
                val jsonObject = JSONObject().apply {
                    put("title", bookmark.title)
                    put("url", bookmark.url)
                    put("description", bookmark.description)
                    put("category", bookmark.category)
                    put("addedDate", bookmark.addedDate)
                }
                jsonArray.put(jsonObject)
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "markly_backup_$timestamp.json"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // API 29+ : Use MediaStore Downloads
                val contentValues = ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = contentResolver
                val uri = resolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(jsonArray.toString(2).toByteArray())
                    }

                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)

                    Toast.makeText(
                        this,
                        "Exported to Downloads/$fileName\n${bookmarks.size} bookmarks saved",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                // API 26-28 : Use legacy external storage
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val exportFile = File(downloadsDir, fileName)

                FileWriter(exportFile).use { writer ->
                    writer.write(jsonArray.toString(2))
                }

                Toast.makeText(
                    this,
                    "Exported to Downloads/$fileName\n${bookmarks.size} bookmarks saved",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun importBookmarks() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, IMPORT_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMPORT_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                importFromUri(uri)
            }
        }
    }

    private fun importFromUri(uri: android.net.Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: return

            val jsonArray = JSONArray(jsonString)

            AlertDialog.Builder(this)
                .setTitle("Import Bookmarks")
                .setMessage("This will add ${jsonArray.length()} bookmarks.\n\nExisting bookmarks will not be deleted.\n\nContinue?")
                .setPositiveButton("Import") { _, _ ->
                    var successCount = 0

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val bookmark = Bookmark(
                            title = jsonObject.getString("title"),
                            url = jsonObject.getString("url"),
                            description = jsonObject.optString("description", ""),
                            category = jsonObject.getString("category"),
                            addedDate = jsonObject.getString("addedDate")
                        )

                        val result = dbHelper.addBookmark(bookmark)
                        if (result > 0) successCount++
                    }

                    loadBookmarks()
                    Toast.makeText(
                        this,
                        "Imported $successCount bookmarks successfully",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .setNegativeButton("Cancel", null)
                .show()

        } catch (e: Exception) {
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    // ==================== DIALOGS ====================
    private fun showBookmarkDetailsDialog(bookmark: Bookmark) {
        val message = """
        Title: ${bookmark.title}
        URL: ${bookmark.url}
        Description: ${bookmark.description}
        Category: ${bookmark.category}
        Date: ${bookmark.addedDate}
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Bookmark Details")
            .setMessage(message)
            .setPositiveButton("Edit") { _, _ ->
                showEditBookmark(bookmark)
            }
            .setNegativeButton("Close", null)
            .setNeutralButton("Delete") { _, _ ->
                showDeleteConfirmation(bookmark)
            }
            .show()
    }

    private fun openUrlInBrowser(url: String) {
        try {
            var validUrl = url.trim()

            // Add https:// if URL doesn't have a protocol
            if (!validUrl.startsWith("http://") && !validUrl.startsWith("https://")) {
                validUrl = "https://$validUrl"
            }

            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(validUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open URL: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation(bookmark: Bookmark) {
        AlertDialog.Builder(this)
            .setTitle("Delete Bookmark")
            .setMessage("Are you sure you want to delete '${bookmark.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                dbHelper.deleteBookmark(bookmark.id)
                loadBookmarks()
                Toast.makeText(this, "Bookmark deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAboutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSplashToggleDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_splash_toggle, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val radioEnabled = dialogView.findViewById<RadioButton>(R.id.radioEnabled)
        val radioDisabled = dialogView.findViewById<RadioButton>(R.id.radioDisabled)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        val isEnabled = sharedPreferences.getBoolean(PREF_SPLASH_ENABLED, true)
        radioEnabled.isChecked = isEnabled
        radioDisabled.isChecked = !isEnabled

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val newValue = radioEnabled.isChecked

            sharedPreferences.edit {
                putBoolean(PREF_SPLASH_ENABLED, newValue)
            }

            val message = if (newValue) {
                "Splash screen enabled"
            } else {
                "Splash screen disabled"
            }

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}



