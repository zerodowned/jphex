package org.solhost.folko.jphex;

public class DayNightCycle {
    private final TimeListener listener;
    private final Timer hourTimer;
    private int hour;

    private static final int MORNING    = 6;
    private static final int FORENOON   = 10;
    private static final int NOON       = 12;
    private static final int AFTERNOON  = 14;
    private static final int EVENING    = 17;
    private static final int NIGHT      = 22;

    public interface TimeListener {
        public void onTimeChange(boolean phaseChanged);
    }

    public DayNightCycle(TimeListener listener, int secondsPerHour) {
        this.listener = listener;
        this.hour = 12;
        Runnable timerRun = new Runnable() {
            public void run() {
                onHourPassed();
                TimerQueue.get().addTimer(hourTimer);
            }
        };
        this.hourTimer = new Timer(secondsPerHour * 1000, timerRun);
    }

    public void start() {
        TimerQueue.get().addTimer(hourTimer);
    }

    public int getHour() {
        return hour;
    }

    private void onHourPassed() {
        hour = (hour + 1) % 24;
        boolean phaseChanged = (hour == MORNING || hour == FORENOON || hour == NOON || hour == AFTERNOON || hour == EVENING || hour == NIGHT);
        listener.onTimeChange(phaseChanged);
        hourTimer.reset();
        TimerQueue.get().addTimer(hourTimer);
    }

    public boolean isMorning() {
        return hour >= MORNING && hour < FORENOON;
    }

    public boolean isForeNoon() {
        return hour >= FORENOON && hour < NOON;
    }

    public boolean isNoon() {
        return hour >= NOON && hour < AFTERNOON;
    }

    public boolean isAfterNoon() {
        return hour >= AFTERNOON && hour < EVENING;
    }

    public boolean isEvening() {
        return hour >= EVENING && hour < NIGHT;
    }

    public boolean isNight() {
        return hour >= NIGHT || hour < MORNING;
    }

    public byte getLightLevel() {
        if(hour < NOON) {
            return (byte) ((NOON - hour) * 31 / NOON);
        } else {
            return (byte) ((hour - NOON) * 31 / NOON);
        }
    }
}
