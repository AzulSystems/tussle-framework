package org.benchmarks.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Interval {
    public long start;
    public long finish;
    public String name = "";

    public boolean contains(long start, long finish) {
        return start <= finish && this.start < finish && start <= this.finish;
    }

    public void adjust(long stamp) {
        if (start != Long.MIN_VALUE) {
            start += stamp;
        }
        if (finish != Long.MAX_VALUE) {
            finish += stamp;
        }
    }

    public void reset() {
        start = Long.MIN_VALUE;
        finish = Long.MAX_VALUE;
    }

    public void update(long s, long f) {
        update(s);
        update(f);
    }

    public void update(long stamp) {
        if (start == Long.MIN_VALUE || start > stamp)
            start = stamp;
        if (finish == Long.MAX_VALUE || finish < stamp)
            finish = stamp;
    }

    public static String toString(long stamp) {
        if (stamp == Long.MIN_VALUE) {
            return "MIN";
        }
        if (stamp == Long.MAX_VALUE) {
            return "MAX";
        }
        return stamp / 1000 + "";
    }

    @Override
    public String toString() {
        return toString(start) + "," + toString(finish) + "," + name;
    }
}
