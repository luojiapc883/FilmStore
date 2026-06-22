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
 * 直播频道适配器
 */
public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {

    private Context context;
    private List<LiveSource.Channel> channels;
    private int selectedPosition = -1;

    public interface OnChannelClickListener {
        void onChannelClick(int position, LiveSource.Channel channel);
    }

    private OnChannelClickListener listener;

    public ChannelAdapter(Context context, List<LiveSource.Channel> channels) {
        this.context = context;
        this.channels = channels;
    }

    public void setOnChannelClickListener(OnChannelClickListener listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        LiveSource.Channel channel = channels.get(position);
        holder.text1.setText(channel.getName() != null ? channel.getName() : "未知频道");

        String status = channel.getStatus();
        String quality = channel.getQuality();
        StringBuilder subtitle = new StringBuilder();
        if (status != null) {
            subtitle.append(status);
        }
        if (quality != null) {
            if (subtitle.length() > 0) subtitle.append(" · ");
            subtitle.append(quality);
        }
        holder.text2.setText(subtitle.toString());

        // 焦点状态
        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.bgCardSelected));
        } else {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.bgCard));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int oldPos = selectedPosition;
                selectedPosition = position;
                notifyItemChanged(oldPos);
                notifyItemChanged(position);
                listener.onChannelClick(position, channel);
            }
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            v.setBackgroundColor(hasFocus
                    ? context.getResources().getColor(R.color.focusBackground)
                    : (position == selectedPosition
                        ? context.getResources().getColor(R.color.bgCardSelected)
                        : context.getResources().getColor(R.color.bgCard)));
        });
    }

    @Override
    public int getItemCount() {
        return channels != null ? channels.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1;
        TextView text2;

        ViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}
