package com.bm.messenger.utility;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.bm.messenger.R;

import java.util.Random;

import static android.content.Context.MODE_PRIVATE;

public final class Utility {

    private static final String CHAR_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBER = "0123456789";
    private static final String PREFERENCES = "ble-msg";

    private volatile static SharedPreferences sharedPreferences;

    public synchronized static SharedPreferences getSharedPreferences(Context context) {
        if (sharedPreferences == null)
            sharedPreferences = context.getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        return sharedPreferences;
    }

    public static String generateToken(int length, Random random) {
        char[] buf = new char[length];
        for (int i = 0; i < length; i++) {
            switch (random.nextInt(7)) {
                case 0:
                case 1:
                case 2:
                    buf[i] = CHAR_UPPER.charAt(random.nextInt(26));
                    break;
                case 3:
                case 4:
                case 5:
                    buf[i] = CHAR_LOWER.charAt(random.nextInt(26));
                    break;
                case 6:
                    buf[i] = NUMBER.charAt(random.nextInt(10));
                    break;
            }
        }
        return new String(buf);
    }

    public static boolean checkPermission(Context baseContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(baseContext,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED /*&& ContextCompat.checkSelfPermission(baseContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED*/;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(baseContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void getToast(Context context, String message) {
        Toast.makeText(context, context.getResources().getString(R.string.error) + message, Toast.LENGTH_LONG).show();
    }
}
