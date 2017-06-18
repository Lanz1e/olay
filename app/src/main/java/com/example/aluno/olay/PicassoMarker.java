package com.example.aluno.olay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Created by Lucas on 16/06/2017.
 */

public class PicassoMarker implements Target {
    Marker mMarker;
    Context mContext;
    int mCont;

    PicassoMarker(Marker marker, Context context, int c) {
        mMarker = marker;
        mContext=context;
        mCont=c;
    }

    @Override
    public int hashCode() {
        return mMarker.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof PicassoMarker) {
            Marker marker = ((PicassoMarker) o).mMarker;
            return mMarker.equals(marker);
        } else {
            return false;
        }
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {

        float aspectRatio = bitmap.getWidth() /
                (float) bitmap.getHeight();
        int height = 120;
        int width = Math.round(height * aspectRatio);

        bitmap = Bitmap.createScaledBitmap(
                bitmap, width, height, false);

        mMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
        Toast.makeText(mContext, "vez:  "+mCont, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {

    }
}