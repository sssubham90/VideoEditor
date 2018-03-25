package com.devil.videoeditor.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.devil.videoeditor.R;

import java.util.ArrayList;

public class PreviewImageAdapter extends RecyclerView.Adapter<PreviewImageAdapter.MyViewHolder> {

    private ArrayList<String> paths;

    public PreviewImageAdapter( ArrayList<String> paths) {
        this.paths = paths;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Bitmap bmp = BitmapFactory.decodeFile(paths.get(position));
        holder.ivPhoto.setImageBitmap(bmp);
    }

    @Override
    public int getItemCount() {
        return paths.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivPhoto;

        MyViewHolder(View itemView) {
            super(itemView);

            ivPhoto = itemView.findViewById(R.id.ivPhoto);
        }
    }

}
