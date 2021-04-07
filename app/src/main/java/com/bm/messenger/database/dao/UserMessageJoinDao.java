package com.bm.messenger.database.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.bm.messenger.model.ChatModel;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface UserMessageJoinDao {

    @Query("SELECT * FROM user u INNER JOIN message m ON (u.user_id = m.message_source AND m.message_destination IS NULL)")
    Flowable<List<ChatModel>> getBroadcastMessages();

    @Query("SELECT * FROM user u, message m WHERE u.user_id = m.conversation_id AND m.message_id IN(SELECT message_id FROM(SELECT message_id, conversation_id, MAX(message_send_time) FROM message GROUP BY conversation_id))")
    Flowable<List<ChatModel>> getHistory();

    @Query("SELECT * FROM user u INNER JOIN message m ON (u.user_id = m.conversation_id AND m.conversation_id = :conversationId)")
    Flowable<List<ChatModel>> getConversationChats(String conversationId);

//    @Query("SELECT DISTINCT message_id FROM message m1, message m2 WHERE m1.message_id = m2.message_id AND m1.message_send_time >= m2.message_send_time")
//    Flowable<List<ChatModel>> test();
}
