package com.bm.messenger.model;

import androidx.room.Embedded;
import androidx.room.Entity;

import java.io.Serializable;

@Entity
public class ChatModel implements Serializable, Comparable<ChatModel> {

    @Embedded
    private UserModel user;
    @Embedded
    private MessageModel message;

    public ChatModel(UserModel user, MessageModel message) {
        this.user = user;
        this.message = message;
    }

    public UserModel getUser() {
        return user;
    }

    public MessageModel getMessage() {
        return message;
    }

    @Override
    public int compareTo(ChatModel o) {
        return Long.compare(this.message.date, o.message.date);
    }
}
