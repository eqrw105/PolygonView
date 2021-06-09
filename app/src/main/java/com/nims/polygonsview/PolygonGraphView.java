package com.nims.polygonsview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.Nullable;

public class PolygonGraphView extends View implements ValueAnimator.AnimatorUpdateListener {
    private int backColor;
    private int fillColor;
    private int axisColor;
    private int axisSize;
    private int dotSize;
    private int dotPadding;
    private int labelSize;
    private int labelPadding;
    private int animDuration;
    private float animValue;
    private Paint paint;
    private Path path;
    private Rect rect;
    private ValueAnimator animator;
    private ValueHolder[] valueHolders;

    public PolygonGraphView(Context context) {
        this(context, null);
    }

    public PolygonGraphView(Context context,
                            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PolygonGraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PolygonGraphView,
                defStyleAttr, 0);
        try {
            backColor = a.getColor(R.styleable.PolygonGraphView_backColor, Color.WHITE);
            fillColor = a.getColor(R.styleable.PolygonGraphView_fillColor, Color.BLUE);
            axisColor = a.getColor(R.styleable.PolygonGraphView_axisColor, Color.BLACK);
            axisSize = a.getDimensionPixelSize(R.styleable.PolygonGraphView_axisSize, 1);
            dotSize = a.getDimensionPixelSize(R.styleable.PolygonGraphView_dotSize, 8);
            dotPadding = a.getDimensionPixelSize(R.styleable.PolygonGraphView_dotPadding, 20);
            labelSize = a.getDimensionPixelSize(R.styleable.PolygonGraphView_labelSize, 30);
            labelPadding = a.getDimensionPixelSize(R.styleable.PolygonGraphView_labelPadding, 20);
            animDuration = a.getInt(R.styleable.PolygonGraphView_animDuration, 1000);
        } finally {
            a.recycle();
        }

        setup();
    }

    private void setup() {
        paint = new Paint();
        path = new Path();
        rect = new Rect();

        animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(animDuration);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(this);

        if(isInEditMode()) {
            setupForEditMode();
        }
    }

    public void setAdapter(Adapter<?> adapter) {
        setAdapter(adapter, true);
    }

    public void setAdapter(Adapter<?> adapter, boolean anim) {
        valueHolders = new ValueHolder[adapter.getSize()];
        for(int i=0; i<adapter.getSize(); i++) {
            valueHolders[i] = adapter.onCreateValueHolder(i);
        }

        if(anim) {
            animator.start();
        } else {
            animValue = 1f;
            invalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        animator.removeAllUpdateListeners();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        animValue = (Float) animation.getAnimatedValue();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (valueHolders == null) {
            return;
        }
        if (valueHolders.length < 2) {
            throw new IllegalArgumentException("size must be greater than 2");
        }

        int radius = Math.min(getWidth(), getHeight()) / 2 - (60 + dotSize + labelSize);
        drawBacks(canvas, radius);
        drawGraph(canvas, radius);
        drawAxis(canvas, radius);
    }

    private void drawAxis(Canvas canvas, int radius) {
        final int size = valueHolders.length;
        final float half = (size / 2) + 1;
        final float quad = half / 2;
        final int centerX = getWidth() / 2;
        final int centerY = getHeight() / 2;

        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(labelSize);
        Point[] points = new Point[size];
        for (int i = 0; i < size; i++) {
            ValueHolder holder = valueHolders[i];
            paint.setColor(holder.color);

            Point dotPoint = computePoint(size, i, radius + dotPadding);
            canvas.drawCircle(dotPoint.x, dotPoint.y, dotSize, paint);

            Point labelPoint = computePoint(size, i, radius + dotSize + dotPadding + labelPadding);
            paint.getTextBounds(holder.title, 0, holder.title.length(), rect);
            if(i > quad && i < (size - quad)) {
                labelPoint.y += rect.height();
            } else if(i == quad || i == (size - quad)) {
                labelPoint.y += rect.height() / 2;
            }
            if(i % half == 0) {
                labelPoint.x -= rect.width() / 2;
            } else if(i > half) {
                labelPoint.x -= rect.width();
            }
            canvas.drawText(holder.title, labelPoint.x, labelPoint.y, paint);

            points[i] = computePoint(size, i, radius);
        }

        paint.setColor(axisColor);
        paint.setStrokeWidth(axisSize);
        paint.setStyle(Paint.Style.STROKE);
        drawPoly(canvas, points, paint);

        paint.setAlpha(128);
        for(Point point: points) {
            canvas.drawLine(point.x, point.y, centerX, centerY, paint);
        }
    }


    private void drawBacks(Canvas canvas, int radius) {
        final int size = valueHolders.length;
        Point[] backPoints = new Point[size];
        for (int i = 0; i < size; i++) backPoints[i] = computePoint(size, i, radius);

        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(backColor);
        paint.setStyle(Paint.Style.FILL);
        drawPoly(canvas, backPoints, paint);
    }


    private void drawGraph(Canvas canvas, int radius) {
        final int size = valueHolders.length;
        Point[] points = new Point[size];
        Point[] backPoints = new Point[size];
        for (int i = 0; i < size; i++) {
            float value = valueHolders[i].value * radius * animValue;
            points[i] = computePoint(size, i, value);

            float backValue = radius * animValue;
            backPoints[i] = computePoint(size, i, backValue);
        }

        paint.reset();
        paint.setColor(fillColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        drawPoly(canvas, points, paint);
    }

    private void drawPoly(Canvas canvas, Point[] points, Paint paint) {
        path.reset();
        path.moveTo(points[0].x, points[0].y);
        for (Point point : points) {
            path.lineTo(point.x, point.y);
        }
        path.lineTo(points[0].x, points[0].y);
        canvas.drawPath(path, paint);
    }

    private Point computePoint(int size, int position, float radius) {
        double angle = ((Math.PI * 2) / size * position) - (Math.PI / 2);
        int x = (int) (getWidth() / 2 + Math.cos(angle) * radius);
        int y = (int) (getHeight() / 2 + Math.sin(angle) * radius);
        return new Point(x, y);
    }

    private class Point {

        float x = 0;
        float y = 0;

        Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private void setupForEditMode() {
        animValue = 1;

        final int size = 5;
        valueHolders = new ValueHolder[size];
        for(int i=0; i<size; i++) {
            valueHolders[i] = new ValueHolder("label" + i, 0.8f, Color.BLACK);
        }
    }

    public static abstract class Adapter<VH extends ValueHolder> {

        public abstract VH onCreateValueHolder(int position);

        public abstract int getSize();
    }

    public static class ValueHolder {

        public float value;
        public String title;
        public int color;

        public ValueHolder(String title, @FloatRange(from = 0, to = 1.0) float value, @ColorInt int color) {
            this.title = title;
            this.value = Math.min(Math.max(value, 0), 1);
            this.color = color;
        }
    }
}