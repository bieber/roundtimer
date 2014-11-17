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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import java.util.HashMap;
import java.util.Map;

public class TimeSelection
    extends Activity
    implements SeekBar.OnSeekBarChangeListener, Button.OnClickListener {

    private static final Map<TimerPhase, TextView> outputLabels =
        new HashMap<TimerPhase, TextView>();
    private static final Map<TimerPhase, SeekBar> inputBars =
        new HashMap<TimerPhase, SeekBar>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timeselection);

        TableLayout inputTable = (TableLayout)findViewById(R.id.inputTable);
        for (TimerPhase p: TimerPhase.values()) {
            this.setupRow(inputTable, p);
        }

        Button startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(this);
    }

    private void setupRow(TableLayout inputTable, TimerPhase phase) {
        addLabelRow(inputTable, phase);
        addSeekBarRow(inputTable, phase);
        setTime(
            phase,
            getPreferences(MODE_PRIVATE).getInt(
                phase.getConfigKey(),
                phase.getDefaultTime()
            )
        );
    }

    private void addLabelRow(TableLayout inputTable, TimerPhase phase) {
        TextView label = new TextView(this);
        label.setText(getText(phase.getInputLabel())+":");
        label.setTextAppearance(this, android.R.style.TextAppearance_Large);

        TableRow.LayoutParams labelLayout = new TableRow.LayoutParams(0);
        labelLayout.leftMargin = getDim(R.dimen.time_label_left_margin);
        labelLayout.rightMargin = getDim(R.dimen.time_label_right_margin);
        label.setLayoutParams(labelLayout);

        TextView outputLabel = new TextView(this);
        outputLabels.put(phase, outputLabel);
        outputLabel.setTextAppearance(
            this,
            android.R.style.TextAppearance_Large
        );

        TableRow.LayoutParams outputLabelLayout = new TableRow.LayoutParams(1);
        outputLabelLayout.leftMargin = getDim(R.dimen.time_output_left_margin);
        outputLabel.setLayoutParams(outputLabelLayout);

        TableRow row = new TableRow(this);
        row.addView(label);
        row.addView(outputLabel);
        inputTable.addView(row);
    }

    private void addSeekBarRow(TableLayout inputTable, TimerPhase phase) {
        SeekBar bar = new SeekBar(this);
        inputBars.put(phase, bar);
        bar.setMax(phase.getMaximum());
        bar.setOnSeekBarChangeListener(this);

        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(0);
        layoutParams.weight = 1;
        layoutParams.leftMargin = getDim(R.dimen.time_seekbar_left_margin);
        layoutParams.rightMargin = getDim(R.dimen.time_seekbar_right_margin);
        bar.setLayoutParams(layoutParams);

        TableRow row = new TableRow(this);
        row.addView(bar);
        inputTable.addView(row);
    }

    @Override
    public void onProgressChanged(
        SeekBar seekBar,
        int progress,
        boolean fromUser
    ) {
        if (!fromUser) {
            return;
        }
        for (TimerPhase p: TimerPhase.values()) {
            if (inputBars.get(p) == seekBar) {
                setTime(p, progress);
                return;
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.startButton) {
            Intent timer = new Intent(this, Timer.class);
            for (TimerPhase p: TimerPhase.values()) {
                timer.putExtra(
                    p.getBundleConfigKey(),
                    inputBars.get(p).getProgress()
                );
            }
            startActivity(timer);
        }
    }

    private void setTime(TimerPhase phase, int time) {
        time = snapToInterval(time, phase.getAdjustmentInterval());
        if (time == 0 && phase != TimerPhase.PREP) {
            time = phase.getAdjustmentInterval();
        }

        outputLabels.get(phase)
            .setText(String.format("%02d:%02d", time / 60, time % 60));
        inputBars.get(phase).setProgress(time);

        getPreferences(MODE_PRIVATE)
            .edit()
            .putInt(phase.getConfigKey(), time)
            .commit();
    }

    private int getDim(int id) {
        return (int)getResources().getDimension(id);
    }

    private static int snapToInterval(int x, int interval) {
        return ((int)Math.round((double)x / interval)) * interval;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

}