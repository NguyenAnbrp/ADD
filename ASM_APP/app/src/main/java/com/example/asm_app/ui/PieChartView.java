package com.example.asm_app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PieChartView extends View {

    public static class Segment {
        public final float value;
        public final int color;

        public Segment(float value, int color) {
            this.value = value;
            this.color = color;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private List<Segment> segments = new ArrayList<>();

    public PieChartView(Context context) {
        super(context);
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSegments(List<Segment> segments) {
        this.segments = segments != null ? segments : new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (segments == null || segments.isEmpty()) {
            return;
        }
        float total = 0f;
        for (Segment s : segments) {
            total += s.value;
        }
        if (total <= 0f) {
            return;
        }

        float size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        rect.set(left, top, left + size, top + size);

        float startAngle = -90f;
        for (Segment s : segments) {
            if (s.value <= 0) continue;
            float sweep = (s.value / total) * 360f;
            paint.setColor(s.color);
            canvas.drawArc(rect, startAngle, sweep, true, paint);
            startAngle += sweep;
        }
    }
}
