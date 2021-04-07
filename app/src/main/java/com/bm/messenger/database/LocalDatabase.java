package com.bm.messenger.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.bm.messenger.database.dao.MessageDao;
import com.bm.messenger.database.dao.UserDao;
import com.bm.messenger.database.dao.UserMessageJoinDao;
import com.bm.messenger.model.MessageModel;
import com.bm.messenger.model.UserModel;

@Database(entities = {UserModel.class, MessageModel.class}, version = 1, exportSchema = false)
public abstract class LocalDatabase extends RoomDatabase {
    public abstract UserDao userDao();

    public abstract MessageDao messageDao();

    public abstract UserMessageJoinDao userMessageJoinDao();
}
