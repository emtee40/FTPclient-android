package de.qwerty287.ftpclient.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ConnectionDao {

    @Query("SELECT * FROM connections")
    fun getAll(): LiveData<List<Connection>>

    @Query("SELECT * FROM connections WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): Connection?

    @Query("SELECT * FROM connections")
    suspend fun getListOfAll(): List<Connection>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: Connection): Long

    @Delete
    suspend fun delete(connection: Connection)

    @Update
    suspend fun update(connection: Connection)
}
