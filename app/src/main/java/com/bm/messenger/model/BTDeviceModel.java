package com.bm.messenger.model;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;
import java.util.Objects;

public class BTDeviceModel implements Serializable {

    private BluetoothDevice device;
    private boolean isAround;

    public BTDeviceModel(BluetoothDevice device) {
        this.device = device;
    }

    public BTDeviceModel(BluetoothDevice device, boolean isAround) {
        this.device = device;
        this.isAround = isAround;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public boolean isAround() {
        return isAround;
    }

    public void setAround(boolean around) {
        isAround = around;
    }

    @Override
    public boolean equals(Object o) {
//        if (this == o) return true;
        if (!(o instanceof BTDeviceModel)) return false;
        BTDeviceModel that = (BTDeviceModel) o;
        return getDevice().equals(that.getDevice());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDevice(), isAround());
    }
}
