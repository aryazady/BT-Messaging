package com.bm.messenger.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bm.messenger.R;
import com.bm.messenger.bluetooth.BluetoothManager;
import com.bm.messenger.database.Database;
import com.bm.messenger.databinding.ActivityMainBinding;
import com.bm.messenger.model.LiveDataModel;
import com.bm.messenger.model.MessageModel;
import com.bm.messenger.model.UserModel;
import com.bm.messenger.ui.fragment.BroadcastPageFragment;
import com.bm.messenger.ui.fragment.CautionPageFragment;
import com.bm.messenger.ui.fragment.ConversationFragment;
import com.bm.messenger.ui.fragment.HistoryPageFragment;
import com.bm.messenger.ui.fragment.NavBarFragment;
import com.bm.messenger.ui.fragment.ToolbarFragment;
import com.bm.messenger.ui.fragment.navigation.NavigationManager;
import com.bm.messenger.utility.SharedViewModel;
import com.bm.messenger.utility.Utility;

import java.util.List;
import java.util.Random;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

//import com.bm.messenger.ui.fragment.TypeAreaFragment;

public class MainActivity extends AppCompatActivity implements MessageHandler {
//    private final ActivityLauncher<Intent, ActivityResult> activityLauncher = ActivityLauncher.registerActivityForResult(this);

