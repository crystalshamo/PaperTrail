package com.example.papertrail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorWheelView extends View {

    private Paint paint;
    private Bitmap bitmap;
    private int centerX, centerY, radius;
    private int selectedColor;
    private OnColorSelectedListener colorSelectedListener;

    public ColorWheelView(Context context) {
        super(context);
        init();
    }

    public ColorWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        selectedColor = Color.BLACK; // Default color

        // Set up a listener interface for color selection
        colorSelectedListener = (OnColorSelectedListener) getContext();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        centerX = w / 2;
        centerY = h / 2;
        radius = Math.min(w, h) / 2;

        // Create a bitmap to draw the color wheel
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawColorWheel(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(bitmap, 0, 0, null);
    }

    private void drawColorWheel(Canvas canvas) {
        // Create a radial gradient with the spectrum of colors
        RadialGradient gradient = new RadialGradient(centerX, centerY, radius, new int[]{
                Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
        }, null, Shader.TileMode.CLAMP);
        paint.setShader(gradient);

        // Draw the color wheel (circle)
        canvas.drawCircle(centerX, centerY, radius, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();

            // Calculate the distance from the center of the circle
            float dx = x - centerX;
            float dy = y - centerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance <= radius) {
                // Calculate the angle of the touch
                float angle = (float) Math.atan2(dy, dx);
                angle = (float) ((angle + Math.PI) / (2 * Math.PI)); // Normalize to [0, 1]

                // Get the color at that angle
                selectedColor = interpolateColor(angle);
                if (colorSelectedListener != null) {
                    colorSelectedListener.onColorSelected(selectedColor);
                }
                invalidate(); // Redraw the view
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    // Method to interpolate between colors on the wheel
    private int interpolateColor(float angle) {
        int[] colors = new int[] {
                Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
        };

        int colorIndex = (int) (angle * (colors.length - 1));
        return colors[colorIndex];
    }

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.colorSelectedListener = listener;
    }
}

