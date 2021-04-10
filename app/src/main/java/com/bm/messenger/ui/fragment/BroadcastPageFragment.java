package com.bm.messenger.ui.fragment;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bm.messenger.R;
import com.bm.messenger.adapter.ChatAdapter;
import com.bm.messenger.database.LocalDatabase;
import com.bm.messenger.databinding.FragmentBroadcastBinding;
import com.bm.messenger.model.ChatModel;
import com.bm.messenger.model.LiveDataModel;
import com.bm.messenger.model.MessageModel;
import com.bm.messenger.ui.activity.interfaces.MessageHandler;
import com.bm.messenger.ui.fragment.interfaces.BackPressHandler;
import com.bm.messenger.utility.SharedViewModel;
import com.bm.messenger.utility.Utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class BroadcastPageFragment extends Fragment implements BackPressHandler, View.OnClickListener {

    private final CompositeDisposable disposables = new CompositeDisposable();
    private FragmentBroadcastBinding binding;
    private ChatAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private LocalDatabase db;
    private List<ChatModel> broadcasts;
    private SharedViewModel sharedViewModel;
    private String pubId;
    private MessageHandler messageHandler;

    public BroadcastPageFragment(LocalDatabase db, String pubId, MessageHandler messageHandler) {
        super(R.layout.fragment_broadcast);
        this.db = db;
        this.messageHandler = messageHandler;
        broadcasts = new ArrayList<>();
        this.pubId = pubId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBroadcastBinding.inflate(inflater, container, false);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        mLayoutManager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, true);
        mAdapter = new ChatAdapter(broadcasts, pubId);
        binding.rvBroadcast.setLayoutManager(mLayoutManager);
        binding.rvBroadcast.setAdapter(mAdapter);
        binding.ibSendMessageBroadcast.setOnClickListener(this);
//        binding.etTypeLayoutBroadcast.addTextChangedListener(this);
        getMessage();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        disposables.dispose();
    }

    @Override
    public void onBackPressed() {
        sharedViewModel.setData(new LiveDataModel(getString(R.string.home), LiveDataModel.BROADCAST, LiveDataModel.HOME));
    }

//    @Override
//    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//    }
//
//    @Override
//    public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//    }
//
//    @Override
//    public void afterTextChanged(Editable s) {
//        binding.llTypeLayoutFragmentBroadcast.invalidate();
//    }

    private void getMessage() {
        disposables.add(db.userMessageJoinDao().getBroadcastMessages()
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(chats -> {
                    broadcasts.clear();
                    broadcasts.addAll(chats);
                    Collections.sort(broadcasts);
                    mAdapter.notifyDataSetChanged();
                }));
    }

    private void createMessage(String content) {
        String token = Utility.generateToken(16, new Random());
        final MessageModel message = new MessageModel(token, null, content, pubId, null, System.currentTimeMillis() / 1000);
        disposables.add(db.messageDao().insert(message)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> messageHandler.onMessageSent(message), throwable -> {
                    if (throwable instanceof SQLiteConstraintException)
                        recreateMessage(message);
                }));
    }

    private void recreateMessage(MessageModel message) {
        String token = Utility.generateToken(16, new Random());
        message.setId(token);
        disposables.add(db.messageDao().insert(message)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> messageHandler.onMessageSent(message), throwable -> {
                    if (throwable instanceof Exception)
                        recreateMessage(message);
                }));
    }

    @Override
    public void onClick(View v) {
        String message = binding.etTypeLayoutBroadcast.getText().toString().trim();
        if (!message.equals("")) {
            createMessage(message);
            binding.etTypeLayoutBroadcast.setText("");
        }
    }
}
