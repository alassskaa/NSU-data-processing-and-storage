package ru.sidorenko.java_2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LineSplitterTest {

    @Test
    void keepsShortLineWhole() {
        assertEquals(List.of("hello"), LineSplitter.split("hello", 80));
    }

    @Test
    void keepsLineOfExactLengthWhole() {
        String line = "x".repeat(80);
        assertEquals(List.of(line), LineSplitter.split(line, 80));
    }

    @Test
    void cutsLongLineIntoPieces() {
        String line = "a".repeat(80) + "b".repeat(80) + "c".repeat(40);
        assertEquals(
                List.of("a".repeat(80), "b".repeat(80), "c".repeat(40)),
                LineSplitter.split(line, 80)
        );
    }

    @Test
    void doesNotBreakSurrogatePairs() {
        String smile = "😀";
        List<String> parts = LineSplitter.split(smile.repeat(3), 2);
        assertEquals(List.of(smile + smile, smile), parts);
    }
}
