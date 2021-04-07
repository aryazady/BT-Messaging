package com.bm.messenger.ui.fragment;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bm.messenger.R;
import com.bm.messenger.databinding.FragmentCautionBinding;
import com.bm.messenger.model.LiveDataModel;
import com.bm.messenger.utility.ActivityLauncher;
import com.bm.messenger.utility.SharedViewModel;

public class CautionPageFragment extends Fragment implements View.OnClickListener {

    private final ActivityLauncher<Intent, ActivityResult> activityLauncher = ActivityLauncher.registerActivityForResult(this);
    private FragmentCautionBinding binding;
    private SharedViewModel sharedViewModel;


    public CautionPageFragment() {
        super(R.layout.fragment_caution);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCautionBinding.inflate(inflater, container, false);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        sharedViewModel.setData(new LiveDataModel(getString(R.string.caution), LiveDataModel.CAUTION));
        binding.btnTurnOnBluetooth.setOnClickListener(this);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_turn_on_bluetooth) {
            binding.btnTurnOnBluetooth.setVisibility(View.GONE);
            binding.pbTurnOnBluetooth.setVisibility(View.VISIBLE);
            turnBluetoothOn();
        }
    }

    private void turnBluetoothOn() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activityLauncher.launch(enableBtIntent, result -> {
            if (result.getResultCode() == Activity.RESULT_CANCELED) {
                binding.btnTurnOnBluetooth.setVisibility(View.VISIBLE);
                binding.pbTurnOnBluetooth.setVisibility(View.GONE);
            } else {
                sharedViewModel.setData(new LiveDataModel(getString(R.string.history), LiveDataModel.CAUTION, LiveDataModel.HISTORY));
            }
        });
    }
}
