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
    private static final float textPaddingRatio = 0.09f;
    private static final float textInternalPaddingRatio = 0.02f;
    private static final float textStrokeRatio = 0.02f;
    private static final float textPhaseSplitRatio = 1/5.f;
    private static final float textRoundSplitRatio = 2/5.f;

    private RectF outerBound = new RectF();
    private Rect measuredSize = new Rect();

    private RectF phaseLabelBound = new RectF();
    private RectF roundLabelBound = new RectF();
    private RectF timeLabelBound = new RectF();
    float phaseFontSize = 0;
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
    private int round = 0;
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

    public void setRound(int round) {
        int oldRound = this.round;
        this.round = round;
        if (round != oldRound) {
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);

        float minDim = width < height ? width : height;
        float textPadding = minDim * textPaddingRatio;
        float textInternalPadding = minDim * textInternalPaddingRatio;
        float phaseBoundary =
            textPadding + (height - 2*textPadding)*textPhaseSplitRatio;
        float roundBoundary =
            textPadding + (height - 2*textPadding)*textRoundSplitRatio;

        String[] phaseLabels = new String[TimerPhase.values().length];
        for (TimerPhase p: TimerPhase.values()) {
            String label = getContext()
                .getResources()
                .getString(p.getTimerLabel());
            phaseLabels[p.ordinal()] = label;
        }

        phaseLabelBound.set(
            textPadding,
            textPadding,
            width - textPadding,
            phaseBoundary - textInternalPadding / 2
        );
        phaseFontSize = getFontSize(phaseLabelBound, textPaint, phaseLabels);

        String roundFormat = getContext()
            .getResources()
            .getString(R.string.round_label_format);
        roundLabelBound.set(
            textPadding,
            phaseBoundary + textInternalPadding / 2,
            width - textPadding,
            roundBoundary - textInternalPadding / 2
        );
        String [] roundLabels = {String.format(roundFormat, 999)};
        roundFontSize = getFontSize(
            roundLabelBound,
            textPaint,
            roundLabels
        );

        timeLabelBound.set(
            textPadding,
            roundBoundary + textInternalPadding / 2,
            width - textPadding,
            height - textPadding
        );
        timeFontSize = getFontSize(timeLabelBound, textPaint, timeStrings);
    }

    float getFontSize(RectF bounds, Paint paint, String[] options) {
        paint.setTextSize(50);
        int widestIndex = 0;
        int tallestIndex = 0;
        float maxWidth = 0;
        float maxHeight = 0;

        for (int i = 0; i < options.length; i++) {
            String s = options[i];
            paint.getTextBounds(s, 0, s.length(), measuredSize);
            if (measuredSize.width() > maxWidth) {
                maxWidth = measuredSize.width();
                widestIndex = i;
            }
            if (measuredSize.height() > maxHeight) {
                maxHeight = measuredSize.height();
                tallestIndex = i;
            }
        }

        float widthRatio = bounds.width() / maxWidth;
        float heightRatio = bounds.height() / maxHeight;
        float candidateSize = Math.min(widthRatio, heightRatio) * 50;
        int testIndex = widthRatio < heightRatio ? widestIndex : tallestIndex;
        String testString = options[testIndex];

        for (boolean fits = false; !fits; candidateSize--) {
            paint.setTextSize(candidateSize);
            paint.getTextBounds(
                testString,
                0,
                testString.length(),
                measuredSize
            );

            fits = measuredSize.width() <= bounds.width() &&
                measuredSize.height() <= bounds.height();
        }
        return candidateSize;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int minDim = width < height ? width : height;

        float borderThickness = borderRatio * minDim;
        float cornerRadius = cornerRatio * minDim;
        outerBound.set(
            borderThickness,
            borderThickness,
            width - borderThickness,
            height - borderThickness
        );

        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(
            getContext().getResources().getColor(phase.getColor())
        );

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(
            getContext().getResources().getColor(R.color.border_color)
        );
        borderPaint.setStrokeWidth(borderThickness);
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

        String phaseText = getContext()
            .getResources()
            .getString(phase.getTimerLabel());
        drawText(canvas, phaseText, phaseFontSize, phaseLabelBound);

        if (round != 0) {
            String roundFormat = getContext()
                .getResources()
                .getString(R.string.round_label_format);
            String roundText = String.format(roundFormat, round);
            drawText(canvas, roundText, roundFontSize, roundLabelBound);
        }

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
