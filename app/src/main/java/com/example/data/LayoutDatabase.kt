package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "custom_layouts")
data class CustomLayoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val gameTitle: String,
    val keySpacingDp: Float = 4f,
    val keyScale: Float = 1f,
    val isDefault: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "custom_keys",
    foreignKeys = [
        ForeignKey(
            entity = CustomLayoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["layoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("layoutId")]
)
data class CustomKeyEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0L,
    val layoutId: Long,
    val keyId: String,
    val label: String,
    val hidUsageCode: Int,
    val row: Int,
    val column: Int,
    val widthWeight: Float = 1f,
    val heightScale: Float = 1f,
    val isModifier: Boolean = false,
    val modifierMask: Int = 0,
    val isFnKey: Boolean = false,
    val isCapsLock: Boolean = false,
    val isCustomAdded: Boolean = false,
    val localTapXPercent: Float = 0.5f,
    val localTapYPercent: Float = 0.5f
)

@Dao
interface LayoutDao {
    @Query("SELECT * FROM custom_layouts ORDER BY isDefault DESC, updatedAt DESC")
    fun observeAllLayouts(): Flow<List<CustomLayoutEntity>>

    @Query("SELECT * FROM custom_layouts WHERE id = :layoutId LIMIT 1")
    suspend fun getLayoutById(layoutId: Long): CustomLayoutEntity?

    @Query("SELECT * FROM custom_keys WHERE layoutId = :layoutId ORDER BY row ASC, column ASC")
    fun observeKeysForLayout(layoutId: Long): Flow<List<CustomKeyEntity>>

    @Query("SELECT * FROM custom_keys WHERE layoutId = :layoutId ORDER BY row ASC, column ASC")
    suspend fun getKeysForLayout(layoutId: Long): List<CustomKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayout(layout: CustomLayoutEntity): Long

    @Update
    suspend fun updateLayout(layout: CustomLayoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeys(keys: List<CustomKeyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: CustomKeyEntity): Long

    @Update
    suspend fun updateKey(key: CustomKeyEntity)

    @Query("DELETE FROM custom_keys WHERE layoutId = :layoutId")
    suspend fun deleteKeysForLayout(layoutId: Long)

    @Query("DELETE FROM custom_keys WHERE layoutId = :layoutId AND keyId = :keyId")
    suspend fun deleteKeyByKeyId(layoutId: Long, keyId: String)

    @Query("DELETE FROM custom_layouts WHERE id = :layoutId AND isDefault = 0")
    suspend fun deleteCustomLayout(layoutId: Long)

    @Transaction
    suspend fun replaceLayoutKeys(layoutId: Long, keys: List<CustomKeyEntity>) {
        deleteKeysForLayout(layoutId)
        insertKeys(keys)
    }
}

@Database(
    entities = [CustomLayoutEntity::class, CustomKeyEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SounceDatabase : RoomDatabase() {
    abstract fun layoutDao(): LayoutDao

    companion object {
        @Volatile
        private var INSTANCE: SounceDatabase? = null

        fun getInstance(context: Context): SounceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SounceDatabase::class.java,
                    "sounce_controller_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
