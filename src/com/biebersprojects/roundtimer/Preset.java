package com.biebersprojects.roundtimer;

import android.content.Context;
import android.content.res.Resources;

import java.io.Serializable;
import java.util.*;

public class Preset implements Comparable<Preset>, Serializable {
    private String name;
    private Map<TimerPhase, Integer> times;

    public Preset(String name, Map<TimerPhase, Integer> times) {
        this.name = name;
        this.times = times;
    }

    public String getName() {
        return name;
    }

    public Map<TimerPhase, Integer> getTimes() {
        return times;
    }

    public String toString() {
        return name;
    }

    public static List<Preset> defaultSet(Context context) {
        Resources resources = context.getResources();

        Map<TimerPhase, Integer> boxingProTimes =
            new HashMap<TimerPhase, Integer>();
        boxingProTimes.put(TimerPhase.PREP, 30);
        boxingProTimes.put(TimerPhase.ROUND, 180);
        boxingProTimes.put(TimerPhase.REST, 60);

        Map<TimerPhase, Integer> boxingAmateurTimes =
            new HashMap<TimerPhase, Integer>();
        boxingAmateurTimes.put(TimerPhase.PREP, 30);
        boxingAmateurTimes.put(TimerPhase.ROUND, 120);
        boxingAmateurTimes.put(TimerPhase.REST, 60);

        Map<TimerPhase, Integer> mmaProTimes =
            new HashMap<TimerPhase, Integer>();
        mmaProTimes.put(TimerPhase.PREP, 30);
        mmaProTimes.put(TimerPhase.ROUND, 300);
        mmaProTimes.put(TimerPhase.REST, 60);

        Map<TimerPhase, Integer> mmaAmateurTimes =
            new HashMap<TimerPhase, Integer>();
        mmaAmateurTimes.put(TimerPhase.PREP, 30);
        mmaAmateurTimes.put(TimerPhase.ROUND, 180);
        mmaAmateurTimes.put(TimerPhase.REST, 60);

        return new ArrayList<Preset>(
            Arrays.asList(
                new Preset(
                    resources.getString(R.string.boxing_pro),
                    boxingProTimes
                ),
                new Preset(
                    resources.getString(R.string.boxing_amateur),
                    boxingAmateurTimes
                ),
                new Preset(
                    resources.getString(R.string.mma_pro),
                    mmaProTimes
                ),
                new Preset(
                    resources.getString(R.string.mma_amateur),
                    mmaAmateurTimes
                )
            )
        );
    }

    @Override
    public int compareTo(Preset another) {
        return this.name.compareTo(another.name);
    }
}
