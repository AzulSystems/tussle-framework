package org.benchmarks.metrics;

import java.util.Queue;

import org.HdrHistogram.AbstractHistogram;

public class MovingWindowSumHistogram {
    public AbstractHistogram sumHistogram;
    public Queue<AbstractHistogram> movingWindowQueue;
    public double percentile;
    public long movingWindowMs;

    public MovingWindowSumHistogram(AbstractHistogram sumHistogram, Queue<AbstractHistogram> movingWindowQueue, double percentile, int movingWindow) {
        this.sumHistogram = sumHistogram;
        this.movingWindowQueue = movingWindowQueue;
        this.percentile = percentile;
        this.movingWindowMs = movingWindow * 1000L;
    }

    public void add(AbstractHistogram intervalHistogram) {
        long windowCutOffTimeStamp = intervalHistogram.getEndTimeStamp() - movingWindowMs;
        sumHistogram.add(intervalHistogram);
        AbstractHistogram head = movingWindowQueue.peek();
        while (head != null && head.getEndTimeStamp() <= windowCutOffTimeStamp) {
            AbstractHistogram prevHist = movingWindowQueue.remove();
            sumHistogram.subtract(prevHist);
            head = movingWindowQueue.peek();
        }
        movingWindowQueue.add(intervalHistogram);
    }
}
