package com.bm.messenger.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bm.messenger.R;
import com.bm.messenger.databinding.FragmentToolbarBinding;
import com.bm.messenger.model.LiveDataModel;
import com.bm.messenger.utility.SharedViewModel;

public class ToolbarFragment extends Fragment {

    private FragmentToolbarBinding binding;

    public ToolbarFragment() {
        super(R.layout.fragment_toolbar);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentToolbarBinding.inflate(inflater, container, false);
        binding.ivBack.setOnClickListener(v -> getActivity().onBackPressed());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedViewModel model = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        model.getData().observe(getViewLifecycleOwner(), data -> {
            switch (data.getNextPage()) {
                case LiveDataModel.BROADCAST:
                case LiveDataModel.HOME:
                    binding.ivBack.setVisibility(View.GONE);
                    binding.toolbarTitle.setText(data.getTitle());
                    break;
                case LiveDataModel.CONVERSATION:
                    binding.ivBack.setVisibility(View.VISIBLE);
                    binding.toolbarTitle.setText(data.getTitle());
                    break;
                case LiveDataModel.NONE:
                    binding.toolbarTitle.setText(data.getTitle());
                    break;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
