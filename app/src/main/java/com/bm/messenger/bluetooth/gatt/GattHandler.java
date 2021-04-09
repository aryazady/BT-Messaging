package com.bm.messenger.bluetooth.gatt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import com.bm.messenger.model.MessageModel;

public interface GattHandler {

    String GATT_SERVICE_UUID = "4f00078c-8bfe-11eb-8dcd-0242ac130003";
    String WRITE_CHARACTERISTIC_UUID = "4f00087c-8bfe-11eb-8dcd-0242ac130003";
    String READ_CHARACTERISTIC_UUID = "9ef8321a-8c7f-11eb-8dcd-0242ac130003";

    void onStateChange(BluetoothDevice device, int state);

//    void onClientConnected(BluetoothDevice device);

    void insertUser(BluetoothDevice device,String userData);

    boolean onMessageReceive(BluetoothDevice device, String msg);
}
