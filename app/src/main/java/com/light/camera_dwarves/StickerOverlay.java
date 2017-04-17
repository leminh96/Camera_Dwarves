package com.light.camera_dwarves;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatDrawableManager;
import android.util.Log;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import java.util.List;

/**
 * Created by Grey on 4/12/2017.
 */

public class StickerOverlay extends Overlay.Sticker {

    private Context mContext;
    private Face mFace;
    private PointF dectectLeftEye;
    private PointF dectectRighttEye;
    private Overlay mOverlay;

    public StickerOverlay(Context context, Overlay overlay) {
        super(overlay);
        mContext = context;
        mOverlay = overlay;
    }

    public void updateFace(Face face)
    {
        mFace = face;
        for (Landmark landmark : face.getLandmarks())
        {
            if (landmark.getType() == Landmark.LEFT_EYE)
                dectectLeftEye = landmark.getPosition();
            else if (landmark.getType() == Landmark.RIGHT_EYE)
                dectectRighttEye = landmark.getPosition();
        }
        postInvalidate();
    }

    //get a bitmap from a svg file (the sticker)
    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId, int width,
                                                     float angle) {
        Drawable drawable = AppCompatDrawableManager.get().getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        float scale = (float)drawable.getIntrinsicHeight()/(float)drawable.getIntrinsicWidth();
        int x = (int)(width * 2);
        int y = (int)(x*scale);


        Bitmap bitmap = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    @Override
    public void draw(Canvas canvas) {

        float leftEyeX,leftEyeY,rightEyeX,rightEyeY;

        if (dectectLeftEye == null || dectectRighttEye == null)
        {
            return;
        }

        if (mOverlay.getmFacing() == CameraSource.CAMERA_FACING_FRONT) {
            leftEyeX = translateX(dectectLeftEye.x);
            leftEyeY = translateY(dectectLeftEye.y);
            rightEyeX = translateX(dectectRighttEye.x);
            rightEyeY = translateY(dectectRighttEye.y);
        }
        else
        {
            leftEyeX = translateX(dectectRighttEye.x);
            leftEyeY = translateY(dectectRighttEye.y);
            rightEyeX = translateX(dectectLeftEye.x);
            rightEyeY = translateY(dectectLeftEye.y);
        }

        float angle = - (180 - ((float) (Math.atan2(leftEyeY - rightEyeY,
                leftEyeX - rightEyeX) * 180 / Math.PI)));

        float distance = (float) (Math.sqrt((leftEyeX - rightEyeX) *
                (leftEyeX - rightEyeX) +
                (leftEyeY - rightEyeY) * (leftEyeY - rightEyeY)));

        Bitmap bitmap = getBitmapFromVectorDrawable(mContext,R.drawable.ic_glasses,
                (int)distance,angle);

        Matrix matrix = new Matrix();
        matrix.postRotate(angle,bitmap.getWidth()/2,bitmap.getHeight()/2);
        matrix.postTranslate(leftEyeX - distance / 2,
                leftEyeY - distance / 2);

        canvas.drawBitmap(bitmap,matrix,null);
    }
}
