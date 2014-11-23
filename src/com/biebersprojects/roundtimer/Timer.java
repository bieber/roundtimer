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
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Timer
    extends Activity
    implements
        Button.OnClickListener,
        CheckBox.OnCheckedChangeListener,
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener {

    private static final String PHASE_KEY = "PHASE";
    private static final String ROUND_KEY = "ROUND";
    private static final String START_TIME_KEY = "START_TIME";
    private static final String PAUSED_TIME_KEY = "PAUSED_TIME";
    private static final String START_PAUSE_KEY = "START_PAUSE_LABEL";
    private static final String KEEP_SCREEN_ON_KEY = "KEEP_SCREEN_ON";

    private Map<TimerPhase, Integer> times = new HashMap<TimerPhase, Integer>();

    private TimerPhase phase = TimerPhase.PREP;
    private int round = 0;
    private float startTime = SystemClock.elapsedRealtime();
    private float pausedTime = 0;

    private ScheduledFuture<?> ticker = null;
    private MediaPlayer alertPlayer = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timer);
        setKeepScreenOn(true);

        Intent intent = getIntent();
        for (TimerPhase p: TimerPhase.values()) {
            times.put(
                p,
                intent.getIntExtra(p.getBundleConfigKey(), p.getDefaultTime())
            );
        }

        CheckBox keepScreenOnBox = (CheckBox)findViewById(R.id.keepScreenOnBox);
        keepScreenOnBox.setOnCheckedChangeListener(this);

        Button pauseStartButton = (Button)findViewById(R.id.startPauseButton);
        pauseStartButton.setOnClickListener(this);

        ticker = Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    tick();
                                }
                            }
                        );
                    }
                },
                0,
                10,
                TimeUnit.MILLISECONDS
            );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(PHASE_KEY, phase);
        outState.putInt(ROUND_KEY, round);
        outState.putFloat(START_TIME_KEY, startTime);
        outState.putFloat(PAUSED_TIME_KEY, pausedTime);
        for (Map.Entry<TimerPhase, Integer> entry: times.entrySet()) {
            outState.putInt(entry.getKey().getConfigKey(), entry.getValue());
        }

        CheckBox keepScreenOnBox = (CheckBox)findViewById(R.id.keepScreenOnBox);
        outState.putBoolean(KEEP_SCREEN_ON_KEY, keepScreenOnBox.isChecked());

        Button startPauseButton = (Button)findViewById(R.id.startPauseButton);
        outState.putCharSequence(START_PAUSE_KEY, startPauseButton.getText());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        phase = (TimerPhase)savedInstanceState.getSerializable(PHASE_KEY);
        round = savedInstanceState.getInt(ROUND_KEY);
        startTime = savedInstanceState.getFloat(START_TIME_KEY);
        pausedTime = savedInstanceState.getFloat(PAUSED_TIME_KEY);
        for (TimerPhase p: TimerPhase.values()) {
            times.put(p, savedInstanceState.getInt(p.getConfigKey()));
        }

        CheckBox keepScreenOnBox = (CheckBox)findViewById(R.id.keepScreenOnBox);
        keepScreenOnBox.setChecked(
            savedInstanceState.getBoolean(KEEP_SCREEN_ON_KEY)
        );
        setKeepScreenOn(savedInstanceState.getBoolean(KEEP_SCREEN_ON_KEY));

        Button startPauseButton = (Button)findViewById(R.id.startPauseButton);
        startPauseButton.setText(
            savedInstanceState.getCharSequence(START_PAUSE_KEY)
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ticker.cancel(true);
    }

    @Override
    public void onClick(View v) {
        Button startPauseButton = (Button)findViewById(R.id.startPauseButton);
        if (v.getId() == R.id.startPauseButton) {
            tick();
            if (pausedTime == 0) {
                startPauseButton.setText(getText(R.string.start_label));
                pausedTime = SystemClock.elapsedRealtime() - startTime;
            } else {
                startPauseButton.setText(getText(R.string.pause_label));
                startTime = SystemClock.elapsedRealtime() - pausedTime;
                pausedTime = 0;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.keepScreenOnBox) {
            setKeepScreenOn(isChecked);
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (alertPlayer == null) {
            return;
        }

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                alertPlayer.setVolume(1.0f, 1.0f);
                if (!alertPlayer.isPlaying()) {
                    alertPlayer.start();
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                alertPlayer.stop();
                alertPlayer.release();
                alertPlayer = null;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                alertPlayer.pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                alertPlayer.setVolume(0.5f, 0.5f);
                break;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.release();
        alertPlayer = null;
        ((AudioManager)getSystemService(Context.AUDIO_SERVICE))
            .abandonAudioFocus(this);
    }

    private void tick() {
        ClockView clock = (ClockView)findViewById(R.id.display);

        float now = pausedTime > 0
            ? startTime + pausedTime
            : SystemClock.elapsedRealtime();
        float target = startTime + (times.get(phase) * 1000);
        float secondsLeft = (target - now) / 1000;

        int wholeSecondsLeft = (int)secondsLeft;
        if (wholeSecondsLeft < 0) {
            wholeSecondsLeft = 0;
        }
        clock.setSecondsLeft(wholeSecondsLeft);
        clock.setPhase(phase);
        clock.setRound(round);

        if (secondsLeft <= 0) {
            changePhase();
        }
    }

    private void changePhase() {
        startTime = SystemClock.elapsedRealtime();
        phase = phase.next();
        if (phase == TimerPhase.ROUND) {
            round++;
        }

        int gotFocus = ((AudioManager)getSystemService(Context.AUDIO_SERVICE))
            .requestAudioFocus(
                this,
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            );
        if (gotFocus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        int alertTone = phase == TimerPhase.ROUND
            ? R.raw.round_tone
            : R.raw.rest_tone;
        alertPlayer = MediaPlayer.create(this, alertTone);
        alertPlayer.setOnCompletionListener(this);
        alertPlayer.start();
    }

    private void setKeepScreenOn(boolean keepScreenOn) {
        if (keepScreenOn) {
            getWindow()
                .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow()
                .clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
