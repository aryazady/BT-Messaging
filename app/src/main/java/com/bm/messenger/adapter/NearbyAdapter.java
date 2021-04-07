package com.bm.messenger.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bm.messenger.R;
import com.bm.messenger.model.UserModel;
import com.bm.messenger.ui.fragment.HistoryPageFragment;

import java.util.List;

public class NearbyAdapter extends RecyclerView.Adapter<NearbyAdapter.ViewHolder> {

    private List<UserModel> data;
    private AdapterOnClickListener onClickListener;

    public NearbyAdapter(List<UserModel> data, AdapterOnClickListener onClickListener) {
        this.data = data;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_nearby, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.getTvName().setText(data.get(position).name);
        holder.itemView.setOnClickListener(v -> onClickListener.onClick(position, HistoryPageFragment.NEARBY));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.nearby_name);
        }

        public TextView getTvName() {
            return tvName;
        }
    }
}
