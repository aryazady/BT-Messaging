package com.bm.messenger.ui.fragment;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bm.messenger.R;
import com.bm.messenger.databinding.FragmentNavBarBinding;
import com.bm.messenger.model.LiveDataModel;
import com.bm.messenger.utility.SharedViewModel;

public class NavBarFragment extends Fragment implements View.OnClickListener {

    //    private Context mContext;
    private int currPage;
    private FragmentNavBarBinding binding;
    private SharedViewModel sharedViewModel;

    public NavBarFragment() {
        super(R.layout.fragment_nav_bar);
    }
//
//    @Override
//    public void onAttach(@NonNull Context context) {
//        super.onAttach(context);
//        mContext = context;
//    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNavBarBinding.inflate(inflater, container, false);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        sharedViewModel.getData().observe(getViewLifecycleOwner(), this::handleLiveData);
        currPage = LiveDataModel.HOME;
        binding.navBarIcGroup.setImageDrawable(getInactiveIcon(R.drawable.ic_broadcast));
        binding.navBarIcHome.setImageDrawable(getActiveIcon(R.drawable.ic_chats));
        binding.navBarIcHome.setOnClickListener(this);
        binding.navBarIcGroup.setOnClickListener(this);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void handleLiveData(LiveDataModel dataModel) {
        if (currPage != dataModel.getCurrPage())
            changePage(dataModel.getCurrPage());
    }

    private Drawable getInactiveIcon(int id) {
        VectorDrawable icGroup = (VectorDrawable) ContextCompat.getDrawable(getContext(), id);
        icGroup.setTint(getResources().getColor(R.color.inactive_btn, null));
        return icGroup;
    }

    private Drawable getActiveIcon(int drawableID) {
        VectorDrawable icGroup = (VectorDrawable) ContextCompat.getDrawable(getContext(), drawableID);
        icGroup.setTint(getResources().getColor(R.color.active, null));
        return icGroup;
    }

    public void changePage(int page) {
        if (page == LiveDataModel.HOME) {
            this.currPage = LiveDataModel.HOME;
            binding.navBarIcHome.setImageDrawable(getActiveIcon(R.drawable.ic_chats));
            binding.navBarIcGroup.setImageDrawable(getInactiveIcon(R.drawable.ic_broadcast));
        } else if (page == LiveDataModel.BROADCAST) {
            this.currPage = LiveDataModel.BROADCAST;
            binding.navBarIcHome.setImageDrawable(getInactiveIcon(R.drawable.ic_chats));
            binding.navBarIcGroup.setImageDrawable(getActiveIcon(R.drawable.ic_broadcast));
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.nav_bar_ic_home:
                if (currPage == LiveDataModel.BROADCAST) {
                    changePage(LiveDataModel.HOME);
                    sharedViewModel.setData(new LiveDataModel(getString(R.string.home), LiveDataModel.BROADCAST, LiveDataModel.HOME));
                }
                break;
            case R.id.nav_bar_ic_group:
                if (currPage == LiveDataModel.HOME) {
                    changePage(LiveDataModel.BROADCAST);
                    sharedViewModel.setData(new LiveDataModel(getString(R.string.broadcast), LiveDataModel.HOME, LiveDataModel.BROADCAST));
                }
                break;
        }
    }
}
