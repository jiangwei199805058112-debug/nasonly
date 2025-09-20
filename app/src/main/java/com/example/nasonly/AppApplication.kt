package com.example.nasonly

import android.app.Application
import androidx.room.Room
import dagger.hilt.android.HiltAndroidApp
import com.example.nasonly.data.db.AppDatabase

@HiltAndroidApp
class AppApplication : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "nas_player_db"
        )
            .allowMainThreadQueries() // 你原来的设置保留
            .build()
    }
}
