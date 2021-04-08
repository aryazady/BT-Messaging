package com.bm.messenger.model;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;
import java.util.List;

public class DeviceUnsentMessageModel implements Serializable {

    private BluetoothDevice device;
    private List<String> messages;
    private int attempt = 0;

    public DeviceUnsentMessageModel(BluetoothDevice device, List<String> messages) {
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

    public void attempted() {
        attempt++;
    }
}
