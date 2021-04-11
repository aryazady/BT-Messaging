package com.bm.messenger.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import com.bm.messenger.R;
import com.bm.messenger.database.Database;
import com.bm.messenger.databinding.ActivityLauncherBinding;
import com.bm.messenger.model.UserModel;
import com.bm.messenger.utility.Utility;

import java.util.Random;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class LauncherActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int SPLASH_TIME = 2500;
    private final Handler fadeoutHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    private final Handler splashHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    private ActivityLauncherBinding binding;
    private float alpha = 1f;
    private Disposable disposable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLauncherBinding.inflate(getLayoutInflater());
        View layout = binding.getRoot();
        binding.btSubmit.setOnClickListener(this);
        binding.btSubmit.setEnabled(false);
        binding.etChooseName.setEnabled(false);
        setContentView(layout);
    }

    @Override
    protected void onStart() {
        super.onStart();
        init();
    }

    @Override
    protected void onStop() {
        super.onStop();
        splashHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fadeoutHandler.removeCallbacksAndMessages(null);
        if (disposable != null) {
            disposable.dispose();
        }
        binding = null;
    }

    private void init() {
        splashHandler.postDelayed(this::splashEnd, SPLASH_TIME);
    }

    private void splashEnd() {
        if (hasPubId())
            startMainActivity();
        else {
            new Runnable() {
                @Override
                public void run() {
                    if (alpha > 0) {
                        alpha -= 0.01f;
                        if (binding != null) {
                            binding.ivSplash.setAlpha(alpha);
                            fadeoutHandler.postDelayed(this, 2);
                        }
                    } else {
                        binding.ivSplash.setVisibility(View.GONE);
                        binding.btSubmit.setEnabled(true);
                        binding.etChooseName.setEnabled(true);
                    }
                }
            }.run();
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
        startActivity(intent);
        finish();
    }

    private boolean hasPubId() {
        String pubId = Utility.getSharedPreferences(this).getString(getString(R.string.preference_user_id), null);
        return pubId != null;
    }

    private String generatePubId() {
        String pubId = Utility.generateToken(10, new Random());
        SharedPreferences.Editor editor = Utility.getSharedPreferences(this).edit();
        editor.putString(getString(R.string.preference_user_id), pubId);
        editor.apply();
        return pubId;
    }

    private void generateUser(String name) {
        String pubId = generatePubId();
        UserModel userModel = new UserModel(pubId, name);
        Database db = new Database();
        disposable = db.getDatabase(getApplicationContext()).userDao().insert(userModel)
                .subscribeOn(Schedulers.single())
                .observeOn(Schedulers.single())
                .subscribe(longs -> startMainActivity(), throwable -> {
                    Utility.makeToast(this, throwable.toString());
                    binding.pbLoading.setVisibility(View.GONE);
                    binding.btSubmit.setVisibility(View.VISIBLE);
                });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bt_submit) {
            String name = binding.etChooseName.getText().toString().trim();
            if (name.matches(""))
                Utility.makeToast(this, "Name cannot be empty");
            else {
                binding.btSubmit.setVisibility(View.GONE);
                binding.pbLoading.setVisibility(View.VISIBLE);
                SharedPreferences.Editor editor = Utility.getSharedPreferences(this).edit();
                editor.putString(getString(R.string.preference_user_name), name);
                editor.apply();
                generateUser(name);
            }
        }
    }
}
