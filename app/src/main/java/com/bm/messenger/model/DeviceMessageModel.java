package com.bm.messenger.model;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

public class DeviceMessageModel implements Serializable {

    private BluetoothDevice device;
    private String message;

    public DeviceMessageModel(String message, BluetoothDevice device) {
        this.message = message;
        this.device = device;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public String getMessage() {
        return message;
    }
}
