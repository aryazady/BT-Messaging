package com.bm.messenger.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;

import java.io.Serializable;

import static androidx.room.ForeignKey.CASCADE;

@Entity(primaryKeys = {"message_id", "message_source"},
        tableName = "message",
        foreignKeys = @ForeignKey(entity = UserModel.class,
                parentColumns = "user_id",
                childColumns = "message_source",
                onDelete = CASCADE))
public class MessageModel implements Serializable {

    @ColumnInfo(name = "message_id")
    @NonNull
    public String id;
    @ColumnInfo(name = "conversation_id")
    public String conversationId;
    @ColumnInfo(name = "message_content")
    @NonNull
    public String content;
    @ColumnInfo(name = "message_source", index = true)
    @NonNull
    public String src;
    @ColumnInfo(name = "message_destination")
    public String dst;
    @ColumnInfo(name = "message_send_time")
    public long date;
    @Ignore
    private int count = 0;

    public MessageModel(@NonNull String id, String conversationId, @NonNull String content, @NonNull String src, @Nullable String dst, long date) {
        this.id = id;
        this.conversationId = conversationId;
        this.content = content;
        this.src = src;
        this.dst = dst;
        this.date = date;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    @Override
    public String toString() {
        if (dst == null)
            return id + "$" + content + "$" + src + "$" + date;
        else
            return id + "$" + conversationId + "$" + content + "$" + src + "$" + dst + "$" + date;

    }

    public void setId(@NonNull String id) {
        this.id = id;
    }
}
