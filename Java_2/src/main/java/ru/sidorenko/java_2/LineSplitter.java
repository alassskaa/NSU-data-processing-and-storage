package ru.sidorenko.java_2;

import java.util.ArrayList;
import java.util.List;

public final class LineSplitter {

    private LineSplitter() {
    }

    public static List<String> split(String line, int maxLength) {
        if (maxLength < 1) {
            throw new IllegalArgumentException(
                    "Максимальная длина записи должна быть положительной"
            );
        }
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < line.length()) {
            int end = line.codePointCount(start, line.length()) > maxLength
                    ? line.offsetByCodePoints(start, maxLength)
                    : line.length();
            parts.add(line.substring(start, end));
            start = end;
        }
        return parts;
    }
}
