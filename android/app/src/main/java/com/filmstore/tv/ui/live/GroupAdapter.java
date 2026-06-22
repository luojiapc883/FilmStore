package com.filmstore.tv.ui.live;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.filmstore.tv.R;
import com.filmstore.tv.model.LiveSource;

import java.util.List;

/**
 * 直播分组适配器
 */
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {

    private Context context;
    private List<LiveSource.Group> groups;
    private int selectedPosition = 0;

    public interface OnGroupClickListener {
        void onGroupClick(int position, LiveSource.Group group);
    }

    private OnGroupClickListener listener;

    public GroupAdapter(Context context, List<LiveSource.Group> groups) {
        this.context = context;
        this.groups = groups;
    }

    public void setOnGroupClickListener(OnGroupClickListener listener) {
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int oldPos = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPos);
        notifyItemChanged(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        LiveSource.Group group = groups.get(position);
        holder.textView.setText(group.getName() != null ? group.getName() : "未分组");

        if (position == selectedPosition) {
            holder.textView.setBackgroundColor(context.getResources().getColor(R.color.bgCardSelected));
            holder.textView.setTextColor(context.getResources().getColor(R.color.colorAccent));
        } else {
            holder.textView.setBackgroundColor(context.getResources().getColor(R.color.bgCard));
            holder.textView.setTextColor(context.getResources().getColor(R.color.textPrimary));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGroupClick(position, group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groups != null ? groups.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
