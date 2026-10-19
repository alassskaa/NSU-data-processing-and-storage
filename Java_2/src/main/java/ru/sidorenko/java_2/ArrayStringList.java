package ru.sidorenko.java_2;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ArrayStringList implements SortableList {

    private final List<String> list = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void addFirst(String value) {
        list.add(0, Objects.requireNonNull(value));
    }

    @Override
    public int print(PrintStream out) {
        int count = 0;
        synchronized (list) {
            for (String value : list) {
                out.println(value);
                count++;
            }
        }
        return count;
    }

    @Override
    public List<String> snapshot() {
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    @Override
    public BubblePass newPass() {
        return new Pass();
    }

    private final class Pass implements BubblePass {

        private int index;

        @Override
        public boolean step(long insideStepMillis) throws InterruptedException {
            synchronized (list) {
                if (index + 1 >= list.size()) {
                    return false;
                }
                Thread.sleep(insideStepMillis);
                String first = list.get(index);
                String second = list.get(index + 1);
                if (first.compareTo(second) > 0) {
                    list.set(index, second);
                    list.set(index + 1, first);
                }
                index++;
                return true;
            }
        }
    }
}
