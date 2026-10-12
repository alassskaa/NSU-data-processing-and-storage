package ru.sidorenko.java_2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SortableListTest {

    @ParameterizedTest
    @EnumSource(ListKind.class)
    void passComparesEveryAdjacentPairOnce(ListKind kind) throws InterruptedException {
        SortableList list = kind.create();
        for (String value : List.of("d", "a", "e", "b", "c")) {
            list.addFirst(value);
        }

        BubblePass pass = list.newPass();
        int steps = 0;
        while (pass.step(0)) {
            steps++;
        }

        assertEquals(4, steps);
        assertEquals(List.of("b", "c", "a", "d", "e"), list.snapshot());
    }

    @ParameterizedTest
    @EnumSource(ListKind.class)
    void passOverShortListMakesNoSteps(ListKind kind) throws InterruptedException {
        SortableList list = kind.create();
        assertFalse(list.newPass().step(0));
        list.addFirst("single");
        assertFalse(list.newPass().step(0));
    }

    @ParameterizedTest
    @EnumSource(ListKind.class)
    void printWritesEveryRecord(ListKind kind) {
        SortableList list = kind.create();
        list.addFirst("b");
        list.addFirst("a");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int count = list.print(new PrintStream(buffer, true, StandardCharsets.UTF_8));

        assertEquals(2, count);
        assertEquals(List.of("a", "b"), buffer.toString(StandardCharsets.UTF_8).lines().toList());
    }

    @ParameterizedTest
    @EnumSource(ListKind.class)
    void readersAlwaysSeeAllInsertedRecords(ListKind kind) throws InterruptedException {
        SortableList list = kind.create();
        SorterPool pool = new SorterPool(list, new SortSettings(4, 0, 0));
        List<String> inserted = new ArrayList<>();
        Random random = new Random(7);

        pool.start();
        try {
            for (int i = 0; i < 300; i++) {
                String value = Integer.toString(random.nextInt(1000));
                list.addFirst(value);
                inserted.add(value);
                assertEquals(sorted(inserted), sorted(list.snapshot()));
            }
        } finally {
            pool.stop();
        }
    }

    @ParameterizedTest
    @EnumSource(ListKind.class)
    void sortersEventuallySortTheList(ListKind kind) throws InterruptedException {
        SortableList list = kind.create();
        List<String> values = new ArrayList<>();
        Random random = new Random(11);
        for (int i = 0; i < 100; i++) {
            String value = Integer.toString(random.nextInt(100_000));
            values.add(value);
            list.addFirst(value);
        }

        SorterPool pool = new SorterPool(list, new SortSettings(3, 0, 0));
        pool.start();
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            while (!list.snapshot().equals(sorted(values)) && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
        } finally {
            pool.stop();
        }

        assertEquals(sorted(values), list.snapshot());
    }

    private static List<String> sorted(List<String> values) {
        List<String> copy = new ArrayList<>(values);
        copy.sort(null);
        return copy;
    }
}
