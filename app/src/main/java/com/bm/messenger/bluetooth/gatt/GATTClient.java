package com.bm.messenger.bluetooth.gatt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.os.HandlerCompat;

import com.bm.messenger.model.DeviceUnsentMessageModel;
import com.bm.messenger.model.ReadQueueModel;
import com.bm.messenger.utility.Utility;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class GATTClient /*extends BluetoothGattCallback*/ {

    private static final String TAG = "GattClient";
    private static final int CONNECTION_TIME_OUT = 6000;
    private static final int RW_TIME_OUT = 9000;
    private static final int CONNECTION_TIMED_OUT = 1863;
    private static final int RW_TIMED_OUT = 1864;
    private final Context mContext;
    //    private final Queue<BluetoothDevice> readQueue = new LinkedList<>();
    private final List<ReadQueueModel> readQueue = new ArrayList<>();
    private final List<String> writeQueue = new ArrayList<>();
    private final Handler connectionTimeoutHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    private final Handler rwTimeoutHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    //    private final BluetoothManager bluetoothManager;
    //    private List<BluetoothGatt> clients = new ArrayList<>();
    private final GattHandler gattHandler;
    private final List<BluetoothDevice> nearbyDevices = new ArrayList<>();
    private final List<DeviceUnsentMessageModel> lostMessages = new ArrayList<>();
    private volatile boolean isReading = false;
    private volatile boolean isWriting = false;
    //    private boolean isRW = false;
//    private boolean isConnected = false;
    private volatile boolean fromQueue = false;
    private volatile boolean isLostMessage = false;
    //    private int currentMtu = 23;
    private int receiverDevice;
    private int writeIndex;
    //    private String data = "";
    private BluetoothGatt currGatt;

    public GATTClient(Context context, /*BluetoothManager bluetoothManager,*/ GattHandler gattHandler) {
        mContext = context;
//        this.bluetoothManager = bluetoothManager;
        this.gattHandler = gattHandler;
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
                        onFailed(gatt, newState);
                    }
                } else {
                    onFailed(gatt, newState);
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
                        } catch (Exception e) {
                            onFailed(gatt, status);
                        }
                    } else if (isWriting) {
                        BluetoothGattService service = gatt.getService(UUID.fromString(GattHandler.GATT_SERVICE_UUID));
                        try {
                            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(GattHandler.WRITE_CHARACTERISTIC_UUID));
                            if (isLostMessage) {
                                lostMessages.get(receiverDevice).attempted();
                                characteristic.setValue(lostMessages.get(receiverDevice).getMessages().get(writeIndex));
                            } else {
                                characteristic.setValue(writeQueue.get(writeIndex));
                            }
                            gatt.writeCharacteristic(characteristic);
                        } catch (Exception e) {
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
                        if (fromQueue) {
                            readQueue.remove(0);
                            fromQueue = false;
                        }
                        isReading = false;
                        gatt.close();
                    } else onFailed(gatt, status);
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                Log.d(TAG, "characteristic wrote");
                rwTimeoutHandler.removeCallbacksAndMessages(null);
                if (characteristic.getUuid().toString().equals(GattHandler.WRITE_CHARACTERISTIC_UUID)) {
                    writeIndex++;
                    if (isLostMessage) {
                        if (writeIndex >= lostMessages.get(receiverDevice).getMessages().size()) {
                            Log.d(TAG, "Writing Lost Message: Done");
                            writeIndex = 0;
                            if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED)
                                receiverDevice += 100000;
                            else
                                receiverDevice++;
                            broadcast();
                        } else {
                            lostMessages.get(receiverDevice).attempted();
                            characteristic.setValue(lostMessages.get(receiverDevice).getMessages().get(writeIndex));
                            gatt.writeCharacteristic(characteristic);
                        }
                    } else {
                        if (writeIndex >= writeQueue.size()) {
                            Log.d(TAG, "Writing Queue: Done");
                            writeIndex = 0;
                            if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED)
                                receiverDevice += 100000;
                            else
                                receiverDevice++;
                            gatt.close();
                            broadcast();
                        } else {
                            characteristic.setValue(writeQueue.get(writeIndex));
                            gatt.writeCharacteristic(characteristic);
                        }
                    }
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

    private void onFailed(BluetoothGatt gatt, int state) {
        Log.d(TAG, "Failed");
//        if (isWriting && !isFromQueue)
//            writeQueue.add(data);
        if (isReading)
            if (!fromQueue)
                readQueue.add(new ReadQueueModel(gatt.getDevice()));
            else if (pruneReadQueue(gatt.getDevice()))
                refreshGatt(gatt);
        fromQueue = false;
        isReading = false;
        connectionTimeoutHandler.removeCallbacksAndMessages(null);
        rwTimeoutHandler.removeCallbacksAndMessages(null);
        if (state == RW_TIMED_OUT) {
            refreshGatt(gatt);
//            nearbyDevices.remove(gatt.getDevice());
//            gattHandler.onStateChange(gatt.getDevice(), BluetoothProfile.);
        }
//        if (gatt != null)
        gatt.close();
//        gattHandler.onStateChange(gatt.getDevice(), BluetoothProfile.STATE_DISCONNECTING);
        if (isWriting) {
            writeIndex = 0;
            for (DeviceUnsentMessageModel model : lostMessages)
                if (model.getDevice() == gatt.getDevice()) {
                    model.addMessage(writeQueue);
                    broadcast();
                    return;
                }
            lostMessages.add(new DeviceUnsentMessageModel(gatt.getDevice(), writeQueue));
            receiverDevice++;
            broadcast();
        } else
            checkQueue();
    }

    private void refreshGatt(BluetoothGatt gatt) {
        try {
            final Method refresh = gatt.getClass().getMethod("refresh");
            refresh.invoke(gatt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connect(BluetoothDevice device) {
//        isRW = false;
//        isConnected = false;
        connectionTimeoutHandler.postDelayed(() -> {
//            if (!isConnected) {
            Log.d(TAG, "connection timed out");
            onFailed(currGatt, CONNECTION_TIMED_OUT);
//            }
        }, CONNECTION_TIME_OUT);
        rwTimeoutHandler.postDelayed(() -> {
//            if (!isRW && isConnected) {
            Log.d(TAG, "rw timed out");
            onFailed(currGatt, RW_TIMED_OUT);
//            }
        }, RW_TIME_OUT);
        Log.d(TAG, "request to connect with device: " + device.getAddress());
        currGatt = device.connectGatt(mContext, false, getCallback(), BluetoothDevice.TRANSPORT_LE);
        currGatt.connect();
    }

    public void getUserData(BluetoothDevice device) {
        if (!isWriting && !isReading) {
            isReading = true;
            connect(device);
            gattHandler.onStateChange(null, BluetoothProfile.STATE_CONNECTING);
        } else {
            readQueue.add(new ReadQueueModel(device));
        }
    }

    public boolean isReading() {
        return isReading;
    }

    private void sendMessage() {
        if (!isWriting && !isReading) {
            isWriting = true;
            receiverDevice = 0;
            broadcast();
            gattHandler.onStateChange(null, BluetoothProfile.STATE_CONNECTING);
//            connect(device);
        } else
            Log.d(TAG, isWriting ? "occupy writing" : "occupy reading");
    }

    public void sendMessage(String message) {
//        nearbyDevices.clear();
//        nearbyDevices.addAll(devices);
        writeQueue.add(message);
        checkQueue();
    }

    public void sendMessage(List<String> messages) {
//        nearbyDevices.clear();
//        nearbyDevices.addAll(devices);
        writeQueue.addAll(messages);
        checkQueue();
    }

//    private void multipleWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//        writeIndex = 0;
//        for (String msg : writeQueue) {
//            characteristic.setValue(msg);
//            gatt.writeCharacteristic(characteristic);
//        }
//    }

    private synchronized void broadcast() {
        if (nearbyDevices.size() > 0)
            if (isLostMessage) {
                if (receiverDevice >= lostMessages.size()) {
                    isLostMessage = false;
                    receiverDevice = 0;
                    pruneLostMessage();
                    broadcast();
                } else {
                    connect(lostMessages.get(receiverDevice).getDevice());
                }
            } else {
                if (receiverDevice >= nearbyDevices.size()) {
                    isWriting = false;
                    writeQueue.clear();
                    checkQueue();
                } else {
                    connect(nearbyDevices.get(receiverDevice));
                }
            }
        else
            noDevice();
    }

    private void pruneLostMessage() {
        Iterator<DeviceUnsentMessageModel> iterator = lostMessages.iterator();
        while (iterator.hasNext()) {
            DeviceUnsentMessageModel model = iterator.next();
            if (model.getAttempt() > 2)
                iterator.remove();
        }
    }

    private void noDevice() {
        isWriting = false;
        fromQueue = false;
        isLostMessage = false;
        gattHandler.onStateChange(null, BluetoothProfile.STATE_DISCONNECTED);
    }

    private boolean pruneReadQueue(BluetoothDevice device) {
        Iterator<ReadQueueModel> iterator = readQueue.iterator();
        while (iterator.hasNext()) {
            ReadQueueModel model = iterator.next();
            if (model.getDevice() == device) {
                if (model.getAttempt() >= 5) {
                    iterator.remove();
                    gattHandler.onStateChange(device, BluetoothProfile.STATE_DISCONNECTED);
                    return true;
                }
                break;
            }
        }
        return false;
    }

    public void terminate() {
        if (currGatt == null)
            return;
        currGatt.close();
        currGatt = null;
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

    private void checkQueue() {
        Log.d(TAG, "Checking Queue. Write: " + writeQueue.size() + " Read: " + readQueue.size() + " Lost Msg: " + lostMessages.size());
        if (!readQueue.isEmpty() && nearbyDevices.size() <= 4) {
            fromQueue = true;
            ReadQueueModel model = readQueue.get(0);
            model.attempted();
            getUserData(model.getDevice());
        } else if (!lostMessages.isEmpty()) {
            isLostMessage = true;
            sendMessage();
        } else if (!writeQueue.isEmpty()) {
            sendMessage();
        } else
            gattHandler.onStateChange(null, BluetoothProfile.STATE_DISCONNECTED);
    }

    public void updateNearby(List<BluetoothDevice> devices) {
        nearbyDevices.clear();
        nearbyDevices.addAll(devices);
        if (!devices.isEmpty()) {
            updateReadQueue(devices);
            updateLostMessages(devices);
            checkQueue();
        } else {
            readQueue.clear();
            lostMessages.clear();
        }
    }

    private void updateReadQueue(List<BluetoothDevice> devices) {
        Iterator<ReadQueueModel> iterator = readQueue.iterator();
        while (iterator.hasNext()) {
            ReadQueueModel model = iterator.next();
            if (!devices.contains(model.getDevice()))
                iterator.remove();
        }
    }

    private void updateLostMessages(List<BluetoothDevice> devices) {
        Iterator<DeviceUnsentMessageModel> iterator = lostMessages.iterator();
        while (iterator.hasNext()) {
            DeviceUnsentMessageModel model = iterator.next();
            if (!devices.contains(model.getDevice()))
                iterator.remove();
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
