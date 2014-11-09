package com.biebersprojects.roundtimer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class TimeSelection
    extends Activity
    implements SeekBar.OnSeekBarChangeListener {

    private static Map<Integer, Integer> OUTPUT_LABELS;
    static {
        OUTPUT_LABELS = new HashMap<Integer, Integer>();
        OUTPUT_LABELS.put(R.id.prepTimeSeekBar, R.id.prepTimeOutput);
        OUTPUT_LABELS.put(R.id.roundTimeSeekBar, R.id.roundTimeOutput);
        OUTPUT_LABELS.put(R.id.restTimeSeekBar, R.id.restTimeOutput);
    }

    private static Map<Integer, Integer> INTERVALS;
    static {
        INTERVALS = new HashMap<Integer, Integer>();
        INTERVALS.put(R.id.prepTimeSeekBar, 15);
        INTERVALS.put(R.id.roundTimeSeekBar, 30);
        INTERVALS.put(R.id.restTimeSeekBar, 30);
    }

    private static Map<Integer, String> PREFERENCE_KEYS;
    static {
        PREFERENCE_KEYS = new HashMap<Integer, String>();
        PREFERENCE_KEYS.put(R.id.prepTimeSeekBar, "prep_time");
        PREFERENCE_KEYS.put(R.id.roundTimeSeekBar, "round_time");
        PREFERENCE_KEYS.put(R.id.restTimeSeekBar, "rest_time");
    }

    private static Map<Integer, Integer> DEFAULT_TIMES;
    static {
        DEFAULT_TIMES = new HashMap<Integer, Integer>();
        DEFAULT_TIMES.put(R.id.prepTimeSeekBar, 30);
        DEFAULT_TIMES.put(R.id.roundTimeSeekBar, 120);
        DEFAULT_TIMES.put(R.id.restTimeSeekBar, 60);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timeselection);

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        for (int id: OUTPUT_LABELS.keySet()) {
            SeekBar bar = (SeekBar)findViewById(id);
            bar.setOnSeekBarChangeListener(this);
            this.setTime(
                bar,
                preferences.getInt(
                    PREFERENCE_KEYS.get(bar.getId()),
                    DEFAULT_TIMES.get(bar.getId())
                )
            );
        }
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
        this.setTime(seekBar, progress);
    }

    private void setTime(SeekBar input, int value) {
        int newValue = snapToInterval(value, INTERVALS.get(input.getId()));
        input.setProgress(newValue);

        int minutes = newValue / 60;
        int seconds = newValue % 60;
        int outputID = OUTPUT_LABELS.get(input.getId());
        TextView outputLabel = (TextView)findViewById(outputID);
        outputLabel.setText(String.format("%02d:%02d", minutes, seconds));

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        preferences
            .edit()
            .putInt(PREFERENCE_KEYS.get(input.getId()), newValue)
            .apply();
    }

    private static int snapToInterval(int x, int interval) {
        double floatQuotient = (double)x / interval;
        int lower = (int)Math.floor(floatQuotient) * interval;
        int upper = (int)Math.ceil(floatQuotient) * interval;
        if (upper - x < x - lower) {
            return upper;
        } else {
            return lower;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}