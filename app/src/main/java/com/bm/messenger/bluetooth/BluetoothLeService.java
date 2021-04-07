//package com.bm.messenger.bluetooth;
//
//import android.app.Service;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothManager;
//import android.content.Context;
//import android.content.Intent;
//import android.os.Binder;
//import android.os.IBinder;
//
//import androidx.annotation.Nullable;
//
//public class BluetoothLeService extends Service {
//
//    private final Binder binder = new LocalBinder();
//    private BluetoothManager bluetoothManager;
//    private BluetoothAdapter bluetoothAdapter;
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return binder;
//    }
//
//    public boolean initialize() {
//        if (bluetoothManager == null) {
//            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//            if (bluetoothManager == null) {
//                return false;
//            }
//        }
//        bluetoothAdapter = bluetoothManager.getAdapter();
//        return bluetoothAdapter != null;
//    }
//
//    public BluetoothAdapter getAdapter() {
//        return bluetoothAdapter;
//    }
//
//    class LocalBinder extends Binder {
//        public BluetoothLeService getService() {
//            return BluetoothLeService.this;
//        }
//    }
//
//}
