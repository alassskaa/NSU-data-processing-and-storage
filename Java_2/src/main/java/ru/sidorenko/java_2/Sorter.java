package ru.sidorenko.java_2;

import java.util.concurrent.atomic.AtomicLong;

public final class Sorter implements Runnable {

    private final SortableList list;
    private final SortSettings settings;
    private final AtomicLong steps = new AtomicLong();

    public Sorter(SortableList list, SortSettings settings) {
        this.list = list;
        this.settings = settings;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                sortPass();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public long steps() {
        return steps.get();
    }

    private void sortPass() throws InterruptedException {
        BubblePass pass = list.newPass();
        boolean stepped = false;
        while (pass.step(settings.insideStepMillis())) {
            steps.incrementAndGet();
            stepped = true;
            Thread.sleep(settings.betweenStepsMillis());
        }
        if (!stepped) {
            Thread.sleep(settings.betweenStepsMillis());
        }
    }
}
