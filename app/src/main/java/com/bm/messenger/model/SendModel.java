package com.bm.messenger.model;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SendModel implements Serializable {

    private BluetoothDevice device;
    private List<String> messages;
    private int attempt = 0;

    public SendModel(BluetoothDevice device) {
        this.device = device;
        messages = new ArrayList<>();
    }

    public SendModel(BluetoothDevice device, List<String> messages) {
        this.device = device;
        this.messages = messages;
    }

    public int getAttempt() {
        return attempt;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void addMessage(List<String> messages) {
        for (String msg : messages)
            if (!this.messages.contains(msg))
                this.messages.add(msg);
    }

    public void addMessage(String message) {
        if (!this.messages.contains(message))
            this.messages.add(message);
    }

    public void removeMessage(String message) {
        messages.remove(message);
    }

    public void attempted() {
        attempt++;
    }

    @Override
    public boolean equals(Object o) {
//        if (this == o) return true;
        if (!(o instanceof SendModel)) return false;
        SendModel sendModel = (SendModel) o;
        return getDevice().equals(sendModel.getDevice());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDevice());
    }
}
