package com.bm.messenger.bluetooth.gatt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import androidx.room.EmptyResultSetException;

import com.bm.messenger.database.Database;
import com.bm.messenger.model.MessageModel;
import com.bm.messenger.model.UserModel;
import com.bm.messenger.ui.fragment.NearbyFindListener;
import com.bm.messenger.utility.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class GATTManager implements GattHandler {

    private static final String TAG = "GattManager";
//    private static final String GATT_SERVICE_UUID = "4f00078c-8bfe-11eb-8dcd-0242ac130003";
//    private static final String CHARACTERISTIC_UUID = "4f00087c-8bfe-11eb-8dcd-0242ac130003";
//    private static final String DESCRIPTOR_UUID = "9ef8321a-8c7f-11eb-8dcd-0242ac130003";
    //    private static final String CHARACTERISTIC_USER_UUID = "4f00087c-8bfe-11eb-8dcd-0242ac130003";
//    private static final String CHARACTERISTIC_MESSAGE_UUID = "32da15e9-44e4-440b-b76c-f34fd3592ed2";
    //    private static final String DESCRIPTOR_USER_ID = "9ef8321a-8c7f-11eb-8dcd-0242ac130003";
//    private static final String DESCRIPTOR_USER_NAME = "42b158cf-2fd5-4280-9766-564408589620";
//    private static final String DESCRIPTOR_MESSAGE_ID = "9ef83490-8c7f-11eb-8dcd-0242ac130003";
//    private static final String DESCRIPTOR_MESSAGE_COUNTER = "f4510a73-da28-42bc-8380-416b6499433f";

    //    private static final String USER = "user";
//    private static final String MESSAGE = "message";
    private final BluetoothGattService service = new BluetoothGattService(UUID.fromString(GATT_SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);
    private final BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(WRITE_CHARACTERISTIC_UUID),
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
    );
    private final BluetoothGattCharacteristic readCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(READ_CHARACTERISTIC_UUID),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
    );
    private final BluetoothManager bluetoothManager;
    private final Database db;
    //    private final BluetoothGattCharacteristic characteristicUser = new BluetoothGattCharacteristic(UUID.fromString(CHARACTERISTIC_USER_UUID),
//            BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ
//    );
//    private final BluetoothGattCharacteristic characteristicMessage = new BluetoothGattCharacteristic(UUID.fromString(CHARACTERISTIC_MESSAGE_UUID),
//            BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ
//    );
//    private final BluetoothGattDescriptor descriptorUserId = new BluetoothGattDescriptor(UUID.fromString(DESCRIPTOR_USER_ID),
//            BluetoothGattDescriptor.PERMISSION_READ);
//    private final BluetoothGattDescriptor descriptorUserName = new BluetoothGattDescriptor(UUID.fromString(DESCRIPTOR_USER_NAME),
//            BluetoothGattDescriptor.PERMISSION_READ);
//    private final BluetoothGattDescriptor descriptorMessageId = new BluetoothGattDescriptor(UUID.fromString(DESCRIPTOR_MESSAGE_ID),
//            BluetoothGattDescriptor.PERMISSION_READ);
//    private final BluetoothGattDescriptor descriptorMessageCounter = new BluetoothGattDescriptor(UUID.fromString(DESCRIPTOR_MESSAGE_COUNTER),
//            BluetoothGattDescriptor.PERMISSION_READ);
    private final Set<BluetoothDevice> bluetoothDevices = new HashSet<>();
    private final Map<UserModel, BluetoothDevice> nearbyPeople = new HashMap<>();
    //    private final ArrayList<BluetoothDevice> pendingSync = new ArrayList<>();
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final List<String> unsentMessage = new ArrayList<>();
    private NearbyFindListener nearbyFindListener;
    private GATTServer server;
    private GATTClient client;
    private Context mContext;
    private UserModel currentUser;

    public GATTManager(Context context, BluetoothManager bluetoothManager) {
        mContext = context;
        this.bluetoothManager = bluetoothManager;
        db = new Database();
        getCurrentUser();
        init();
//        characteristicUser.addDescriptor(descriptorUserId);
//        characteristicUser.addDescriptor(descriptorUserName);
//        characteristicMessage.addDescriptor(descriptorMessageId);
//        characteristicMessage.addDescriptor(descriptorMessageCounter);
//        service.addCharacteristic(characteristicUser);
//        service.addCharacteristic(characteristicMessage);
    }

    private void getCurrentUser() {
        disposables.add(db.getDatabase(mContext).userDao().getCurrentUser()
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> {
                    readCharacteristic.setValue(user.toString());
                    currentUser = user;
                }, throwable -> {
                    if (throwable instanceof EmptyResultSetException) {
                        Thread.sleep(2000);
                        getCurrentUser();
                    }
                }));
    }

    private void init() {
        initClient();
        initServer();
    }

    public void addDevice(BluetoothDevice device) {
        if (bluetoothDevices.add(device)) {
            Log.d(TAG, "Device added: " + device.getAddress());
            client.getUserData(device);
        }
//            if (!server.addClient(device))
//                Utility.getToast(mContext, "Can't Add Client " + device.getName());
    }

    public void setNearbyFindListener(NearbyFindListener nearbyFind) {
        this.nearbyFindListener = nearbyFind;
    }

    private void initClient() {
        if (client == null)
            client = new GATTClient(mContext, bluetoothManager, this);
    }

    private void initServer() {
        if (server == null) {
            server = new GATTServer(mContext, bluetoothManager, this);
            if (!service.addCharacteristic(writeCharacteristic))
                Utility.getToast(mContext, "Can't Add WRITE to Host");
            if (!service.addCharacteristic(readCharacteristic))
                Utility.getToast(mContext, "Can't Add READ to Host");
            if (!server.addService(service))
                Utility.getToast(mContext, "Can't Add Service to Host");
        }
    }

    public void terminate() {
        disposables.dispose();
        client.terminate();
        server.terminate();
    }

    public void sendMessage(String message) {
        if (nearbyPeople.size() == 0) {
            if (bluetoothDevices.size() > 0 && !client.isReading())
                bluetoothDevices.clear();
            unsentMessage.add(message);
        } else
            client.sendMessage(new ArrayList<>(nearbyPeople.values()), message);
    }