    public static final int FINE_LOCATION_PERMISSION = 120;
    public static final int COARSE_LOCATION_PERMISSION = 121;
    private static final String TAG = "MainActivity";
    final Handler blScanHandler = HandlerCompat.createAsync(Looper.myLooper());
    //    private static final String CHATS = "ChatModel";
//    private static final String BROADCAST = "Broadcast";
//    private static final String CAUTION = "Caution";
//    private static final int WITH_TRANSACTION = 1;
//    private static final int WITHOUT_TRANSACTION = 2;
    //    private Random random;
    //    public static final String IS_BLUETOOTH_ENABLE = "isEnable";
    private BluetoothManager bluetoothManager;
    private ActivityMainBinding binding;
    private NavigationManager navigationManager;
    //    private UserDao userDao;
//    private MessageDao messageDao;
    //    private AHBottomNavigationItem home, social;
    private SharedViewModel sharedViewModel;
    private LiveDataModel liveDataModel;
    private String pubId;
    private Database db;
    private Runnable runnable;
//    private boolean isForeground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View layout = binding.getRoot();
        setContentView(layout);
//        activityLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE));
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        isForeground = true;
        bluetoothManager.startAdvertising();
        registerBluetoothReceiver();
        scanBleNearby();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        isForeground = false;
        blScanHandler.removeCallbacksAndMessages(null);
        bluetoothManager.stopAdvertising();
        unregisterReceiver(bluetoothManager.getBluetoothReceiver());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.closeDatabase();
        binding = null;
        bluetoothManager.dispose();
//        unregisterReceiver(bluetoothManager);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            locationStatusCheck();
    }

    @Override
    public void onBackPressed() {
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        if (liveDataModel == null ||
                liveDataModel.getCurrPage() == LiveDataModel.HISTORY ||
                liveDataModel.getCurrPage() == LiveDataModel.CAUTION)
            super.onBackPressed();
        for (Fragment fragment : fragmentList)
            if (fragment instanceof BroadcastPageFragment) {
                ((BroadcastPageFragment) fragment).onBackPressed();
                return;
            } else if (fragment instanceof ConversationFragment) {
                ((ConversationFragment) fragment).onBackPressed();
                return;
            }
    }

    private void init() {
        initVar();
        checkPubId();
        doTestHere();
        checkBluetooth();
//        initNavBar();
        if (bluetoothManager.getAdapter() != null) {
            if (!Utility.checkPermission(getBaseContext()))
                getPermission();
            else
                locationStatusCheck();
            initView();
        }
    }

    @SuppressLint("all")
    private void doTestHere() {
        db.getDatabase(getApplicationContext()).userDao()
                .insert(new UserModel("1", "Arya"),
                        new UserModel("3", "Sam"),
                        new UserModel("4", "Jack"),
                        new UserModel("5", "Sarah"),
                        new UserModel("6", "Jennifer"),
                        new UserModel("2", "Tom"))
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(l -> l.size(), Throwable::printStackTrace);
//        Date date = new Date(System.currentTimeMillis());
//        Calendar.getInstance().get(Calendar.)
//        LocalDateTime a = LocalDateTime.now();
//        a.get
        long timestamp = System.currentTimeMillis() / 1000;
        db.getDatabase(getApplicationContext()).messageDao()
                .insert(new MessageModel("100", "2", "content1", "2", "1", timestamp++),
                        new MessageModel("102", "2", "content2", "2", "1", timestamp++),
                        new MessageModel("103", "2", "content3", "2", "1", timestamp++),
                        new MessageModel("104", "2", "content4", "1", "2", timestamp++),
                        new MessageModel("105", "3", "content5", "1", "3", timestamp++),
                        new MessageModel("106", "3", "content6", "3", "1", timestamp++),
                        new MessageModel("107", "3", "content7", "3", "1", timestamp++),
                        new MessageModel("108", "3", "content8", "1", "3", timestamp++),
                        new MessageModel("109", "3", "content9", "3", "1", timestamp++),
                        new MessageModel("110", "3", "content10", "3", "1", timestamp++),
                        new MessageModel("111", "3", "content11", "1", "3", timestamp++),
                        new MessageModel("112", "4", "content12", "4", "1", timestamp++),
                        new MessageModel("113", "4", "content13", "1", "4", timestamp++),
                        new MessageModel("114", "4", "content14", "4", "1", timestamp++),
                        new MessageModel("115", "5", "content15", "5", "1", timestamp++),
                        new MessageModel("116", "5", "content16", "5", "1", timestamp++),
                        new MessageModel("117", "5", "content17", "5", "1", timestamp++),
                        new MessageModel("118", "6", "content18", "6", "1", timestamp++),
                        new MessageModel("119", "6", "content19", "6", "1", timestamp++),
                        new MessageModel("120", "6", "content20", "6", "1", timestamp++),
                        new MessageModel("121", null, "Broadcast1", "6", null, timestamp++),
                        new MessageModel("122", null, "Broadcast2", "6", null, timestamp++),
                        new MessageModel("123", null, "Broadcast3", "6", null, timestamp++),
                        new MessageModel("124", null, "Broadcast4", "6", null, timestamp++),
                        new MessageModel("125", null, "Broadcast5", "6", null, timestamp++),
                        new MessageModel("126", null, "Broadcast6", "6", null, timestamp))
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(l -> {
                    l.size();
                }, throwable -> {
                    throwable.printStackTrace();
                });
    }

    private void initVar() {
//        home = new AHBottomNavigationItem(getString(R.string.home), R.drawable.ic_home_emp);
//        social = new AHBottomNavigationItem(getString(R.string.social), R.drawable.ic_group_emp);
        bluetoothManager = new BluetoothManager(getApplicationContext());
        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
        sharedViewModel.getData().observe(this, this::handleLiveData);
        navigationManager = new NavigationManager(getSupportFragmentManager());
        db = new Database();
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        registerReceiver(bluetoothManager, filter);
//        random = new Random();
    }

    private void registerBluetoothReceiver() {
        IntentFilter bluetoothChange = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothManager.getBluetoothReceiver(), bluetoothChange);
    }

    private void scanBleNearby() {
        runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "request to scan");
                bluetoothManager.startScanning();
                blScanHandler.postDelayed(this, 15000);
            }
        };
        runnable.run();
    }

    private void checkBluetooth() {
        if (bluetoothManager.getAdapter() == null) {
            Utility.getToast(this, "Your Phone Doesn't Support Bluetooth. Delete This App");
            finishAndRemoveTask();
        } else {
//            Bridgefy.initialize(getApplicationContext(), new RegistrationListener() {
//                @Override
//                public void onRegistrationSuccessful(BridgefyClient bridgefyClient) {
//                    super.onRegistrationSuccessful(bridgefyClient);
//                }
//
//                @Override
//                public void onRegistrationFailed(int errorCode, String message) {
//                    super.onRegistrationFailed(errorCode, message);
//                }
//            });
//            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//            bindService(gattServiceIntent, bluetoothManager, Context.BIND_AUTO_CREATE);
        }
    }

   /* private void initNavBar() {
        binding.fragmentBottomNav.setVisibility(View.VISIBLE);
        sharedViewModel.setData(new LiveDataModel(CHATS, LiveDataModel.CHANGE_PAGE));
        binding.bottomNav.addItem(home);
        binding.bottomNav.addItem(social);
        binding.bottomNav.setDefaultBackgroundColor(Color.parseColor("#6200EE"));
        binding.bottomNav.setAccentColor(Color.parseColor("#FFFFFF"));
        binding.bottomNav.setInactiveColor(Color.parseColor("#FF808080"));
        binding.bottomNav.setTitleState(AHBottomNavigation.TitleState.ALWAYS_HIDE);
        binding.bottomNav.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
                if (!wasSelected)
                    if (position == 0)
                        sharedViewModel.setMessage(getString(R.string.home));
                    else
                        sharedViewModel.setMessage(getString(R.string.social));
                return true;
            }
        });
        binding.bottomNav.setCurrentItem(0);
    }*/

    private void initView() {
        navigationManager.fragmentTransaction(R.id.fragment_toolbar, new ToolbarFragment(), liveDataModel);
        navigationManager.fragmentTransaction(R.id.fragment_bottom_bar, new NavBarFragment(), liveDataModel);
        if (bluetoothManager.getAdapter().isEnabled()) {
            navigationManager.fragmentTransaction(R.id.fragment_container, new HistoryPageFragment(db.getDatabase(getApplicationContext()), bluetoothManager), liveDataModel);
            binding.fragmentBottomBar.setVisibility(View.VISIBLE);
        } else
            navigationManager.fragmentTransaction(R.id.fragment_container, new CautionPageFragment(), liveDataModel);
//        ft.add(R.id.fragment_container, new CautionPageFragment());
//        else
//            initNavBar();
    }

    private void checkPubId() {
        String pubId = Utility.getSharedPreferences(this).getString(getString(R.string.preference_pub_id), null);
        if (pubId == null)
            generatePubId();
        else
            this.pubId = pubId;
    }

    private void handleLiveData(LiveDataModel data) {
        liveDataModel = data;
        if (binding.fragmentBottomBar.getVisibility() != View.VISIBLE && liveDataModel.getNextPage() != LiveDataModel.NONE)
            binding.fragmentBottomBar.setVisibility(View.VISIBLE);
        switch (liveDataModel.getNextPage()) {
            case LiveDataModel.BROADCAST:
            case LiveDataModel.HISTORY:
//                bluetoothManager.startAdvertising();
//                getSupportFragmentManager().popBackStack();
                selectPage();
//                changeBottomBar();
                break;
            case LiveDataModel.CONVERSATION:
                binding.fragmentBottomBar.setVisibility(View.GONE);
                selectPage();
//                changeBottomBar();
                break;
        }
        liveDataModel.updateCurrPage();
    }

    private void selectPage() {
//        if (isAnimated == WITH_TRANSACTION) {
        switch (liveDataModel.getNextPage()) {
            case LiveDataModel.HISTORY:
                navigationManager.fragmentTransaction(R.id.fragment_container,
                        new HistoryPageFragment(db.getDatabase(getApplicationContext()), bluetoothManager),
                        liveDataModel);
//                    ft.setCustomAnimations(R.anim.slide_in_ltr, R.anim.slide_out_ltr);
//                    ft.replace(R.id.fragment_container, new HistoryPageFragment(bluetoothAdapter, getDatabase()));
                break;
            case LiveDataModel.BROADCAST:
                navigationManager.fragmentTransaction(R.id.fragment_container, new BroadcastPageFragment(db.getDatabase(getApplicationContext()), pubId, this), liveDataModel);
//                    ft.setCustomAnimations(R.anim.slide_in_rtl, R.anim.slide_out_rtl);
//                    ft.replace(R.id.fragment_container, new BroadcastPageFragment());
                break;
            case LiveDataModel.CONVERSATION:
                navigationManager.fragmentTransaction(R.id.fragment_container, new ConversationFragment(db.getDatabase(getApplicationContext()), this), liveDataModel);
                break;
        }
//        } else
//            switch (title) {
//                case CHATS:
//                    ft.add(R.id.fragment_container, new HistoryPageFragment(bluetoothAdapter, getDatabase()));
//                    break;
//                case BROADCAST:
//                    ft.add(R.id.fragment_container, new BroadcastPageFragment());
//                case CAUTION:
//                    ft.add(R.id.fragment_container, new CautionPageFragment());
//                    break;
//            }
//        ft.commit();
    }

//    private void changeBottomBar() {
//        if (liveDataModel.getNextPage() == LiveDataModel.HISTORY)
//            navigationManager.fragmentTransaction(R.id.fragment_bottom_bar, new NavBarFragment(), null);
//        else
//            navigationManager.fragmentTransaction(R.id.fragment_bottom_bar, new TypeAreaFragment(), null);
//    }

    private void generatePubId() {
        String pubId = Utility.generateToken(24, new Random());
        SharedPreferences.Editor editor = Utility.getSharedPreferences(this).edit();
        editor.putString(getString(R.string.preference_pub_id), "1");
        editor.apply();
        this.pubId = pubId;
    }

    private void locationStatusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            alertNoLocation();
        }
    }

    private void alertNoLocation() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("No", (dialog, id) -> dialog.cancel());
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION/*, Manifest.permission.ACCESS_COARSE_LOCATION*/},
                    FINE_LOCATION_PERMISSION);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    COARSE_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onMessageSent(MessageModel message) {
        bluetoothManager.sendMessage(message.toString());
    }
}