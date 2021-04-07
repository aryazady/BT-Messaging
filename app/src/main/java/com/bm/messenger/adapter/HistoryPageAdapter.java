package com.bm.messenger.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bm.messenger.R;
import com.bm.messenger.model.ChatModel;
import com.bm.messenger.ui.fragment.HistoryPageFragment;

import java.util.List;

public class HistoryPageAdapter extends RecyclerView.Adapter<HistoryPageAdapter.ViewHolder> {

    private List<ChatModel> userChats;
    private AdapterOnClickListener onClickListener;

    public HistoryPageAdapter(List<ChatModel> userChats, AdapterOnClickListener onClickListener) {
        this.userChats = userChats;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public HistoryPageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_history_chat, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryPageAdapter.ViewHolder holder, int position) {
        holder.getTvName().setText(userChats.get(position).getUser().name);
        holder.getTvPreview().setText(userChats.get(position).getMessage().content);
        holder.itemView.setOnClickListener(v -> onClickListener.onClick(position, HistoryPageFragment.HISTORY));
    }

    @Override
    public int getItemCount() {
        return userChats.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName, tvPreview;
        private final View rootView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_pv_name_adapter);
            tvPreview = itemView.findViewById(R.id.tv_pv_preview_adapter);
            rootView = itemView.findViewById(R.id.ll_pv_root_view);
        }

        public View getRootView() {
            return rootView;
        }

        public TextView getTvName() {
            return tvName;
        }

        public TextView getTvPreview() {
            return tvPreview;
        }
    }
}
