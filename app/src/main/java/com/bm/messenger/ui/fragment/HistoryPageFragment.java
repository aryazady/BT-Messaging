package com.bm.messenger.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bm.messenger.R;
import com.bm.messenger.adapter.AdapterOnClickListener;
import com.bm.messenger.adapter.HistoryPageAdapter;
import com.bm.messenger.adapter.NearbyAdapter;
import com.bm.messenger.bluetooth.BluetoothManager;
import com.bm.messenger.database.LocalDatabase;
import com.bm.messenger.databinding.FragmentHistoryChatBinding;
import com.bm.messenger.model.ChatModel;
import com.bm.messenger.model.LiveDataModel;
import com.bm.messenger.model.UserModel;
import com.bm.messenger.ui.fragment.interfaces.NearbyFindListener;
import com.bm.messenger.utility.SharedViewModel;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class HistoryPageFragment extends Fragment implements AdapterOnClickListener, NearbyFindListener {

    public static final int HISTORY = 101;
    public static final int NEARBY = 202;
    private final LocalDatabase db;
    private final List<ChatModel> historyChat = new ArrayList<>();
    private final List<UserModel> nearbyPeople = new ArrayList<>();
    private final Handler refreshNearbyHandler = HandlerCompat.createAsync(Looper.myLooper());
    private HistoryPageAdapter historyAdapter;
    private NearbyAdapter nearbyAdapter;
    private LinearLayoutManager historyLayoutManager, nearbyLayoutManager;
    private FragmentHistoryChatBinding binding;
    private Disposable disposable;
    private SharedViewModel sharedViewModel;
    //    private boolean isDestroy = false;
    private boolean hasChange = false;


    public HistoryPageFragment(LocalDatabase db, BluetoothManager bluetoothManager) {
        super(R.layout.fragment_history_chat);
        this.db = db;
        bluetoothManager.setNearbyFindListener(this);
        nearbyPeople.addAll(bluetoothManager.getNearby());
        //TODO get pub id cause getting history is not right
//        conversations = new ArrayList<>();
//        users = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryChatBinding.inflate(inflater, container, false);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
//        sharedViewModel.getData().observe(getViewLifecycleOwner(), this::handleLiveData);
        historyLayoutManager = new LinearLayoutManager(getActivity());
        nearbyLayoutManager = new LinearLayoutManager(getActivity());
        nearbyLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        historyAdapter = new HistoryPageAdapter(historyChat, this);
        nearbyAdapter = new NearbyAdapter(nearbyPeople, this);
        binding.rvChatHistory.setLayoutManager(historyLayoutManager);
        binding.rvNearby.setLayoutManager(nearbyLayoutManager);
        binding.rvChatHistory.setAdapter(historyAdapter);
        binding.rvNearby.setAdapter(nearbyAdapter);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getHistory();
        new Runnable() {
            @Override
            public void run() {
                if (hasChange) {
                    hasChange = false;
                    updateNearby();
                }
                refreshNearbyHandler.postDelayed(this, 3000);
            }
        }.run();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        refreshNearbyHandler.removeCallbacksAndMessages(null);
        disposable.dispose();

    }

    private void getHistory() {
//        users.clear();
        binding.pbLoadingChat.setVisibility(View.VISIBLE);
        disposable = db.userMessageJoinDao().getHistory()
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(l -> {
                    historyChat.clear();
                    historyChat.addAll(l);
                    initView();
                });
//        userDispose = db.userDao().userList().subscribeOn(Schedulers.single())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(users -> {
//                    for (UserModel user : users)
//                        if (!user.id.equals(pubId))
//                            this.users.add(user);
//                    if (this.users.size() > 0)
//                        getMessages();
//                    else
//                        initView();
//                }, Throwable::printStackTrace);
    }

    private void initView() {
        binding.pbLoadingChat.setVisibility(View.GONE);
        if (!historyChat.isEmpty()) {
            historyAdapter.notifyDataSetChanged();
            if (binding.tvEmptyChat.getVisibility() == View.GONE) {
                binding.tvEmptyChat.setVisibility(View.GONE);
                binding.rvChatHistory.setVisibility(View.VISIBLE);
//            binding.ivEmptyChat.setVisibility(View.GONE);
            }
        } else {
            binding.rvChatHistory.setVisibility(View.GONE);
            binding.tvEmptyChat.setVisibility(View.VISIBLE);
//            binding.ivEmptyChat.setVisibility(View.VISIBLE);
        }
        if (nearbyPeople.isEmpty()) {
            binding.rvNearby.setVisibility(View.GONE);
            binding.tvNoNearby.setVisibility(View.VISIBLE);
        } else {
            binding.tvNoNearby.setVisibility(View.GONE);
            binding.rvNearby.setVisibility(View.VISIBLE);
        }
    }

    private void updateNearby() {
        if (nearbyPeople.isEmpty()) {
            binding.rvNearby.setVisibility(View.GONE);
            binding.tvNoNearby.setVisibility(View.VISIBLE);
        } else {
            binding.tvNoNearby.setVisibility(View.GONE);
            binding.rvNearby.setVisibility(View.VISIBLE);
        }
        nearbyAdapter.notifyDataSetChanged();
    }

//    private void getMessages() {
//        historyChat.clear();
//        currChats = 0;
//        prevChats = 0;
//        List<String> userIdList = new ArrayList<>();
//        for (UserModel u : users)
//            userIdList.add(u.id);
//        for (int i = 0; i < users.size(); i++) {
//            messageDispose = db.messageDao().historyMessageList(userIdList)
//                    .subscribeOn(Schedulers.single())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(msgList -> {
//                        if (msgList.size() > 0) {
//                            for (UserModel u : users) {
//                                MessageModel m = null;
//                                for (MessageModel msg : msgList)
//                                    if (u.id.equals(msg.src) || u.id.equals(msg.dst))
//                                        m = msg;
//                                if (m != null) {
//                                    historyChat.add(new ChatModel(u, m));
//                                    currChats++;
//                                }
//                            }
//                            mAdapter.notifyDataSetChanged();
//                        }
//                        initView();
//                    }, Throwable::printStackTrace);
//            compositeDisposable.add(messageDispose);
//        }
//    }
//

//    private void handleLiveData(LiveDataModel<List<UserModel>> dataModel) {
//        if (dataModel.getData() instanceof List) {
//            nearbyPeople.addAll(dataModel.getData());
//        }
//    }

    @Override
    public void onClick(int position, int view) {
        if (view == HISTORY)
            sharedViewModel.setData(new LiveDataModel(historyChat.get(position).getUser().name,
                    LiveDataModel.HOME,
                    LiveDataModel.CONVERSATION, historyChat.get(position).getUser().id));
        else if (view == NEARBY)
            sharedViewModel.setData(new LiveDataModel(nearbyPeople.get(position).name,
                    LiveDataModel.HOME,
                    LiveDataModel.CONVERSATION, nearbyPeople.get(position).id));
//        sharedViewModel.setData(new LiveDataModel(pChat.get(position).getUser().name, LiveDataModel.TO_PRIVATE_CHAT));
//        singleMessageDispose = db.messageDao().userMessageList(historyChat.get(position).getUser().id)
//                .subscribeOn(Schedulers.single())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(msg -> {
//                    for (MessageModel message : msg)
//                        conversations.add(new ChatModel(historyChat.get(position).getUser(), message));
//                    Gson gson = new Gson();
//                    String data = gson.toJson(conversations);
//                    sharedViewModel.setData(new LiveDataModel(historyChat.get(position).getUser().name,
//                            LiveDataModel.HISTORY,
//                            LiveDataModel.CONVERSATION, data));
//                }, Throwable::printStackTrace);
//        compositeDisposable.add(singleMessageDispose);
//        sharedViewModel.setData(new LiveDataModel(pChat.get(position).getUser().name, LiveDataModel.HISTORY));
//        getParentFragmentManager().beginTransaction()
//                .setCustomAnimations(R.anim.slide_in_ltr, R.anim.slide_out_ltr, R.anim.slide_in_rtl, R.anim.slide_out_rtl)
//                .replace(R.id.fragment_container, PrivateChatPageFragment.class, bundle)
//                .addToBackStack(null)
//                .commit();
    }

    @Override
    public void nearbyList(List<UserModel> user) {
        nearbyPeople.clear();
        nearbyPeople.addAll(user);
        hasChange = true;
//        nearbyAdapter.notifyDataSetChanged();
    }
}
