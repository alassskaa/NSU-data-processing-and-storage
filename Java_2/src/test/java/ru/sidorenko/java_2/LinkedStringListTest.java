package ru.sidorenko.java_2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LinkedStringListTest {

    @Test
    void forEachWalksFromHead() {
        LinkedStringList list = new LinkedStringList();
        list.addFirst("first");
        list.addFirst("second");
        list.addFirst("third");

        List<String> seen = new ArrayList<>();
        for (String value : list) {
            seen.add(value);
        }

        assertEquals(List.of("third", "second", "first"), seen);
    }

    @Test
    void finishedIterationReleasesAllLocks() throws InterruptedException {
        LinkedStringList list = new LinkedStringList();
        list.snapshot();
        list.addFirst("b");
        list.addFirst("a");
        list.snapshot();

        Thread other = new Thread(() -> {
            list.addFirst("c");
            list.snapshot();
        });
        other.start();
        other.join(2000);

        assertFalse(other.isAlive());
        assertEquals(List.of("c", "a", "b"), list.snapshot());
    }
}
