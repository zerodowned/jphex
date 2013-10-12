/*******************************************************************************
 * Copyright (c) 2013 Folke Will <folke.will@gmail.com>
 * 
 * This file is part of JPhex.
 * 
 * JPhex is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JPhex is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.solhost.folko.jphex;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Logger;

public class TimerQueue {
    private static final Logger log = Logger.getLogger("jphex.timerqueue");
    private static TimerQueue instance;
    private final PriorityBlockingQueue<Timer> timers;
    private final Thread timerThread;
    private boolean wantStop;

    private TimerQueue() {
        this.timers = new PriorityBlockingQueue<Timer>();
        this.timerThread = getTimerThread();
    }

    public static TimerQueue get() {
        if(instance == null) {
            log.severe("access to timer queue before init");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public static void init() {
        if(instance != null) {
            log.severe("timer queue initialized twice");
            return;
        }
        instance = new TimerQueue();
        instance.startTimerThread();
    }

    public void addTimer(Timer timer) {
        timers.add(timer);
        timerThread.interrupt();
    }

    private void timerLoop() {
        log.fine("TimerQueue active");
        while(!wantStop) {
            Timer first = null;
            try {
                first = timers.take();
                // log.finest("queue took timer expiring in " + first.getExpirationDelta());
            } catch (InterruptedException e1) {
                if(wantStop) {
                    return;
                } else if(first == null) {
                    continue;
                }
            }

            // check first timer
            if(first.hasExpired()) {
                // log.finest("running timer");
                first.invoke();
            } else {
                timers.add(first);
                try {
                    Thread.sleep(first.getExpirationDelta());
                } catch (InterruptedException e) {
                    // a new timer has been added or want to stop -> both ok to continue
                }
            }
        }
    }

    private Thread getTimerThread() {
        return new Thread() {
            @Override
            public void run() {
                timerLoop();
            }
        };
    }

    private void startTimerThread() {
        timerThread.start();
    }

    public void stop() {
        wantStop = true;
        timerThread.interrupt();
        try {
            timerThread.join();
        } catch (InterruptedException e) {
            // doesn't matter as we're stopping anyways
        }
    }
}
