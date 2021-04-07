package com.bm.messenger.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.bm.messenger.model.MessageModel;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public interface MessageDao {

    @Insert
    Single<List<Long>> insert(MessageModel... message);

    @Delete
    Single<Integer> delete(MessageModel... message);

    @Query("SELECT * FROM message WHERE (message_source = :srcId AND message_destination IS NOT NULL) OR message_destination = :srcId")
    Flowable<List<MessageModel>> userMessageList(String srcId);

    @Query("SELECT * FROM message WHERE (message_source IN(:srcId) AND message_destination IS NOT NULL) OR message_destination IN(:srcId)")
    Flowable<List<MessageModel>> historyMessageList(List<String> srcId);

    @Query("SELECT * FROM message m WHERE m.message_destination IS NULL")
    Flowable<List<MessageModel>> broadcastMessageList();
}
