package com.light.camera_dwarves;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class CameraDwarvesActivity extends AppCompatActivity {

    private static final int RC_HANDLE_STORAGE_PERM = 3;
    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAM_PERM = 2;
    private static final int RESULT_LOAD_IMAGE = 4;
    private static final int REQUEST_FROM_IMAGEACTIVITY = 5;
    private boolean isFrontCamera = true;
    private FrameLayout mFrameLayout;
    private ImageButton mCaptureButton, mSwitchButton, mAlbumButton;
    private Overlay overlay;
    private PreviewCamera mPreview;
    private CameraSource mCameraSource = null;

    //listener to take a picture
    private CameraSource.PictureCallback mPicture = new CameraSource.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes) {
            mPreview.stop();
            //create the file to save picture
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d("mytag", "Error creating media file, check storage permissions: ");
                return;
            }

            //get the photo taken by the camera
            Bitmap photo = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            Matrix m = new Matrix();
            if (isFrontCamera) {
                m.preScale(-1.0f, 1.0f);
            }
            m.postRotate(90.f);
            Bitmap scaledPhoto = Bitmap.createScaledBitmap(photo,
                    overlay.getHeight(),overlay.getWidth(),true);
            Bitmap oriPhoto = Bitmap.createBitmap(scaledPhoto,0,0,
                    scaledPhoto.getWidth(),scaledPhoto.getHeight(),m,true);
            oriPhoto = oriPhoto.copy(oriPhoto.getConfig(),true);

            Bitmap overlayStickers = getBitmapFromView(overlay);
            Canvas canvas = new Canvas(oriPhoto);
            canvas.drawBitmap(overlayStickers,new Matrix(),null);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            oriPhoto.compress(Bitmap.CompressFormat.JPEG,100,stream);
            byte[] data = stream.toByteArray();

            //
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("mytag", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("mytag", "Error accessing file: " + e.getMessage());
            }

            MediaScannerConnection.scanFile(CameraDwarvesActivity.this,
                    new String[] {pictureFile.getPath()},null,null);

            startCameraSource();
        }
    };

    private Bitmap getBitmapFromView(View v)
    {
        Bitmap bitmap = Bitmap.createBitmap(v.getWidth(),
                v.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        v.draw(c);
        return bitmap;
    }

    @Nullable
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
            try {
                mediaFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            return null;
        }

        return mediaFile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_dwarves);
        overlay = (Overlay) findViewById(R.id.overlay);
        mFrameLayout = (FrameLayout) findViewById(R.id.camera_preview);
        //request camera permissions
        int rcc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rcc == PackageManager.PERMISSION_GRANTED)
        {
            init();
        }
        else
        {
            requestCameraPermission();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
            mCameraSource = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null)
            mPreview.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    private void init()
    {
        createCameraSource();
        mPreview = new PreviewCamera(this,mCameraSource);
        mFrameLayout.addView(mPreview);
        mCaptureButton = (ImageButton) findViewById(R.id.capture_button);
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int rcs = ActivityCompat.checkSelfPermission(CameraDwarvesActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if (rcs == PackageManager.PERMISSION_GRANTED)
                {
                    mCameraSource.takePicture(null,mPicture);
                }
                else
                {
                    ActivityCompat.requestPermissions(CameraDwarvesActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},RC_HANDLE_STORAGE_PERM);
                }
            }
        });

        mAlbumButton = (ImageButton) findViewById(R.id.album_button);
        mAlbumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i,RESULT_LOAD_IMAGE);
            }
        });

        mSwitchButton = (ImageButton) findViewById(R.id.switch_button);
        mSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFrontCamera = !isFrontCamera;
                mPreview.release();

                createCameraSource();
                startCameraSource();
            }
        });
    }

    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, overlay);
            } catch (IOException e) {
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null)
        {
            mPreview.stop();
            Uri selectedImage = data.getData();
            Intent i = ImageActivity.createNewIntent(CameraDwarvesActivity.this,selectedImage);
            startActivityForResult(i,REQUEST_FROM_IMAGEACTIVITY);
        }
        else
        {
            mPreview.release();
            createCameraSource();
            startCameraSource();
        }
    }

    private void requestCameraPermission()
    {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    RC_HANDLE_CAM_PERM);
    }

    //Handle the permission response
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case RC_HANDLE_CAM_PERM:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    init();
                }
                else
                {
                    Toast.makeText(this,"Cannot access camera!",Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case RC_HANDLE_STORAGE_PERM: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPreview.stop();
                    mCameraSource.takePicture(null, mPicture);
                    startCameraSource();
                } else {
                    Toast.makeText(this, "Cannot access storage!", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    //Create a camera
    public void createCameraSource()
    {
        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(true)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .build();

        detector.setProcessor(new MultiProcessor.Builder<>(new FaceTrackerFactory())
                .build());

        if (!detector.isOperational())
        {
            Log.w("myTag","face detector are not yet available");
        }

        Camera camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        List sizes = parameters.getSupportedPreviewSizes();
        Camera.Size size = (Camera.Size) sizes.get(0);
        camera.release();

        int facing;
        if (isFrontCamera)
            facing = CameraSource.CAMERA_FACING_FRONT;
        else
            facing = CameraSource.CAMERA_FACING_BACK;

        mCameraSource = new CameraSource.Builder(context,detector)
                .setFacing(facing)
                .setRequestedPreviewSize(size.width,size.height)
                .setRequestedFps(30.0f)
                .build();
        overlay.setCameraInfo(size.height,size.width,facing);
    }


    private class FaceTrackerFactory implements MultiProcessor.Factory<Face>
    {
        @Override
        public Tracker<Face> create(Face face) {
            return new FaceTracker(overlay);
        }
    }

    private class FaceTracker extends Tracker<Face>
    {
        private StickerOverlay stickerOverlay;
        private Overlay mOverlay;

        public FaceTracker(Overlay overlay) {
            super();
            mOverlay = overlay;
            stickerOverlay = new StickerOverlay(CameraDwarvesActivity.this,mOverlay);
        }

        @Override
        public void onNewItem(int id, Face face) {
            super.onNewItem(id, face);
        }

        @Override
        public void onUpdate(FaceDetector.Detections<Face> detections, Face face) {
            super.onUpdate(detections, face);
            mOverlay.add(stickerOverlay);
            stickerOverlay.updateFace(face);
        }

        @Override
        public void onMissing(Detector.Detections<Face> detections) {
            super.onMissing(detections);
            mOverlay.remove(stickerOverlay);
        }

        @Override
        public void onDone() {
            super.onDone();
            mOverlay.remove(stickerOverlay);
        }
    }
}


