package com.unio.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class Channel(val label: String) {
    SMS("SMS"), WHATSAPP("WhatsApp"), MESSENGER("Messenger"), FACEBOOK("Facebook")
}

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactKey: String,
    val contactName: String,
    val channel: String,
    val body: String,
    val fromMe: Boolean,
    val timestamp: Long,
    val read: Boolean
)

data class UnreadCount(val contactKey: String, val n: Int)

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE id IN (SELECT MAX(id) FROM messages GROUP BY contactKey) ORDER BY timestamp DESC")
    fun threads(): Flow<List<MessageEntity>>

    @Query("SELECT contactKey, COUNT(*) AS n FROM messages WHERE read = 0 GROUP BY contactKey")
    fun unread(): Flow<List<UnreadCount>>

    @Query("SELECT * FROM messages WHERE contactKey = :key ORDER BY timestamp ASC")
    fun thread(key: String): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE contactKey = :key AND body = :body AND timestamp > :since")
    suspend fun recentDuplicates(key: String, body: String, since: Long): Int

    @Query("UPDATE messages SET read = 1 WHERE contactKey = :key")
    suspend fun markRead(key: String)

    @Insert
    suspend fun insert(m: MessageEntity)
}

@Database(entities = [MessageEntity::class], version = 1, exportSchema = false)
abstract class UnioDb : RoomDatabase() {
    abstract fun dao(): MessageDao

    companion object {
        fun create(ctx: Context): UnioDb =
            Room.databaseBuilder(ctx, UnioDb::class.java, "unio.db").build()
    }
}
