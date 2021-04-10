package com.bm.messenger.model;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

public class ReadQueueModel implements Serializable {

    private BluetoothDevice device;
    private int attempt = 0;

    public ReadQueueModel(BluetoothDevice device) {
        this.device = device;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public int getAttempt() {
        return attempt;
    }

    public void attempted() {
        attempt += 2;
    }

    public void dismiss() {
        attempt--;
    }
}
