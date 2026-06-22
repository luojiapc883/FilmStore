package com.filmstore.tv.ui.vod;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.filmstore.tv.R;
import com.filmstore.tv.model.VodItem;

import java.util.List;

/**
 * 点播网格适配器 - 用于分类列表和搜索结果
 */
public class VodGridAdapter extends RecyclerView.Adapter<VodGridAdapter.ViewHolder> {

    private Context context;
    private List<VodItem> items;

    public VodGridAdapter(Context context, List<VodItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_vod_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        VodItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView poster;
        TextView title;
        TextView remarks;
        View focusBorder;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            poster = itemView.findViewById(R.id.poster);
            title = itemView.findViewById(R.id.title);
        }

        void bind(VodItem item) {
            if (title != null) {
                title.setText(item.getVodName() != null ? item.getVodName() : "");
            }

            // 加载海报
            if (poster != null && item.getFullPosterUrl() != null) {
                Glide.with(context)
                        .load(item.getFullPosterUrl())
                        .placeholder(R.color.placeholder)
                        .error(R.color.placeholder)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(poster);
            }

            // 点击事件
            cardView.setOnClickListener(v -> {
                Intent intent = new Intent(context, VodDetailActivity.class);
                intent.putExtra("vod_id", item.getVodId());
                intent.putExtra("vod_name", item.getVodName());
                intent.putExtra("vod_pic", item.getVodPic());
                intent.putExtra("vod_remarks", item.getVodRemarks());
                intent.putExtra("vod_score", item.getVodScore());
                context.startActivity(intent);
            });

            // 焦点变化
            cardView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    cardView.setCardElevation(8f);
                    cardView.setScaleX(1.05f);
                    cardView.setScaleY(1.05f);
                } else {
                    cardView.setCardElevation(4f);
                    cardView.setScaleX(1.0f);
                    cardView.setScaleY(1.0f);
                }
            });
        }
    }
}
