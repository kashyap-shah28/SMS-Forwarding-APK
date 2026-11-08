package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.FilterAction
import com.example.data.model.FilterRule
import com.example.data.model.FilterType
import com.example.data.model.ForwardLog
import com.example.data.model.LogStatus

class Converters {
    @TypeConverter
    fun fromFilterType(value: FilterType): String = value.name

    @TypeConverter
    fun toFilterType(value: String): FilterType = try {
        FilterType.valueOf(value)
    } catch (_: Exception) {
        FilterType.KEYWORD
    }

    @TypeConverter
    fun fromFilterAction(value: FilterAction): String = value.name

    @TypeConverter
    fun toFilterAction(value: String): FilterAction = try {
        FilterAction.valueOf(value)
    } catch (_: Exception) {
        FilterAction.FORWARD
    }

    @TypeConverter
    fun fromLogStatus(value: LogStatus): String = value.name

    @TypeConverter
    fun toLogStatus(value: String): LogStatus = try {
        LogStatus.valueOf(value)
    } catch (_: Exception) {
        LogStatus.PENDING
    }
}

@Database(entities = [FilterRule::class, ForwardLog::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun filterRuleDao(): FilterRuleDao
    abstract fun forwardLogDao(): ForwardLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_forwarder.db"
                ).build().also { instance = it }
            }
        }
    }
}
