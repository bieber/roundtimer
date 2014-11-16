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

public enum TimerPhase {
    PREP(
        R.string.prep_time_input_label,
        R.string.prep_time_timer_label,
        30,
        15,
        120,
        "PREP_TIME"
    ),
    ROUND(
        R.string.round_time_input_label,
        R.string.round_time_timer_label,
        120,
        30,
        300,
        "ROUND_TIME"
    ),
    REST(
        R.string.rest_time_input_label,
        R.string.rest_time_timer_label,
        60,
        30,
        300,
        "REST_TIME"
    );

    private int inputLabel;
    private int timerLabel;
    private int defaultTime;
    private int adjustmentInterval;
    private int maximum;
    private String configKey;

    TimerPhase(
        int inputLabel,
        int timingLabel,
        int defaultTime,
        int adjustmentInterval,
        int maximum,
        String configKey
    ) {
        this.inputLabel = inputLabel;
        this.timerLabel = timingLabel;
        this.defaultTime = defaultTime;
        this.adjustmentInterval = adjustmentInterval;
        this.maximum = maximum;
        this.configKey = configKey;
    }

    public int getInputLabel() {
        return inputLabel;
    }

    public int getTimerLabel() {
        return timerLabel;
    }

    public int getDefaultTime() {
        return defaultTime;
    }

    public int getAdjustmentInterval() {
        return adjustmentInterval;
    }

    public int getMaximum() {
        return maximum;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getBundleConfigKey() {
        return "com.biebersprojects.roundtimer."+configKey;
    }

    public TimerPhase next() {
        switch (this) {
            case PREP:
                return ROUND;
            case ROUND:
                return REST;
            case REST:
                return ROUND;
        }
        return null;
    }
}
