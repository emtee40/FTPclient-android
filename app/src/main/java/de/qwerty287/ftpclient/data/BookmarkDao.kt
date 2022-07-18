package de.qwerty287.ftpclient.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks")
    fun getAll(): LiveData<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): Bookmark?

    @Query("SELECT * FROM bookmarks WHERE connection = :connectionId")
    suspend fun getAllByConnection(connectionId: Long): List<Bookmark>

    @Query("SELECT * FROM bookmarks")
    suspend fun getListOfAll(): List<Bookmark>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: Bookmark): Long

    @Delete
    suspend fun delete(bookmark: Bookmark)

    @Update
    suspend fun update(bookmark: Bookmark)
}