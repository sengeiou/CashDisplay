package com.resonance.cashdisplay.slide_show;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
//import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

import com.resonance.cashdisplay.utils.ImageUtils;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.R;

import java.io.File;
import java.util.List;

public class SlideViewAdapter extends RecyclerView.Adapter<SlideViewAdapter.SlideViewHolder> {

    public final String TAG = "Slide";

    private List<Slide> moviesList;

    public class SlideViewHolder extends RecyclerView.ViewHolder {

        public ImageView slideImage;
        public SlideViewHolder(View view) {
            super(view);
            slideImage = (ImageView) view.findViewById(R.id.slideImageView);

        }
    }


    public SlideViewAdapter(List<Slide> moviesList) {
        this.moviesList = moviesList;
    }

    @Override
    public SlideViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.slide_row, parent, false);
        return new SlideViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SlideViewHolder holder, int position) {

        Slide movie = moviesList.get(position);
        File fileImg =new File(movie.getPathToImgFile());
       // Log.w(TAG, "Slide fileImg: " + fileImg.getPath());

        if (fileImg.exists())
        {
            ImageUtils.freeMemory();
            Bitmap bm = ImageUtils.resizeBitmap(ImageUtils.getImage(fileImg, MainActivity.sizeScreen, false), MainActivity.sizeScreen.x, MainActivity.sizeScreen.y, ImageUtils.RequestSizeOptions.RESIZE_FIT);
           // Bitmap bm = ImageUtils.getImage(fileImg, MainActivity.sizeScreen, false);
            holder.slideImage.setImageBitmap(bm);
           // setFadeAnimation(holder.itemView);
        }else
        {
            Bitmap bm = ImageUtils.resizeBitmap(BitmapFactory.decodeResource(MainActivity.mContext.getResources(), R.drawable.noimagefound), MainActivity.sizeScreen.x, MainActivity.sizeScreen.y, ImageUtils.RequestSizeOptions.RESIZE_FIT);
            holder.slideImage.setImageBitmap(bm);
            moviesList.clear();
        }

    }

    private void setFadeAnimation(View view) {
        AlphaAnimation anim = new AlphaAnimation(0.2f, 1.0f);
        anim.setDuration(800);
        anim.setStartOffset(800);
        anim.setFillAfter(true);
        view.startAnimation(anim);
    }

    @Override
    public int getItemCount() {
        return moviesList.size();
    }
}
