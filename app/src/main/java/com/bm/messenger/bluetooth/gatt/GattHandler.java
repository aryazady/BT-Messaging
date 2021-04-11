package com.bm.messenger.bluetooth.gatt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import com.bm.messenger.model.MessageModel;

public interface GattHandler {

    String GATT_SERVICE_UUID = "050add86-75be-4f36-83fd-5203eccd11e1";
    String WRITE_CHARACTERISTIC_UUID = "41715aa2-0d0a-4339-9969-a2cf12fb198b";
    String READ_CHARACTERISTIC_UUID = "a1279e07-2b41-4b71-b95c-e2a43c0d431c";

    void onStateChange(BluetoothDevice device, int state);

//    void onClientConnected(BluetoothDevice device);

    void insertUser(BluetoothDevice device,String userData);

    boolean onMessageReceive(BluetoothDevice device, String msg);
}
