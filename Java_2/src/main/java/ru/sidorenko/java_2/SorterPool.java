package ru.sidorenko.java_2;

import java.util.ArrayList;
import java.util.List;

public final class SorterPool {

    private final List<Sorter> sorters = new ArrayList<>();
    private final List<Thread> threads = new ArrayList<>();
    private long startedAt;
    private long stoppedAt;
    private boolean stopped;

    public SorterPool(SortableList list, SortSettings settings) {
        for (int i = 1; i <= settings.threads(); i++) {
            Sorter sorter = new Sorter(list, settings);
            Thread thread = new Thread(sorter, "sorter-" + i);
            thread.setDaemon(true);
            sorters.add(sorter);
            threads.add(thread);
        }
    }

    public void start() {
        startedAt = System.nanoTime();
        threads.forEach(Thread::start);
    }

    public void stop() throws InterruptedException {
        stoppedAt = System.nanoTime();
        stopped = true;
        threads.forEach(Thread::interrupt);
        for (Thread thread : threads) {
            thread.join();
        }
    }

    public long[] stepsPerThread() {
        return sorters.stream()
                .mapToLong(Sorter::steps)
                .toArray();
    }

    public long totalSteps() {
        return sorters.stream()
                .mapToLong(Sorter::steps)
                .sum();
    }

    public double elapsedMillis() {
        long end = stopped ? stoppedAt : System.nanoTime();
        return (end - startedAt) / 1_000_000.0;
    }
}
