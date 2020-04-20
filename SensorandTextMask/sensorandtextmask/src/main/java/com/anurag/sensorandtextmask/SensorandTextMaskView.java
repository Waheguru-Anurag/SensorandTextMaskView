package com.anurag.sensorandtextmask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

public class SensorandTextMaskView extends androidx.appcompat.widget.AppCompatTextView implements SensorEventListener{
    private Bitmap mMaskBitmap;
    private Canvas mMaskCanvas;
    private Paint mPaint;
    private Drawable mBackground;
    private Bitmap mBackgroundBitmap;
    private Canvas mBackgroundCanvas;
    private Canvas coverCanvas;
    private Bitmap coverBitmap;
    private Drawable coverDrawable;
    private SensorManager sensorManager;
    private Sensor accelometer,magnetometer;
    private int width= 0,height = 0;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];

    public SensorandTextMaskView(Context context) {
        super(context);
    }
    public SensorandTextMaskView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        super.setTextColor(Color.BLACK);
        super.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        sensorManager = (SensorManager)getContext().getSystemService(Context.SENSOR_SERVICE);
        accelometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    @Deprecated
    public void setBackgroundDrawable(final Drawable bg) {
        mBackground = bg;
        coverDrawable = new ColorDrawable(Color.WHITE);

        // Will always draw drawable using view bounds. This might be a
        // problem if the drawable should force the view to be bigger, e.g.
        // the view sets its dimensions to wrap_content and the drawable
        // is larger than the text.
        width = getWidth();
        height = getHeight();
        coverDrawable.setBounds(0,0,width,height);
        requestLayout();
        invalidate();
    }

    @Override
    public void setBackgroundColor(final int color) {
        setBackgroundDrawable(new ColorDrawable(color));
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w == 0 || h == 0) {
            freeBitmaps();
            return;
        }
        width = w;
        height = h;
        createBitmaps(w, h);
        if (mBackground != null) {
            mBackground.setBounds((int)(-w/2),(int)(-h/2),(int)(1800), (int)(2000));
            coverDrawable.setBounds(0,0,w,h);
        }
    }

    private void createBitmaps(int w, int h) {
        mBackgroundBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mBackgroundCanvas = new Canvas(mBackgroundBitmap);
        coverBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        coverCanvas = new Canvas(coverBitmap);
        mMaskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8);
        mMaskCanvas = new Canvas(mMaskBitmap);
    }

    private void freeBitmaps() {
        mBackgroundBitmap = null;
        mBackgroundCanvas = null;
        mMaskBitmap = null;
        mMaskCanvas = null;
        coverBitmap=null;
        coverCanvas=null;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        if (isNothingToDraw()) {
            return;
        }
        drawMask();
        drawBackground();
        canvas.drawBitmap(coverBitmap, 0.f, 0.f, null);
    }

    private boolean isNothingToDraw() {
        return mBackground == null
                || getWidth() == 0
                || getHeight() == 0;
    }

    // draw() calls onDraw() leading to stack overflow
    @SuppressLint("WrongCall")
    private void drawMask() {
        clear(mMaskCanvas);
        super.onDraw(mMaskCanvas);
    }

    private void drawBackground() {
        clear(mBackgroundCanvas);
        clear(coverCanvas);
        coverDrawable.draw(mBackgroundCanvas);
        mBackgroundCanvas.drawBitmap(mMaskBitmap,0,0,mPaint);
        mBackground.draw(coverCanvas);
        coverCanvas.drawBitmap(mBackgroundBitmap,0,0,null);

    }

    private static void clear(Canvas canvas) {
        canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
        }
        updateOrientationAngles();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE) {
            sensorManager.registerListener(this, accelometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }//onResume called
        else
            sensorManager.unregisterListener(this);// onPause() called
    }

    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        // "mRotationMatrix" now has up-to-date information.
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
        String r = getRotation(getContext());
        if(r.equals("portrait") | r.equals("reverse portrait"))
            mBackground.setBounds((int)((-1*width/4)*(Math.cos(orientationAngles[2])+1)),(int)((-1*height/4)*(Math.cos(orientationAngles[1])+1)),1800,2000);
        else
            mBackground.setBounds((int)((-1*width/4)*(Math.cos(orientationAngles[2]))+1),(int)((-1*height/4)*(Math.cos(orientationAngles[1])+1)),2000,1800);
        setBackgroundDrawable(mBackground);

        // "mOrientationAngles" now has up-to-date information.
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            sensorManager.registerListener(this, accelometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        else
            sensorManager.unregisterListener(this);// onPause() called
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        sensorManager.registerListener(this, accelometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }

    public String getRotation(Context context){
        final int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return "portrait";
            case Surface.ROTATION_90:
                return "landscape";
            case Surface.ROTATION_180:
                return "reverse portrait";
            default:
                return "reverse landscape";
        }
    }

}
