package com.bm.messenger.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.os.HandlerCompat;

import com.bm.messenger.bluetooth.gatt.GATTManager;
import com.bm.messenger.model.UserModel;
import com.bm.messenger.ui.fragment.interfaces.NearbyFindListener;
import com.bm.messenger.utility.Utility;

import java.util.ArrayList;
import java.util.List;

public class BluetoothManager extends ScanCallback {

    private static final String TAG = "BTManager";
    private static final long SCAN_PERIOD = 3000;
    private static final String ADVERTISE_UUID = "802ceca0-3af2-41b8-b6f9-fa7e1a794193";
    private static final int GATT_AND_GATT_SERVER = -1;
    private final ScanFilter scanFilter = new ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString(ADVERTISE_UUID))
            .build();
    private final ScanSettings scanSettings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build();
    //    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final Handler scanHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    private Context mContext;
    //    private SocketHandler socketHandler;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothAdapter bluetoothAdapter;
    //    private BluetoothGatt gatt;
//    private BluetoothGattServer gattServer;
    //    private BluetoothLeService bluetoothService;
    private BluetoothLeAdvertiser advertiser;
    //    private android.bluetooth.BluetoothManager bluetoothManager;
    private GATTManager gattManager;
    private boolean isScanning;
    private boolean isConnect;
    private volatile boolean isAdvertising = false;
    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Advertising Success");
            isAdvertising = true;
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d(TAG, "Advertising Failed");
            isAdvertising = false;
            terminate(BluetoothProfile.GATT_SERVER);
            switch (errorCode) {
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    Utility.makeToast(mContext, "BT Discovery Failed: " + "Large Data");
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    Utility.makeToast(mContext, "BT Discovery Failed: " + "No Resource Available");
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    Utility.makeToast(mContext, "BT Discovery Failed: " + "Internal Error");
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    Utility.makeToast(mContext, "BT Discovery Failed: " + "Unsupported");
                    break;
            }
        }
    };
    //    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
