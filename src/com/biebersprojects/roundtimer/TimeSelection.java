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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.util.*;

public class TimeSelection
    extends Activity
    implements
        SeekBar.OnSeekBarChangeListener,
        Button.OnClickListener,
        AdapterView.OnItemClickListener {

    private static final String PRESETS_FILE = "presets";

    private static final Map<TimerPhase, TextView> outputLabels =
        new HashMap<TimerPhase, TextView>();
    private static final Map<TimerPhase, SeekBar> inputBars =
        new HashMap<TimerPhase, SeekBar>();

    private List<Preset> presets = null;

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

        Button addPresetButton = (Button)findViewById(R.id.addPresetButton);
        addPresetButton.setOnClickListener(this);

        ListView presetView = (ListView)findViewById(R.id.presetView);
        presetView.setOnItemClickListener(this);
        loadPresets();
        registerForContextMenu(presetView);
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
        } else if (v.getId() == R.id.addPresetButton) {
            startAddDialog();
        }
    }

    private void startAddDialog() {
        LinearLayout outer = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        );

        int margin =
            (int)getResources().getDimension(R.dimen.dialog_margin);
        params.setMargins(margin, margin, margin, margin);

        final EditText textBox = new EditText(this);
        textBox.setLayoutParams(params);
        outer.addView(textBox);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
            .setMessage(getResources().getString(R.string.add_preset_title))
            .setView(outer);

        builder.setPositiveButton(
            R.string.add_preset_label,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            }
        );

        builder.setNegativeButton(
            R.string.cancel_preset_label,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            }
        );

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (textBox.getText().length() != 0) {
                        addPreset(textBox.getText().toString());
                        dialog.dismiss();
                    }
                }
            }
        );
        textBox.addTextChangedListener(
            new TextWatcher() {
                @Override
                public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
                ) {}

                @Override
                public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
                ) {
                    dialog
                        .getButton(AlertDialog.BUTTON_POSITIVE)
                        .setEnabled(s.length() != 0);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            }
        );
    }

    private void addPreset(String name) {
        Map<TimerPhase, Integer> times = new HashMap<TimerPhase, Integer>();
        for (TimerPhase p: TimerPhase.values()) {
            times.put(
                p,
                getPreferences(MODE_PRIVATE).getInt(p.getConfigKey(), 0)
            );
        }
        presets.add(new Preset(name, times));
        updatePresets();
    }

    @Override
    public void onItemClick(
        AdapterView<?> parent,
        View view,
        int position,
        long id
    ) {
        Preset preset = presets.get(position);
        for (Map.Entry<TimerPhase, Integer> t: preset.getTimes().entrySet()) {
            setTime(t.getKey(), t.getValue());
        }
    }

    @Override
    public void onCreateContextMenu(
        ContextMenu menu,
        View v,
        ContextMenu.ContextMenuInfo menuInfo
    ) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.preset, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.deletePreset) {
            AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

            presets.remove(info.position);
            updatePresets();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void loadPresets() {
        try {
            InputStream fin = openFileInput(PRESETS_FILE);
            ObjectInput oin = new ObjectInputStream(fin);
            presets = (List<Preset>)oin.readObject();
        } catch (FileNotFoundException e) {
            presets = Preset.defaultSet(this);
        } catch (StreamCorruptedException e) {
            presets = Preset.defaultSet(this);
        } catch (IOException e) {
            presets = Preset.defaultSet(this);
        } catch (ClassNotFoundException e) {
            presets = Preset.defaultSet(this);
        } catch (ClassCastException e) {
            presets = Preset.defaultSet(this);
        }
        updatePresets();
    }

    private void updatePresets() {
        Collections.sort(presets);
        ListView list = (ListView)findViewById(R.id.presetView);
        list.setAdapter(
            new ArrayAdapter<Preset>(
                this,
                android.R.layout.simple_list_item_1,
                presets
            )
        );

        try {
            OutputStream fout = openFileOutput(PRESETS_FILE, MODE_PRIVATE);
            ObjectOutput oout = new ObjectOutputStream(fout);
            oout.writeObject(presets);
            fout.close();
        } catch (FileNotFoundException e) {
            // This shouldn't be able to happen opening an output file
        } catch (IOException e) {
            // Not really anything constructive we can do here
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