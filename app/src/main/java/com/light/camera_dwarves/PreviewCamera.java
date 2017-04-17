package com.light.camera_dwarves;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;

import java.io.IOException;

/**
 * Created by Grey on 4/11/2017.
 */

public class PreviewCamera extends SurfaceView implements SurfaceHolder.Callback {

    private CameraSource mCameraSource;
    private SurfaceHolder mHolder;
    private Overlay mOverlay;
    private boolean mStartRequested;
    private boolean mSurfaceAvailable;

    public PreviewCamera(Context context, CameraSource cameraSource)
    {
        super(context);
        mCameraSource = cameraSource;
        mStartRequested = false;
        mSurfaceAvailable = false;
        //
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void start(CameraSource cameraSource) throws IOException {
        if (cameraSource == null) {
            stop();
        }

        mCameraSource = cameraSource;

        if (mCameraSource != null) {
            mStartRequested = true;
            startIfReady();
        }
    }

    public void start(CameraSource cameraSource, Overlay overlay) throws IOException {
        mOverlay = overlay;
        start(cameraSource);
    }

    public void stop() {
        if (mCameraSource != null) {
            mCameraSource.stop();
        }
    }

    public void release() {
        if (mCameraSource != null) {
            mCameraSource.release();
            mCameraSource = null;
        }
    }

    private void startIfReady() throws IOException {
        if (mStartRequested && mSurfaceAvailable) {
            mCameraSource.start(mHolder);
            mStartRequested = false;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mSurfaceAvailable = true;
        try {
            startIfReady();
        } catch (IOException e) {
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mSurfaceAvailable = false;
    }
}