//
//    };
//    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
//            super.onConnectionStateChange(device, status, newState);
//        }
//    };
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        startAdvertising();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        stopAdvertising();
                        break;
                }
            }
        }
    };

    public BluetoothManager(Context context) {
        mContext = context;
        android.bluetooth.BluetoothManager bluetoothManager = (android.bluetooth.BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
//        socketHandler = new SocketHandler(mContext, bluetoothAdapter);
        gattManager = new GATTManager(mContext, this /*bluetoothManager,*/);
    }

    public BroadcastReceiver getBluetoothReceiver() {
        return bluetoothReceiver;
    }

    public BluetoothAdapter getAdapter() {
        return bluetoothAdapter;
    }

    public List<UserModel> getNearby() {
        return gattManager.getNearby();
    }

    public void setNearbyFindListener(NearbyFindListener listener) {
        gattManager.setNearbyFindListener(listener);
    }

    public void startAdvertising() {
        if (bluetoothAdapter.isEnabled() && !isAdvertising) {
            gattManager.init();
            try {
                advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
                AdvertiseSettings settings = new AdvertiseSettings.Builder()
                        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                        .setConnectable(true)
                        .build();
                AdvertiseData data = new AdvertiseData.Builder()
                        .addServiceUuid(ParcelUuid.fromString(ADVERTISE_UUID))
                        .setIncludeDeviceName(false)
                        .setIncludeTxPowerLevel(false)
                        .build();
                advertiser.startAdvertising(settings, data, advertiseCallback);
            } catch (Exception e) {
                e.printStackTrace();
                if (advertiser == null) {
                    Utility.makeToast(mContext, "Your Phone cannot Advertise!");
                }
                Utility.makeToast(mContext, "Failed to Enable Bluetooth Discovery");
            }
        }
    }

    public void startScanning() {
        if (Utility.checkPermission(mContext)) {
            if (bluetoothLeScanner == null)
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            ArrayList<ScanFilter> scanFilters = new ArrayList<>();
            scanFilters.add(scanFilter);
            if (bluetoothLeScanner != null && bluetoothAdapter.isEnabled() && !isConnect) {
                if (!isScanning) {
                    isScanning = true;
                    gattManager.clearDevices();
                    scanHandler.postDelayed(this::stopScanning, SCAN_PERIOD);
                    bluetoothLeScanner.startScan(scanFilters, scanSettings, this);
                }
            }
        }
    }

    public void stopAdvertising() {
        if (advertiser != null && isAdvertising) {
            isAdvertising = false;
            advertiser.stopAdvertising(advertiseCallback);
            stopScanning();
            terminate(GATT_AND_GATT_SERVER);
        }
//        if (bluetoothAdapter.isDiscovering())
//            bluetoothAdapter.cancelDiscovery();
    }

    private void stopScanning() {
        if (!isScanning)
            return;
        if (bluetoothLeScanner != null && bluetoothAdapter.isEnabled())
            bluetoothLeScanner.stopScan(this);
        isScanning = false;
        gattManager.updateDevices();
    }

    public void sendMessage(String message) {
        gattManager.sendMessage(message, null);
    }

//    private ExecutorService getExecutor() {
//        return executorService;
//    }

//    public void stopService() {
////        getExecutor().shutdown();
//        gattManager.terminate();
//    }

//    public void notifyDataSetChanged() {
//        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(bluetoothDevices.get(bluetoothDevices.size() - 1).getAddress());
//        if (!gattManager.connect(device))
//            bluetoothDevices.remove(device);
//    }

//    @Override
//    public void startAdvertising(boolean isConnectable) {
//        if (isAdvertising) {
//            if (this.isConnectable != isConnectable) {
//                stopAdvertise();
//                startAdvertise(isConnectable);
//            }
//        } else
//            startAdvertise(isConnectable);
//    }
//
//    @Override
//    public void stopAdvertising() {
//        stopAdvertise();
//    }

    private void terminate(int gattType) {
        switch (gattType) {
            case BluetoothProfile.GATT:
                gattManager.terminateClient();
                break;
            case BluetoothProfile.GATT_SERVER:
                gattManager.terminateServer();
                break;
            case GATT_AND_GATT_SERVER:
                gattManager.terminateClient();
                gattManager.terminateServer();
                break;
        }
    }

    public void destroy() {
        gattManager.destroy();
    }

    public void onStateChange(int state) {
        if (state == BluetoothProfile.STATE_CONNECTING && !isConnect) {
            isConnect = true;
            stopScanning();
        } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
            isConnect = false;
        }
    }

//    @Override
//    public void onServiceConnected(ComponentName name, IBinder service) {
//        bluetoothService = ((BluetoothLeService.LocalBinder) service).getService();
//        if (bluetoothService != null) {
//            if (!bluetoothService.initialize()) {
//                ((Activity) mContext).finish();
//            } else {
//                bluetoothAdapter = bluetoothService.getAdapter();
//                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
//            }
//        }
//    }
//
//    @Override
//    public void onServiceDisconnected(ComponentName name) {
//        bluetoothService = null;
//    }

//    public void sendMessage() {
//        gattServer.notifyCharacteristicChanged();
//    }

//    @Override
//    public void onReceive(Context context, Intent intent) {
//        String action = intent.getAction();
//        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//            if (device.getName().equals("sEc"))
//                socketHandler.addDevice(device);
//            String deviceName = device.getName();
//            String deviceHardwareAddress = device.getAddress();
//        }
//    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);
//        Log.d(TAG, "UUID: " + result.getScanRecord().getServiceUuids().get(0) + " | " + result.getDevice().getAddress());
//        socketHandler.addDevice(result.getDevice());
        gattManager.addDevice(result.getDevice());
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
        Utility.makeToast(mContext, "Bluetooth Scan Failed");
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        super.onBatchScanResults(results);
        for (ScanResult result : results)
//            socketHandler.addDevice(result.getDevice());
            gattManager.addDevice(result.getDevice());
    }
}
