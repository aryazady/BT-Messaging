package com.bm.messenger.ui.activity;

import com.bm.messenger.model.MessageModel;

public interface MessageHandler {
    void onMessageSent(MessageModel message);
}
