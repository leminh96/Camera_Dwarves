package com.light.camera_dwarves;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatDrawableManager;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.IOException;

public class ImageActivity extends AppCompatActivity {

    private static final String EXTRA_IMAGE_PATH = "com.light.camera_dwarves.image_path";
    private ImageView imageView;
    private Bitmap oriImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        imageView = (ImageView) findViewById(R.id.image_view);

        Uri imagePath = getIntent().getParcelableExtra(EXTRA_IMAGE_PATH);
        try {
            oriImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imagePath);
            imageView.setImageBitmap(oriImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Intent createNewIntent(Context packageContext, Uri imagePath)
    {
        Intent i = new Intent(packageContext,ImageActivity.class);
        i.putExtra(EXTRA_IMAGE_PATH,imagePath);
        return i;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setResult(RESULT_OK);
    }
}
