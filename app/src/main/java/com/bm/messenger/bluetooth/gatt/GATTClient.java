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

import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;

import com.bm.messenger.model.ExcludeDestaionModel;
import com.bm.messenger.model.ReadQueueModel;
import com.bm.messenger.model.SendModel;
import com.bm.messenger.model.UserModel;
import com.bm.messenger.utility.Utility;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GATTClient /*extends BluetoothGattCallback*/ {

    private static final String TAG = "GattClient";
    private static final int CONNECTION_TIME_OUT = 6000;
    private static final int RW_TIME_OUT = 9000;
    private static final int CONNECTION_TIMED_OUT = 1863;
    private static final int RW_TIMED_OUT = 1864;
    private static final int TERMINATE = 1865;
    private final Context mContext;
    //    private final Queue<BluetoothDevice> readQueue = new LinkedList<>();
    private final List<ReadQueueModel> readQueue = new ArrayList<>();
    private final List<String> writeQueue = new ArrayList<>();
    private final Handler connectionTimeoutHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    private final Handler rwTimeoutHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    //    private final BluetoothManager bluetoothManager;
    //    private List<BluetoothGatt> clients = new ArrayList<>();
    private final GattHandler gattHandler;
    private final List<SendModel> lostMessages = new ArrayList<>();
    private final List<ExcludeDestaionModel> excludeDevice = new ArrayList<>();
    private Map<UserModel, BluetoothDevice> nearbyPeople;
    private List<SendModel> sendQueue = new ArrayList<>();
    private volatile boolean isReading = false;
    private volatile boolean isWriting = false;
    //    private boolean isRW = false;
//    private boolean isConnected = false;
    private volatile boolean fromQueue = false;
    //    private volatile boolean isLostMessage = false;
    //    private int currentMtu = 23;
    private int receiverDevice;
    private int writeIndex;
    //    private String data = "";
    private BluetoothGatt currGatt;

    public GATTClient(Context context, Map<UserModel, BluetoothDevice> nearbyPeople, GattHandler gattHandler) {
        mContext = context;
        this.nearbyPeople = nearbyPeople;
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
                        } catch (Exception e) {
                            onFailed(gatt, status);
                        }
                    } else if (isWriting) {
                        BluetoothGattService service = gatt.getService(UUID.fromString(GattHandler.GATT_SERVICE_UUID));
                        try {
                            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(GattHandler.WRITE_CHARACTERISTIC_UUID));
                            SendModel model = sendQueue.get(receiverDevice);
                            for (; ; ) {
                                String message = model.getMessages().get(writeIndex);
                                String dst = null;
                                for (Map.Entry<UserModel, BluetoothDevice> entry : nearbyPeople.entrySet())
                                    if (entry.getValue() == gatt.getDevice()) {
                                        dst = entry.getKey().id;
                                        break;
                                    }
//                                if (dst != null) {
                                ExcludeDestaionModel exclude = new ExcludeDestaionModel(message, dst);
                                if (excludeDevice.contains(exclude)) {
                                    Log.d(TAG, "Message was excluded");
                                    excludeDevice.remove(exclude);
                                    writeIndex++;
                                } else {
                                    characteristic.setValue(message);
                                    gatt.writeCharacteristic(characteristic);
                                    break;
                                }
                                if (writeIndex >= model.getMessages().size()) {
                                    doneWriting(BluetoothGatt.GATT_WRITE_NOT_PERMITTED);
                                    gatt.close();
                                    broadcast();
                                    break;
                                }
//                                } else {
//                                    characteristic.setValue(message);
//                                    gatt.writeCharacteristic(characteristic);
//                                    break;
//                                }
                            }
//                            characteristic.setValue(sendQueue.get(receiverDevice).getMessages().get(writeIndex));
//                            if (isLostMessage) {
//                                SendModel model = lostMessages.get(receiverDevice);
//
//                                characteristic.setValue(model.getMessages().get(writeIndex));
//                                model.attempted();
//                            } else {
//                                if (excludeDevice.contains(new DeviceMessageModel(writeQueue.get(writeIndex), gatt.getDevice()))) {
//                                    writeIndex++;
//                                    if (writeIndex >= writeQueue.size()) {
//                                        doneWriting(BluetoothGatt.GATT_WRITE_NOT_PERMITTED);
//                                    }
//                                }
//                                characteristic.setValue(writeQueue.get(writeIndex));
//                            }
//                            gatt.writeCharacteristic(characteristic);
                        } catch (Exception e) {
                            e.printStackTrace();
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
                Log.d(TAG, "characteristic wrote: " + new String(characteristic.getValue()));
                rwTimeoutHandler.removeCallbacksAndMessages(null);
                if (characteristic.getUuid().toString().equals(GattHandler.WRITE_CHARACTERISTIC_UUID)) {
                    writeIndex++;
//                    if (isLostMessage) {
//                        SendModel model = lostMessages.get(receiverDevice);
//                        if (writeIndex >= model.getMessages().size()) {
//                            Log.d(TAG, "Writing Lost Message: Done");
//                        } else {
//                            model.attempted();
//                            characteristic.setValue(model.getMessages().get(writeIndex));
//                            gatt.writeCharacteristic(characteristic);
//                        }
//                    } else {
                    if (writeIndex >= writeQueue.size()) {
                        doneWriting(status);
                        gatt.close();
                        broadcast();
                    } else {
                        SendModel model = sendQueue.get(receiverDevice);
                        for (; ; ) {
                            String message = model.getMessages().get(writeIndex);
                            String dst = getDestination(message);
                            if (dst != null) {
                                ExcludeDestaionModel exclude = new ExcludeDestaionModel(message, dst);
                                if (excludeDevice.contains(exclude)) {
                                    Log.d(TAG, "Message was excluded");
                                    excludeDevice.remove(exclude);
                                    writeIndex++;
                                } else {
                                    characteristic.setValue(message);
                                    gatt.writeCharacteristic(characteristic);
                                    break;
                                }
                                if (writeIndex >= model.getMessages().size()) {
                                    doneWriting(status);
                                    gatt.close();
                                    broadcast();
                                    break;
                                }
                            } else {
                                characteristic.setValue(message);
                                gatt.writeCharacteristic(characteristic);
                                break;
                            }
                        }
//                        characteristic.setValue(writeQueue.get(writeIndex));
//                        gatt.writeCharacteristic(characteristic);
                    }
//                }
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
                    Utility.makeToast(mContext, "Can't get Max Throughput");
                }
            }
        }

                ;
    }

    private void doneWriting(int status) {
        Log.d(TAG, "going next device message writing");
        writeIndex = 0;
        if (status == BluetoothGatt.GATT_SUCCESS)
            receiverDevice += 100000;
        else
            receiverDevice++;
    }

    private void onFailed(BluetoothGatt gatt, int status) {
        Log.d(TAG, "Failed");
//        if (isWriting && !isFromQueue)
//            writeQueue.add(data);
        connectionTimeoutHandler.removeCallbacksAndMessages(null);
        rwTimeoutHandler.removeCallbacksAndMessages(null);
        if (isReading)
            if (!fromQueue)
                readQueue.add(new ReadQueueModel(gatt.getDevice()));
            else if (pruneReadQueue(gatt.getDevice(), status))
                refreshGatt(gatt);
        fromQueue = false;
        isReading = false;
        if (status == RW_TIMED_OUT) {
            refreshGatt(gatt);
//            nearbyDevices.remove(gatt.getDevice());
//            gattHandler.onStateChange(gatt.getDevice(), BluetoothProfile.);
        }
//        if (gatt != null)
        gatt.close();
//        gattHandler.onStateChange(gatt.getDevice(), BluetoothProfile.STATE_DISCONNECTING);
        if (isWriting) {
            writeIndex = 0;
            SendModel sendModel = sendQueue.get(receiverDevice);
            if (!sendModel.getMessages().isEmpty())
                lostMessages.add(sendQueue.get(receiverDevice));
            if (status == TERMINATE) {
                isWriting = false;
                sendQueue.clear();
            } else {
                receiverDevice++;
                broadcast();
            }
        } else if (status == 133)
            HandlerCompat.createAsync(Looper.getMainLooper()).postDelayed(this::checkQueue, 1000);
        else if (status != TERMINATE)
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

    public void getUserData(ReadQueueModel readQueueModel) {
        if (!isWriting && !isReading) {
            isReading = true;
            readQueueModel.attempted();
            connect(readQueueModel.getDevice());
            gattHandler.onStateChange(null, BluetoothProfile.STATE_CONNECTING);
        } else {
            readQueue.add(readQueueModel);
        }
    }

    public boolean isReading() {
        return isReading;
    }

    private void sendMessage() {
        isWriting = true;
        receiverDevice = 0;
        if (checkInclude()) {
            broadcast();
            gattHandler.onStateChange(null, BluetoothProfile.STATE_CONNECTING);
        } else {
            isWriting = false;
            excludeDevice.clear();
            sendQueue.clear();
            checkQueue();
        }
//            connect(device);
    }

    private boolean checkInclude() {
        Set<String> uniqueExclude = new HashSet<>();
        List<String> messages = new ArrayList<>();
        for (ExcludeDestaionModel exclude : excludeDevice) {
            uniqueExclude.add(exclude.getDestination());
            messages.add(exclude.getMessage());
        }
        return uniqueExclude.size() != sendQueue.size() || messages.size() != sendQueue.get(0).getMessages().size();
    }

    public void sendMessage(String message, String excludeDestination) {
//        nearbyDevices.clear();
//        nearbyDevices.addAll(devices);
        if (excludeDestination != null)
            this.excludeDevice.add(new ExcludeDestaionModel(message, excludeDestination));
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

    private void broadcast() {
//        if (!sendQueue.isEmpty()) {
        if (receiverDevice >= sendQueue.size()) {
            isWriting = false;
            sendQueue.clear();
            pruneLostMessage();
            checkQueue();
        } else
            connect(sendQueue.get(receiverDevice).getDevice());
//        } else
//            noDevice();
//        if (nearbyDevices.size() > 0)
//            if (isLostMessage) {
//                if (receiverDevice >= lostMessages.size()) {
//                    isLostMessage = false;
//                    receiverDevice = 0;
//                    pruneLostMessage();
//                    broadcast();
//                } else {
//                    connect(lostMessages.get(receiverDevice).getDevice());
//                }
//            } else {
//                if (receiverDevice >= nearbyDevices.size()) {
//                    isWriting = false;
//                    sendQueue.clear();
//                    checkQueue();
//                } else {
//                    connect(nearbyDevices.get(receiverDevice));
//                }
//            }
//        else
//            noDevice();
    }

    private void pruneLostMessage() {
        Iterator<SendModel> iterator = lostMessages.iterator();
        while (iterator.hasNext()) {
            SendModel model = iterator.next();
            if (model.getAttempt() > 3)
                iterator.remove();
        }
    }

//    private void noDevice() {
//        isWriting = false;
//        fromQueue = false;
////        isLostMessage = false;
//        gattHandler.onStateChange(null, BluetoothProfile.STATE_DISCONNECTED);
//    }

    private boolean pruneReadQueue(BluetoothDevice device, int status) {
        ListIterator<ReadQueueModel> iterator = readQueue.listIterator();
        while (iterator.hasNext()) {
            ReadQueueModel model = iterator.next();
            if (model.getDevice() == device) {
                if (status == 133) {
                    model.dismiss();
                    iterator.set(model);
                    return false;
                } else if (model.getAttempt() >= 5) {
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
        onFailed(currGatt, TERMINATE);
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
        if (!isReading && !isWriting) {
            if (!readQueue.isEmpty() && nearbyPeople.size() <= 4) {
                fromQueue = true;
                getUserData(readQueue.get(0));
            } else if (!lostMessages.isEmpty()) {
                for (SendModel model : lostMessages) {
                    model.addMessage(new ArrayList<>(writeQueue));
                    model.attempted();
                    sendQueue.add(model);
                }
                lostMessages.clear();
                if (!writeQueue.isEmpty()) {
                    List<SendModel> sendModels = new ArrayList<>();
                    Iterator<String> iterator = writeQueue.iterator();
                    while (iterator.hasNext()) {
                        String message = iterator.next();
                        String dst = getDestination(message);
                        if (dst != null) {
                            UserModel user = new UserModel(dst, "");
                            SendModel knownDst = new SendModel(nearbyPeople.get(user));
                            if (nearbyPeople.containsKey(user)) {
                                Log.d(TAG, "Message has known dst");
                                int index = sendModels.indexOf(knownDst);
                                if (index >= 0)
                                    sendModels.get(index).addMessage(message);
                                else {
                                    knownDst.addMessage(message);
                                    sendModels.add(knownDst);
                                }
                                iterator.remove();
                            }
                        }
                    }
                    if (!sendModels.isEmpty())
                        sendQueue.addAll(sendModels);
                    if (!writeQueue.isEmpty()) {
                        for (BluetoothDevice device : nearbyPeople.values()) {
                            boolean contain = false;
                            for (SendModel model : sendQueue)
                                if (device == model.getDevice()) {
                                    contain = true;
                                    break;
                                }
                            if (!contain)
                                sendQueue.add(new SendModel(device, new ArrayList<>(writeQueue)));
                        }
                        writeQueue.clear();
                    }
                }
                sendMessage();
            } else if (!writeQueue.isEmpty()) {
                List<SendModel> sendModels = new ArrayList<>();
                Iterator<String> iterator = writeQueue.iterator();
                while (iterator.hasNext()) {
                    String message = iterator.next();
                    String dst = getDestination(message);
                    if (dst != null) {
                        UserModel user = new UserModel(dst, "");
                        SendModel knownDst = new SendModel(nearbyPeople.get(user));
                        if (nearbyPeople.containsKey(user)) {
                            Log.d(TAG, "Message has known dst");
                            int index = sendModels.indexOf(knownDst);
                            if (index >= 0)
                                sendModels.get(index).addMessage(message);
                            else {
                                knownDst.addMessage(message);
                                sendModels.add(knownDst);
                            }
                            iterator.remove();
                        }
                    }
                }
                if (!sendModels.isEmpty())
                    sendQueue.addAll(sendModels);
                if (!writeQueue.isEmpty()) {
                    for (BluetoothDevice device : nearbyPeople.values())
                        sendQueue.add(new SendModel(device, new ArrayList<>(writeQueue)));
                    writeQueue.clear();
                }
                sendMessage();
            } else
                gattHandler.onStateChange(null, BluetoothProfile.STATE_DISCONNECTED);
        } else
            Log.d(TAG, isWriting ? "occupy writing" : "occupy reading");
    }

    @Nullable
    private String getDestination(String message) {
        String[] data = message.split("\\$");
        if (data.length == 6)
            return data[4];
        return null;
    }

    public void updateNearby() {
        if (!nearbyPeople.isEmpty()) {
            List<BluetoothDevice> devices = new ArrayList<>(nearbyPeople.values());
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
        Iterator<SendModel> iterator = lostMessages.iterator();
        while (iterator.hasNext()) {
            SendModel model = iterator.next();
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
