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

public class Timer implements Comparable<Timer> {
    private final Runnable what;
    private final long delay;
    private long when;

    // call in n milliseconds
    public Timer(long milliseconds, Runnable what) {
        this.delay = milliseconds;
        this.when = getCurrentTicks() + delay;
        this.what = what;
    }

    public void reset() {
        this.when = getCurrentTicks() + delay;
    }

    // base reference for timers
    public static long getCurrentTicks() {
        return System.currentTimeMillis();
    }

    public boolean hasExpired() {
        return getExpirationDelta() <= 0;
    }

    public long getExpirationDelta() {
        return when - getCurrentTicks();
    }

    public void invoke() {
        what.run();
    }

    @Override
    public int compareTo(Timer o) {
        if(this.when < o.when) return -1;
        else if(this.when > o.when) return 1;
        else return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((what == null) ? 0 : what.hashCode());
        result = prime * result + (int) (when ^ (when >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Timer other = (Timer) obj;
        if (what == null) {
            if (other.what != null)
                return false;
        } else if (!what.equals(other.what))
            return false;
        if (when != other.when)
            return false;
        return true;
    }
}
