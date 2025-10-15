package com.example.gestordetareas.ui;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SignaturePad extends View {
    private static final float STROKE_WIDTH = 5f;

    private Paint paint = new Paint();
    private Path path = new Path();

    public SignaturePad(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(STROKE_WIDTH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float eventX = event.getX();
        float eventY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(eventX, eventY);
                return true;

            case MotionEvent.ACTION_MOVE:
                path.lineTo(eventX, eventY);
                break;

            default:
                return false;
        }

        invalidate();
        return true;
    }

    public void clear() {
        path.reset();
        invalidate();
    }

    public boolean isEmpty() {
        return path.isEmpty();
    }

    public Bitmap getSignatureBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        draw(canvas);
        return bitmap;
    }
}