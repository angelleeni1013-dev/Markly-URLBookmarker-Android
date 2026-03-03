package com.example.markly

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "MarklyDB"
        private const val DATABASE_VERSION = 1
        private const val TABLE = "bookmarks"
        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_URL = "url"
        private const val COL_DESC = "description"
        private const val COL_CAT = "category"
        private const val COL_DATE = "added_date"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT,
                $COL_URL TEXT,
                $COL_DESC TEXT,
                $COL_CAT TEXT,
                $COL_DATE TEXT
            )
        """
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    // Add bookmark
    fun addBookmark(bookmark: Bookmark): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, bookmark.title)
            put(COL_URL, bookmark.url)
            put(COL_DESC, bookmark.description)
            put(COL_CAT, bookmark.category)
            put(COL_DATE, bookmark.addedDate)
        }
        val id = db.insert(TABLE, null, values)
        db.close()
        return id
    }

    // Get all bookmarks - FIXED VERSION
    fun getAllBookmarks(): ArrayList<Bookmark> {
        val list = ArrayList<Bookmark>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE ORDER BY $COL_ID DESC", null)

        try {
            if (cursor.moveToFirst()) {
                do {
                    val bookmark = Bookmark(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
                        url = cursor.getString(cursor.getColumnIndexOrThrow(COL_URL)),
                        description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESC)),
                        category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT)),
                        addedDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE))
                    )
                    list.add(bookmark)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor.close()
            db.close()
        }

        return list
    }

    // Update bookmark
    fun updateBookmark(bookmark: Bookmark): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, bookmark.title)
            put(COL_URL, bookmark.url)
            put(COL_DESC, bookmark.description)
            put(COL_CAT, bookmark.category)
            put(COL_DATE, bookmark.addedDate)
        }
        val result = db.update(TABLE, values, "$COL_ID=?",
            arrayOf(bookmark.id.toString()))
        db.close()
        return result
    }

    // Delete bookmark
    fun deleteBookmark(id: Long): Int {
        val db = writableDatabase
        val result = db.delete(TABLE, "$COL_ID=?", arrayOf(id.toString()))
        db.close()
        return result
    }

    // Get bookmarks by category - NEW METHOD
    fun getBookmarksByCategory(category: String): ArrayList<Bookmark> {
        val list = ArrayList<Bookmark>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE WHERE $COL_CAT=? ORDER BY $COL_ID DESC",
            arrayOf(category)
        )

        try {
            if (cursor.moveToFirst()) {
                do {
                    val bookmark = Bookmark(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
                        url = cursor.getString(cursor.getColumnIndexOrThrow(COL_URL)),
                        description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESC)),
                        category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT)),
                        addedDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE))
                    )
                    list.add(bookmark)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor.close()
            db.close()
        }

        return list
    }

    // Count total bookmarks
    fun getBookmarkCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return count
    }

    fun debugPrintAllBookmarks() {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE", null)

        android.util.Log.d("DatabaseHelper", "==========================================")
        android.util.Log.d("DatabaseHelper", "TOTAL BOOKMARKS IN DATABASE: ${cursor.count}")
        android.util.Log.d("DatabaseHelper", "==========================================")

        if (cursor.moveToFirst()) {
            var index = 1
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE))
                val category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT))
                android.util.Log.d("DatabaseHelper", "$index. ID: $id | Title: $title | Category: $category")
                index++
            } while (cursor.moveToNext())
        } else {
            android.util.Log.d("DatabaseHelper", "DATABASE IS EMPTY!")
        }

        cursor.close()
        db.close()
        android.util.Log.d("DatabaseHelper", "==========================================")
    }

    fun deleteAllBookmarks(): Int {
        val db = this.writableDatabase
        val result = db.delete(TABLE, null, null)
        db.close()
        return result
    }
}