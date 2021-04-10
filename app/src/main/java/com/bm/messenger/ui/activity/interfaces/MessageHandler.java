package com.bm.messenger.ui.activity.interfaces;

import com.bm.messenger.model.MessageModel;

public interface MessageHandler {
    void onMessageSent(MessageModel message);
}
