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
import android.widget.Button;
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
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener {
    private static final String PHASE_KEY = "PHASE";
    private static final String START_TIME_KEY = "START_TIME";
    private static final String PAUSED_TIME_KEY = "PAUSED_TIME";
    private static final String START_PAUSE_KEY = "START_PAUSE_LABEL";

    private Map<TimerPhase, Integer> times = new HashMap<TimerPhase, Integer>();

    private float startTime = SystemClock.elapsedRealtime();
    private TimerPhase phase = TimerPhase.PREP;
    private float pausedTime = 0;

    private ScheduledFuture<?> ticker = null;
    private MediaPlayer alertPlayer = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timer);

        Intent intent = getIntent();
        for (TimerPhase p: TimerPhase.values()) {
            times.put(
                p,
                intent.getIntExtra(p.getBundleConfigKey(), p.getDefaultTime())
            );
        }

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
        outState.putFloat(START_TIME_KEY, startTime);
        outState.putFloat(PAUSED_TIME_KEY, pausedTime);
        for (Map.Entry<TimerPhase, Integer> entry: times.entrySet()) {
            outState.putInt(entry.getKey().getConfigKey(), entry.getValue());
        }

        Button startPauseButton = (Button)findViewById(R.id.startPauseButton);
        outState.putCharSequence(START_PAUSE_KEY, startPauseButton.getText());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        phase = (TimerPhase)savedInstanceState.getSerializable(PHASE_KEY);
        startTime = savedInstanceState.getFloat(START_TIME_KEY);
        pausedTime = savedInstanceState.getFloat(PAUSED_TIME_KEY);
        for (TimerPhase p: TimerPhase.values()) {
            times.put(p, savedInstanceState.getInt(p.getConfigKey()));
        }

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
        TextView timeLabel = (TextView)findViewById(R.id.timeLabel);
        TextView phaseLabel = (TextView)findViewById(R.id.phaseLabel);

        float now = pausedTime > 0
            ? startTime + pausedTime
            : SystemClock.elapsedRealtime();
        float target = startTime + (times.get(phase) * 1000);
        float secondsLeft = (target - now) / 1000;

        int wholeSecondsLeft = (int)secondsLeft;
        if (wholeSecondsLeft < 0) {
            wholeSecondsLeft = 0;
        }
        timeLabel.setText(
            String.format(
                "%02d:%02d",
                wholeSecondsLeft / 60,
                wholeSecondsLeft % 60
            )
        );
        phaseLabel.setText(getText(phase.getTimerLabel()));

        if (secondsLeft <= 0) {
            changePhase();
        }
    }

    private void changePhase() {
        startTime = SystemClock.elapsedRealtime();
        phase = phase.next();

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
}
