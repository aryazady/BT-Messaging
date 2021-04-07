package com.bm.messenger.database;

import android.content.Context;

import androidx.room.Room;

public class Database {

    private static final String DATABASE_NAME = "my-app-db";
    private static LocalDatabase dbInstance;

    public void closeDatabase() {
        if (dbInstance != null) {
            dbInstance.close();
            dbInstance = null;
        }
    }

    public synchronized LocalDatabase getDatabase(Context context) {
        if (dbInstance == null)
            dbInstance = Room.databaseBuilder(context,
                    LocalDatabase.class,
                    DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        return dbInstance;
    }
}
