package com.bm.messenger.bluetooth.gatt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.os.HandlerCompat;

import com.bm.messenger.utility.Utility;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class GATTClient /*extends BluetoothGattCallback*/ {

    private static final String TAG = "GattClient";
    private static final int CONNECTION_TIME_OUT = 6000;
    private static final int RW_TIME_OUT = 9000;
    private static final int TIMED_OUT = 1864;
    private final Context mContext;
    private final Queue<BluetoothDevice> readQueue = new LinkedList<>();
    private final Set<BluetoothDevice> readUnique = new HashSet<>();
    private final Queue<String> writeQueue = new LinkedList<>();
    private final Handler connectionTimeoutHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    private final Handler rwTimeoutHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    private final BluetoothManager bluetoothManager;
    //    private List<BluetoothGatt> clients = new ArrayList<>();
    private final GattHandler gattHandler;
    private final List<BluetoothDevice> nearbyDevices = new ArrayList<>();
    private boolean isReading = false;
    private boolean isWriting = false;
    //    private boolean isRW = false;
//    private boolean isConnected = false;
    private boolean isFromQueue = false;
    //    private int currentMtu = 23;
    private int receiverDevice;
    private String data = "";
    private BluetoothGatt currGatt;

    public GATTClient(Context context, BluetoothManager bluetoothManager, GattHandler gattHandler) {
        mContext = context;
        this.bluetoothManager = bluetoothManager;
        this.gattHandler = gattHandler;
    }

    private void connect(BluetoothDevice device) {
//        isRW = false;
//        isConnected = false;
        connectionTimeoutHandler.postDelayed(() -> {
//            if (!isConnected) {
            Log.d(TAG, "connection timed out");
            onFailed(currGatt, TIMED_OUT);
//            }
        }, CONNECTION_TIME_OUT);
        rwTimeoutHandler.postDelayed(() -> {
//            if (!isRW && isConnected) {
            Log.d(TAG, "rw timed out");
            onFailed(currGatt, TIMED_OUT);
//            }
        }, RW_TIME_OUT);
        Log.d(TAG, "request to connect with device: " + device.getAddress());
        currGatt = device.connectGatt(mContext, false, getCallback(), BluetoothDevice.TRANSPORT_LE);
        currGatt.connect();
    }

    private BluetoothGattCallback getCallback() {
        return new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                Log.d(TAG, "connected with state: " + status);
                connectionTimeoutHandler.removeCallbacksAndMessages(null);
                if (/*!isTimedOut && */status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED)
                        gatt.requestMtu(512);
                    else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        onFailed(gatt, status);
                    }
                } else {
                    onFailed(gatt, status);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                Log.d(TAG, "discovered with status: " + status);
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    onFailed(gatt, status);
                } else {
                    if (isReading) {
                        BluetoothGattService service = gatt.getService(UUID.fromString(GattHandler.GATT_SERVICE_UUID));
                        try {
                            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(GattHandler.READ_CHARACTERISTIC_UUID));
                            gatt.readCharacteristic(characteristic);
                        } catch (NullPointerException e) {
                            onFailed(gatt, status);
                        }
                    } else if (isWriting) {
                        BluetoothGattService service = gatt.getService(UUID.fromString(GattHandler.GATT_SERVICE_UUID));
                        try {
                            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(GattHandler.WRITE_CHARACTERISTIC_UUID));
                            characteristic.setValue(data);
                            gatt.writeCharacteristic(characteristic);
                        } catch (NullPointerException e) {
                            onFailed(gatt, status);
                        }
                    }
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                Log.d(TAG, "characteristic read");
                rwTimeoutHandler.removeCallbacksAndMessages(null);
                if (characteristic.getUuid().toString().equals(GattHandler.READ_CHARACTERISTIC_UUID)) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        gattHandler.insertUser(gatt.getDevice(), characteristic.getStringValue(0));
                        if (isFromQueue) {
                            readQueue.remove();
                            readUnique.remove(gatt.getDevice());
                            isFromQueue = false;
                        }
                        isReading = false;
                        gatt.close();
                        checkQueue();
                    } else onFailed(gatt, status);
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                Log.d(TAG, "characteristic wrote");
                rwTimeoutHandler.removeCallbacksAndMessages(null);
                if (characteristic.getUuid().toString().equals(GattHandler.WRITE_CHARACTERISTIC_UUID)) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (isFromQueue) {
                            writeQueue.remove();
                            isFromQueue = false;
                        }
                        gatt.close();
                        broadcast();
                    } else onFailed(gatt, status);
                }
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);
                Log.d(TAG, "mtu changed. mtu = " + mtu);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    gatt.discoverServices();
                } else {
                    onFailed(gatt, status);
                    Utility.getToast(mContext, "Can't get Max Throughput");
                }
            }
        };
    }

    public void getUserData(BluetoothDevice device) {
        if (!isWriting && !isReading) {
            isReading = true;
            connect(device);
        } else {
            if (readUnique.add(device))
                readQueue.add(device);
        }
    }

    public boolean isReading() {
        return isReading;
    }

    private void sendMessage(String msg) {
        if (!isWriting && !isReading) {
            isWriting = true;
            data = msg;
            receiverDevice = 0;
            broadcast();
//            connect(device);
        } else {
            Log.d(TAG, isWriting ? "occupy writing" : "occupy reading");
            writeQueue.add(msg);
        }
    }

    public void sendMessage(List<BluetoothDevice> devices, String message) {
        nearbyDevices.clear();
        nearbyDevices.addAll(devices);
        sendMessage(message);
    }

    public void sendMessage(List<BluetoothDevice> devices, List<String> messages) {
        nearbyDevices.clear();
        nearbyDevices.addAll(devices);
        for (String msg : messages)
            sendMessage(msg);
    }

    private synchronized void broadcast() {
        if (nearbyDevices.size() > 0)
            if (receiverDevice >= nearbyDevices.size()) {
                isWriting = false;
                checkQueue();
            } else {
                connect(nearbyDevices.get(receiverDevice));
                receiverDevice++;
            }
        else
            noDevice();
    }

    private void noDevice() {
        if (!isFromQueue)
            writeQueue.add(data);
        isWriting = false;
        isFromQueue = false;
    }

    public void terminate() {
        if (currGatt != null)
            currGatt.close();
    }

