package com.bm.messenger.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bm.messenger.model.UserModel;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public interface UserDao {
    @Insert
    Single<List<Long>> insert(UserModel... user);

    @Delete
    Single<Integer> delete(UserModel... user);

    @Query("SELECT * FROM user LIMIT 1")
    Single<UserModel> getCurrentUser();

    @Query("SELECT * FROM user")
    Flowable<List<UserModel>> userList();

    @Query("SELECT * FROM user WHERE user_id IN(:uId)")
    Flowable<List<UserModel>> getUsers(List<String> uId);
}
