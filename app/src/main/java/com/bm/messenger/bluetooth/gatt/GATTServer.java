package com.bm.messenger.bluetooth.gatt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;


public class GATTServer extends BluetoothGattServerCallback {

    private final GattHandler callbackHandler;
    private BluetoothGattServer server;

    public GATTServer(Context mContext, BluetoothManager bluetoothManager, GattHandler gattHandler) {
        server = bluetoothManager.openGattServer(mContext, this);
        this.callbackHandler = gattHandler;
    }

    public boolean addService(BluetoothGattService service) {
        return server.addService(service);
    }

    public void terminate() {
        if (server == null)
            return;
        server.close();
        server = null;
    }

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        super.onConnectionStateChange(device, status, newState);
//        callbackHandler.onStateChange(device, newState);
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
//        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        boolean isMine = callbackHandler.onMessageReceive(new String(value));
        if (responseNeeded)
            server.sendResponse(device, requestId, isMine ? BluetoothGatt.GATT_SUCCESS : BluetoothGatt.GATT_WRITE_NOT_PERMITTED, offset, value);
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
        super.onMtuChanged(device, mtu);
    }
}
