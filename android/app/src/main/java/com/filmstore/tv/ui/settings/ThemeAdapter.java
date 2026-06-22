package com.filmstore.tv.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.filmstore.tv.R;
import com.filmstore.tv.model.ClientConfig;

import java.util.List;

/**
 * 主题选择适配器
 */
public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {

    private Context context;
    private List<ClientConfig.ThemeItem> items;
    private String selectedThemeName;

    public interface OnThemeClickListener {
        void onThemeClick(String name, String title);
    }

    private OnThemeClickListener listener;

    public ThemeAdapter(Context context, List<ClientConfig.ThemeItem> items) {
        this.context = context;
        this.items = items;
    }

    public void setOnThemeClickListener(OnThemeClickListener listener) {
        this.listener = listener;
    }

    public void updateList(List<ClientConfig.ThemeItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    public void setSelectedTheme(String themeName) {
        this.selectedThemeName = themeName;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_vod_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ClientConfig.ThemeItem item = items.get(position);

        // 显示主题名称
        holder.titleView.setText(item.getTitle() != null ? item.getTitle() : item.getName());

        // 预览颜色
        if (item.getConfig() != null && item.getConfig().getPrimary() != null) {
            try {
                int color = Color.parseColor(item.getConfig().getPrimary());
                holder.cardView.setCardBackgroundColor(color);
            } catch (Exception e) {
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.bgCard));
            }
        }

        // 选中状态
        if (item.getName() != null && item.getName().equals(selectedThemeName)) {
            holder.cardView.setCardElevation(8f);
            holder.cardView.setStrokeWidth(4);
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.colorAccent));
        } else {
            holder.cardView.setCardElevation(4f);
            holder.cardView.setStrokeWidth(0);
        }

        // 默认标记
        if (item.isDefault() && selectedThemeName == null) {
            holder.titleView.setText(item.getTitle() + " (默认)");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onThemeClick(item.getName(), item.getTitle());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView titleView;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            titleView = itemView.findViewById(R.id.title);
        }
    }
}