//    private void sendPacket(String msg) {
//        final BluetoothGattCharacteristic characteristic = client
//                .getService(UUID.fromString(GattHandler.GATT_SERVICE_UUID))
//                .getCharacteristic(UUID.fromString(GattHandler.WRITE_CHARACTERISTIC_UUID));
//        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
//        characteristic.setValue(msg);
//        if (!client.writeCharacteristic(characteristic)) {
//            Utility.getToast(mContext, "Couldn't send data!!");
//        }
//    }

    private void onFailed(BluetoothGatt gatt, int status) {
        Log.d(TAG, "Failed");
        if (isWriting && !isFromQueue)
            writeQueue.add(data);
        isFromQueue = false;
        isReading = false;
        connectionTimeoutHandler.removeCallbacksAndMessages(null);
        rwTimeoutHandler.removeCallbacksAndMessages(null);
        if (status == TIMED_OUT || status == 133) {
            try {
                final Method refresh = gatt.getClass().getMethod("refresh");
                refresh.invoke(gatt);
            } catch (Exception e) {
                e.printStackTrace();
            }
            nearbyDevices.remove(gatt.getDevice());
            gattHandler.onStateChange(gatt.getDevice(), BluetoothGatt.GATT_FAILURE);
        }
        gatt.close();
        if (isWriting)
            broadcast();
        else
            checkQueue();
    }

    private void checkQueue() {
        Log.d(TAG, "Checking Queue. Write: " + writeQueue.size() + " Read: " + readUnique.size());
        if (!readQueue.isEmpty() && readQueue.size() >= writeQueue.size()) {
            isFromQueue = true;
            getUserData(readQueue.element());
        } else if (!writeQueue.isEmpty()) {
            isFromQueue = true;
            String message = writeQueue.element();
            sendMessage(message);
        }
    }

//    @Override
//    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//        super.onConnectionStateChange(gatt, status, newState);
//        if (!isTimedOut && status == BluetoothGatt.GATT_SUCCESS) {
//            if (newState == BluetoothProfile.STATE_CONNECTED)
//                gatt.requestMtu(512);
//            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                onFailed(gatt);
//            }
//        } else {
//            onFailed(gatt);
//        }
//    }
//
//    @Override
//    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//        super.onServicesDiscovered(gatt, status);
//        if (status != BluetoothGatt.GATT_SUCCESS) {
//            onFailed(gatt);
//        } else {
////            if (currentMtu == 512) {
//            if (isReading) {
//                BluetoothGattService service = gatt.getService(UUID.fromString(GattHandler.GATT_SERVICE_UUID));
//                try {
//                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(GattHandler.READ_CHARACTERISTIC_UUID));
//                    gatt.readCharacteristic(characteristic);
//                } catch (NullPointerException e) {
//                    onFailed(gatt);
//                }
//            } else if (isWriting) {
//                BluetoothGattService service = gatt.getService(UUID.fromString(GattHandler.GATT_SERVICE_UUID));
//                try {
//                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(GattHandler.WRITE_CHARACTERISTIC_UUID));
//                    characteristic.setValue(data);
//                    gatt.writeCharacteristic(characteristic);
//                } catch (NullPointerException e) {
//                    onFailed(gatt);
//                }
//            }
////            } else {
////                onFailed(gatt);
////                Utility.getToast(mContext, "Can't get Max Throughput");
////            }
//        }
//    }
//
//    @Override
//    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//        super.onCharacteristicWrite(gatt, characteristic, status);
//        if (characteristic.getUuid().toString().equals(GattHandler.WRITE_CHARACTERISTIC_UUID)) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                if (isFromQueue) {
//                    writeQueue.remove();
//                    isFromQueue = false;
//                }
////                gatt.disconnect();
////                currentMtu = 23;
//                receiverDevice++;
//                broadcast();
//            } else onFailed(gatt);
//        }
//    }
//
//    @Override
//    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//        super.onCharacteristicRead(gatt, characteristic, status);
//        if (characteristic.getUuid().toString().equals(GattHandler.READ_CHARACTERISTIC_UUID)) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                gattHandler.insertUser(gatt.getDevice(), characteristic.getStringValue(0));
//                if (isFromQueue) {
//                    readQueue.remove();
//                    isFromQueue = false;
//                }
//                gatt.disconnect();
//                isReading = false;
////                currentMtu = 23;
//                checkQueue();
//            } else onFailed(gatt);
//        }
//    }
//
//    @Override
//    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
//        super.onMtuChanged(gatt, mtu, status);
//        if (status == BluetoothGatt.GATT_SUCCESS) {
////            currentMtu = mtu;
//            gatt.discoverServices();
//        } else {
//            onFailed(gatt);
//            Utility.getToast(mContext, "Can't get Max Throughput");
//        }
//    }
}
