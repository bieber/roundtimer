/*
 *  This file is part of roundtimer.
 *
 *  roundtimer is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  roundtimer is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with roundtimer.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.biebersprojects.roundtimer;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class ClockView extends View {
    private static final float borderRatio = 0.02f;
    private static final float cornerRatio = 0.04f;
    private static final float textPaddingRatio = 0.2f;
    private static final float textStrokeRatio = 0.003f;
    private static final float textTimeSplitRatio = 1/3.f;

    private RectF bound = new RectF();
    private RectF labelBound = new RectF();
    private Rect measuredSize = new Rect();

    private Paint backgroundPaint = new Paint();
    private Paint borderPaint = new Paint();
    private Paint textPaint = new Paint();

    private static String[] timeStrings = {
        "00:00",
        "11:11",
        "22:22",
        "33:33",
        "44:44",
        "55:55",
        "66:66",
        "77:77",
        "88:88",
        "99:99",
    };

    private TimerPhase phase = TimerPhase.PREP;
    private int secondsLeft = 0;

    public ClockView(Context context) {
        super(context);
    }

    public ClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSecondsLeft(int secondsLeft) {
        this.secondsLeft = secondsLeft;
        invalidate();
    }

    public void setPhase(TimerPhase phase) {
        this.phase = phase;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int minDim = width < height ? width : height;

        float borderWidth = borderRatio * minDim;
        float cornerRadius = cornerRatio * minDim;
        bound.set(
            borderWidth,
            borderWidth,
            width - borderWidth,
            height - borderWidth
        );

        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(
            getContext().getResources().getColor(phase.getColor())
        );

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(
            getContext().getResources().getColor(R.color.border_color)
        );
        borderPaint.setStrokeWidth(borderWidth);
        borderPaint.setStrokeJoin(Paint.Join.ROUND);
        borderPaint.setAntiAlias(true);

        canvas.drawRoundRect(
            bound,
            cornerRadius,
            cornerRadius,
            backgroundPaint
        );
        canvas.drawRoundRect(bound, cornerRadius, cornerRadius, borderPaint);

        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(
            getContext().getResources().getColor(R.color.text_color)
        );

        String[] labels = new String[TimerPhase.values().length];
        String label = null;
        for (TimerPhase p: TimerPhase.values()) {
            String thisLabel = getContext()
                .getResources()
                .getString(p.getTimerLabel());
            labels[p.ordinal()] = thisLabel;
            if (p == phase) {
                label = thisLabel;
            }
        }

        float labelBoundary = height * textTimeSplitRatio;
        float textPadding = minDim * textPaddingRatio;
        float strokeWidth = minDim * textStrokeRatio;

        labelBound.set(
            textPadding / 2,
            textPadding / 2,
            width - textPadding / 2,
            labelBoundary - textPadding / 2
        );
        drawText(canvas, labels, label, labelBound, strokeWidth);

        labelBound.set(
            textPadding / 2,
            labelBoundary + textPadding / 2,
            width - textPadding / 2,
            height - textPadding
        );
        drawText(
            canvas,
            timeStrings,
            String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60),
            labelBound,
            strokeWidth
        );
    }

    private void drawText(
        Canvas canvas,
        String[] options,
        String text,
        RectF bounds,
        float strokeWidth
    ) {
        int maxWidth = 0;
        int maxHeight = 0;
        float textSize = 1;

        while (maxWidth < bounds.width() && maxHeight < bounds.height()) {
            textPaint.setTextSize(textSize);
            for (String s : options) {
                textPaint.getTextBounds(s, 0, s.length(), measuredSize);
                if (measuredSize.width() > maxWidth) {
                    maxWidth = measuredSize.width();
                }
                if (measuredSize.height() > maxHeight) {
                    maxHeight = measuredSize.height();
                }
            }
            textSize++;
        }
        textPaint.setTextSize(textSize - 2);
        textPaint.getTextBounds(text, 0, text.length(), measuredSize);

        textPaint.setColor(
            getContext().getResources().getColor(R.color.text_color)
        );
        textPaint.setStyle(Paint.Style.FILL);
        canvas.drawText(
            text,
            bounds.left + (bounds.width() / 2),
            bounds.bottom - (bounds.height() - measuredSize.height()) / 2,
            textPaint
        );

        textPaint.setColor(
            getContext().getResources().getColor(R.color.text_stroke_color)
        );
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setStrokeWidth(strokeWidth);
        canvas.drawText(
            text,
            bounds.left + (bounds.width() / 2),
            bounds.bottom - (bounds.height() - measuredSize.height()) / 2,
            textPaint
        );
    }
}
