package com.sanx.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sanx.app.data.local.dao.ContactDao
import com.sanx.app.data.local.dao.LogDao
import com.sanx.app.data.local.entity.Contact
import com.sanx.app.data.local.entity.EmergencyLog

@Database(
    entities = [EmergencyLog::class, Contact::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun contactDao(): ContactDao
}
