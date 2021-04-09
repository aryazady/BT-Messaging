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
import com.bm.messenger.adapter.ChatRecycleAdapter;
import com.bm.messenger.database.LocalDatabase;
import com.bm.messenger.databinding.FragmentConversationBinding;
import com.bm.messenger.model.ChatModel;
import com.bm.messenger.model.LiveDataModel;
import com.bm.messenger.model.MessageModel;
import com.bm.messenger.ui.activity.MessageHandler;
import com.bm.messenger.ui.fragment.interfaces.BackPressHandler;
import com.bm.messenger.utility.SharedViewModel;
import com.bm.messenger.utility.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ConversationFragment extends Fragment implements BackPressHandler, View.OnClickListener {

    private final CompositeDisposable disposables = new CompositeDisposable();
    private FragmentConversationBinding binding;
    private LocalDatabase db;
    private List<ChatModel> conversationChats;
    private ChatRecycleAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private SharedViewModel sharedViewModel;
    private String conversationId;
    private String pubId;
    private MessageHandler messageHandler;

    public ConversationFragment(LocalDatabase db, MessageHandler messageHandler) {
        super(R.layout.fragment_conversation);
        this.db = db;
        this.messageHandler = messageHandler;
        conversationChats = new ArrayList<>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        mLayoutManager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, true);
        pubId = Utility.getSharedPreferences(getContext()).getString(getString(R.string.preference_pub_id), "");
        mAdapter = new ChatRecycleAdapter(conversationChats, pubId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        sharedViewModel.getData().observe(getViewLifecycleOwner(), this::handleLiveData);
        binding = FragmentConversationBinding.inflate(inflater, container, false);
        binding.rvConversation.setLayoutManager(mLayoutManager);
        binding.rvConversation.setAdapter(mAdapter);
        binding.ibSendMessageConversation.setOnClickListener(this);
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
        sharedViewModel.setData(new LiveDataModel(getString(R.string.history), LiveDataModel.CONVERSATION, LiveDataModel.HISTORY));
    }

    private void handleLiveData(LiveDataModel data) {
            if (data.getNextPage() == LiveDataModel.CONVERSATION) {
                conversationId = data.getData()[0];
                getConversationChats(conversationId);
            }
    }

    private void getConversationChats(String conversationId) {
        disposables.add(db.userMessageJoinDao().getConversationChats(conversationId)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(conversations -> {
                    conversationChats.clear();
                    conversationChats.addAll(conversations);
                    mAdapter.notifyDataSetChanged();
                }));
    }

    private void createMessage(String content) {
        String token = Utility.generateToken(20, new Random());
        final MessageModel message = new MessageModel(token, conversationId, content, pubId, conversationId, System.currentTimeMillis() / 1000);
        disposables.add(db.messageDao().insert(message)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> messageHandler.onMessageSent(message), throwable -> {
                    if (throwable instanceof SQLiteConstraintException)
                        recreateMessage(message);
                }));
    }

    private void recreateMessage(MessageModel message) {
        String token = Utility.generateToken(20, new Random());
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
        String message = binding.etTypeLayoutConversation.getText().toString().trim();
        if (!message.equals("")) {
            createMessage(message);
            binding.etTypeLayoutConversation.setText("");
        }
    }
}