//    private void Sync(BluetoothDevice device) {
//        if (currentUser != null) {
//            client.sendPacket(device, currentUser.toString());
//        } else
//            pendingSync.add(device);
//    }

//    private void getUserInfo(BluetoothDevice device) {
//
//    }

    private void removeNearby(BluetoothDevice device) {
        Iterator<Map.Entry<UserModel, BluetoothDevice>> iterator = nearbyPeople.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UserModel, BluetoothDevice> entry = iterator.next();
            if (device.equals(entry.getValue())) {
                iterator.remove();
            }
        }
        notifyNearbyChange();
    }

    @Override
    public void onStateChange(BluetoothDevice device, int state) {
        if (state == BluetoothProfile.STATE_CONNECTED) {
            if (bluetoothDevices.add(device))
                client.getUserData(device);
        } else if (state == BluetoothGatt.GATT_FAILURE || state == 133) {
            Log.d(TAG, "Device removed. state: " + state);
            bluetoothDevices.remove(device);
            removeNearby(device);
        }
    }

    @Override
    public void insertUser(BluetoothDevice device, String userData) {
        String[] userArray = userData.split("\\$");
        if (userArray.length == 2) {
            UserModel user = new UserModel(userArray[0], userArray[1]);
            disposables.add(db.getDatabase(mContext).userDao().insert(user).subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(l -> {
                        nearbyPeople.put(user, device);
                        notifyGattFind();
                        notifyNearbyChange();
                    }, throwable -> {
                        if (throwable instanceof SQLiteConstraintException) {
                            nearbyPeople.put(user, device);
                            notifyGattFind();
                            notifyNearbyChange();
                        }
                    }));
        }
    }

    private void notifyGattFind() {
        if (unsentMessage.size() > 0)
            client.sendMessage(new ArrayList<>(nearbyPeople.values()), unsentMessage);
        unsentMessage.clear();
    }

    private void notifyNearbyChange() {
        if (nearbyFindListener != null) {
            List<UserModel> nearby = new ArrayList<>(nearbyPeople.keySet());
            nearbyFindListener.nearbyList(nearby);
        }
    }
    //    public boolean connect(BluetoothDevice device) {
//        if (server == null)
//            startServer();
//        if (!bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER).contains(device)) {
//            boolean isConnect = server.connect(device, false);
//            boolean isConnect2 = device.connectGatt(mContext, false, new BluetoothGattCallback() {
//                @Override
//                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//                    super.onConnectionStateChange(gatt, status, newState);
//                    if (newState == BluetoothProfile.STATE_CONNECTED) {
////                        gatt.disconnect();
//                        gatt.close();
//                    }
//                }
//            }).connect();
//            if (isConnect)
//                introduction(device);
//            return isConnect;
//        } else
//            return true;
//    }

//    private void introduction(BluetoothDevice device) {
//        disposable = db.getDatabase(mContext).userDao().getMyself()
//                .subscribeOn(Schedulers.single())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(user -> {
////                    descriptorUserId.setValue(user.id.getBytes());
////                    descriptorUserName.setValue(user.name.getBytes());
//                    descriptor.setValue(USER.getBytes());
//                    characteristic.setValue(user.toString());
//                    server.notifyCharacteristicChanged(device, characteristic, false);
//                }, Throwable::printStackTrace);
//    }

//    private void sendMessage(MessageModel message) {
//        descriptor.setValue(MESSAGE.getBytes());
//        characteristic.setValue(message.toString());
//    }

    @Override
    public void onMessageReceive(String message) {
        final String[] data = message.split("\\$");
        MessageModel messageModel = null;
        if (data.length == 5) {
            messageModel = new MessageModel(data[0], data[2], data[3], data[1], data[2], Long.parseLong(data[4]));
        } else if (data.length == 4)
            messageModel = new MessageModel(data[0], null, data[2], data[1], null, Long.parseLong(data[3]));
        if (messageModel != null) {
            disposables.add(db.getDatabase(mContext).messageDao().insert(messageModel)
                    .subscribeOn(Schedulers.single())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(l -> {
                        if (!(data.length == 5 && data[2].equals(currentUser.id)))
                            sendMessage(message);
                    }, throwable -> {
                    }));

        }
//        for (String s : result)
//            message = message.concat(s + " ");
//        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    public List<UserModel> getNearby() {
        if (nearbyPeople.size() > 0) {
            return new ArrayList<>(nearbyPeople.keySet());
        }
        return new ArrayList<>();
    }

//    private final class PacketBuilder {
//
//        private BluetoothDevice device;
//
//        public PacketBuilder setPacket(String bDescriptor, String bCharacteristic) {
//            descriptor.setValue(bDescriptor.getBytes());
//            characteristic.setValue(bCharacteristic);
//            return this;
//        }
//
//        public PacketBuilder setDevice(BluetoothDevice device) {
//            this.device = device;
//            return this;
//        }
//
//        public void sendPacket() {
//            if (device != null)
//                client.sendPacket(device);
//        }
//    }

}
