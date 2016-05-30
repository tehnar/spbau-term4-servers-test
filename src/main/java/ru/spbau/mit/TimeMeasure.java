package ru.spbau.mit;

/**
 * Created by Сева on 23.05.2016.
 */
public class TimeMeasure {
    private static final double MILLI = 1e6;
    private volatile long totalTime = 0;
    private volatile int cnt = 0;

    public Timer startNewTimer() {
        return new Timer();
    }

    public double mean() {
        return (double) totalTime / cnt / MILLI;
    }

    public class Timer {
        private long startTime = System.nanoTime();

        public void stop() {
            totalTime += System.nanoTime() - startTime;
            cnt++;
        }
    }
}
