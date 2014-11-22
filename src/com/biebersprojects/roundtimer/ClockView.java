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
    private static final float textStrokeRatio = 0.02f;
    private static final float textTimeSplitRatio = 1/3.f;

    private RectF outerBound = new RectF();
    private Rect measuredSize = new Rect();

    private RectF roundLabelBound = new RectF();
    private RectF timeLabelBound = new RectF();
    float roundFontSize = 0;
    float timeFontSize = 0;

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
        int oldSecondsLeft = this.secondsLeft;
        this.secondsLeft = secondsLeft;
        if (secondsLeft != oldSecondsLeft) {
            invalidate();
        }
    }

    public void setPhase(TimerPhase phase) {
        TimerPhase oldPhase = this.phase;
        this.phase = phase;
        if (phase != oldPhase) {
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);

        float minDim = width < height ? width : height;
        float labelBoundary = height * textTimeSplitRatio;
        float textPadding = minDim * textPaddingRatio;

        String[] roundLabels = new String[TimerPhase.values().length];
        for (TimerPhase p: TimerPhase.values()) {
            String label = getContext()
                .getResources()
                .getString(p.getTimerLabel());
            roundLabels[p.ordinal()] = label;
        }

        roundLabelBound.set(
            textPadding / 2,
            textPadding / 2,
            width - textPadding / 2,
            labelBoundary - textPadding / 2
        );
        roundFontSize = getFontSize(roundLabelBound, textPaint, roundLabels);


        timeLabelBound.set(
            textPadding / 2,
            labelBoundary + textPadding / 2,
            width - textPadding / 2,
            height - textPadding
        );
        timeFontSize = getFontSize(timeLabelBound, textPaint, timeStrings);
    }

    float getFontSize(RectF bounds, Paint paint, String[] options) {
        paint.setTextSize(50);
        float maxWidth = 0;
        float maxHeight = 0;

        for (String s: options) {
            paint.getTextBounds(s, 0, s.length(), measuredSize);
            if (measuredSize.width() > maxWidth) {
                maxWidth = measuredSize.width();
            }
            if (measuredSize.height() > maxHeight) {
                maxHeight = measuredSize.height();
            }
        }

        float widthRatio = bounds.width() / maxWidth;
        float heightRatio = bounds.height() / maxHeight;
        return Math.min(widthRatio, heightRatio) * 50;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int minDim = width < height ? width : height;

        float borderWidth = borderRatio * minDim;
        float cornerRadius = cornerRatio * minDim;
        outerBound.set(
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
            outerBound,
            cornerRadius,
            cornerRadius,
            backgroundPaint
        );
        canvas.drawRoundRect(
            outerBound,
            cornerRadius,
            cornerRadius,
            borderPaint
        );

        String roundText = getContext()
            .getResources()
            .getString(phase.getTimerLabel());
        drawText(canvas, roundText, roundFontSize, roundLabelBound);

        String timeText = String.format(
            "%02d:%02d",
            secondsLeft / 60,
            secondsLeft % 60
        );
        drawText(canvas, timeText, timeFontSize, timeLabelBound);
    }

    private void drawText(
        Canvas canvas,
        String text,
        float fontSize,
        RectF bounds
    ) {
        textPaint.setTextSize(fontSize);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.getTextBounds(text, 0, text.length(), measuredSize);

        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        textPaint.setStrokeWidth(textStrokeRatio * measuredSize.height());
        textPaint.setColor(
            getContext().getResources().getColor(R.color.text_stroke_color)
        );
        canvas.drawText(
            text,
            bounds.left + (bounds.width() / 2),
            bounds.bottom - (bounds.height() - measuredSize.height()) / 2,
            textPaint
        );

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(
            getContext().getResources().getColor(R.color.text_color)
        );
        canvas.drawText(
            text,
            bounds.left + (bounds.width() / 2),
            bounds.bottom - (bounds.height() - measuredSize.height()) / 2,
            textPaint
        );
    }
}
