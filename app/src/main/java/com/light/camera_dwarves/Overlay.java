package com.light.camera_dwarves;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.gms.vision.CameraSource;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Grey on 4/12/2017.
 */

public class Overlay extends View {

    private final Object mLock = new Object();
    private int mPreviewWidth;
    private float mWidthScaleFactor = 1.0f;
    private int mPreviewHeight;
    private float mHeightScaleFactor = 1.0f;
    private int mFacing = CameraSource.CAMERA_FACING_FRONT;
    private Set<Sticker> mStickers = new HashSet<>();

    public static abstract class Sticker
    {
        private Overlay mOverLay;

        public Sticker(Overlay overlay)
        {
            mOverLay = overlay;
        }

        public abstract void draw(Canvas canvas);

        public float scaleX(float horizontal)
        {
            return horizontal * mOverLay.mWidthScaleFactor;
        }

        public float scaleY(float vertical)
        {
            return vertical * mOverLay.mHeightScaleFactor;
        }

        public float translateX(float x)
        {
            if (mOverLay.mFacing == CameraSource.CAMERA_FACING_FRONT)
                return mOverLay.getWidth() - scaleX(x);

            return scaleX(x);
        }

        public float translateY(float y)
        {
            return scaleY(y);
        }

        public void postInvalidate()
        {
            mOverLay.postInvalidate();
        }
    }

    public Overlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void add(Sticker sticker)
    {
        synchronized (mLock) {
            mStickers.add(sticker);
        }
        postInvalidate();
    }

    public void clear()
    {
        synchronized (mLock) {
            mStickers.clear();
        }
        postInvalidate();
    }

    public void remove(Sticker sticker) {
        synchronized (mLock) {
            mStickers.remove(sticker);
        }
        postInvalidate();
    }

    /**
     * Sets the camera attributes for size and facing direction, which informs how to transform
     * image coordinates later.
     */
    public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
        synchronized (mLock) {
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
            mFacing = facing;
        }
        postInvalidate();
    }

    public int getmFacing()
    {
        synchronized (mLock) {
            return mFacing;
        }
    }

    /**
     * Draws the overlay with its associated graphic objects.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (mLock) {
            if ((mPreviewWidth != 0) && (mPreviewHeight != 0)) {
                mWidthScaleFactor = (float) canvas.getWidth() / (float) mPreviewWidth;
                mHeightScaleFactor = (float) canvas.getHeight() / (float) mPreviewHeight;
            }

            for (Sticker sticker : mStickers) {
                sticker.draw(canvas);
            }
        }
    }
}
