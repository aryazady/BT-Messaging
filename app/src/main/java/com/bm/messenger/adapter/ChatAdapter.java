package com.bm.messenger.adapter;

import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bm.messenger.R;
import com.bm.messenger.model.ChatModel;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private final List<ChatModel> dataSet;
    private final String myPubId;

    public ChatAdapter(List<ChatModel> dataSet, String myPubId) {
        this.dataSet = dataSet;
        this.myPubId = myPubId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_chat, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RelativeLayout.LayoutParams bgParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        Resources resources = holder.itemView.getResources();
        layoutParams.rightMargin = dpToPixel(resources, 8);
        layoutParams.leftMargin = dpToPixel(resources, 8);
        layoutParams.topMargin = dpToPixel(resources, 2);
        layoutParams.bottomMargin = dpToPixel(resources, 2);
        holder.getName().setVisibility(View.GONE);
        holder.getBg().setPadding(dpToPixel(resources, 10), dpToPixel(resources, 8), dpToPixel(resources, 12), dpToPixel(resources, 10));
        if (position == 0)
            layoutParams.bottomMargin = dpToPixel(resources, 6);
        else if (position == dataSet.size() - 1)
            layoutParams.topMargin = dpToPixel(resources, 8);
        int chatNum = dataSet.size() - position - 1;
        if (dataSet.get(chatNum).getMessage().dst == null && !dataSet.get(chatNum).getMessage().src.equals(myPubId)) {
            holder.getName().setVisibility(View.VISIBLE);
            holder.getBg().setPadding(dpToPixel(resources, 10), dpToPixel(resources, 4), dpToPixel(resources, 12), dpToPixel(resources, 6));
        }
        if (dataSet.get(chatNum).getMessage().src.equals(myPubId)) {
            bgParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            if (chatNum != dataSet.size() - 1 && dataSet.get(chatNum).getMessage().src.equals(dataSet.get(chatNum + 1).getMessage().src))
                holder.getBg().setBackground(ContextCompat.getDrawable(holder.getBg().getContext(), R.drawable.self_message_mid));
            else {
                holder.getBg().setBackground(ContextCompat.getDrawable(holder.getBg().getContext(), R.drawable.self_message_end));
                layoutParams.bottomMargin = dpToPixel(resources, 6);
            }
        } else {
            bgParams.addRule(RelativeLayout.ALIGN_PARENT_START);
            if (chatNum != dataSet.size() - 1 && dataSet.get(chatNum).getMessage().src.equals(dataSet.get(chatNum + 1).getMessage().src))
                holder.getBg().setBackground(ContextCompat.getDrawable(holder.getBg().getContext(), R.drawable.message_mid));
            else {
                holder.getBg().setBackground(ContextCompat.getDrawable(holder.getBg().getContext(), R.drawable.message_end));
                layoutParams.bottomMargin = dpToPixel(resources, 6);
            }
        }
        holder.getContent().setText(dataSet.get(chatNum).getMessage().content);
        holder.getName().setText(dataSet.get(chatNum).getUser().name);
        holder.getBg().setLayoutParams(bgParams);
        holder.getLayout().setLayoutParams(layoutParams);
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public int dpToPixel(Resources resources, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                resources.getDisplayMetrics()
        );
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView content, name;
        private final LinearLayout bg;
        private final RelativeLayout layout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            content = itemView.findViewById(R.id.tv_message_adapter);
            name = itemView.findViewById(R.id.tv_name_adapter);
            bg = itemView.findViewById(R.id.message_bg_adapter);
            layout = itemView.findViewById(R.id.message_layout_adapter);
            itemView.setOnClickListener(v -> {
                getAdapterPosition();
            });
        }

        public TextView getName() {
            return name;
        }

        public RelativeLayout getLayout() {
            return layout;
        }

        public LinearLayout getBg() {
            return bg;
        }

        public TextView getContent() {
            return content;
        }
    }
}
