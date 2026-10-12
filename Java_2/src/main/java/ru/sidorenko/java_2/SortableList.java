package ru.sidorenko.java_2;

import java.io.PrintStream;
import java.util.List;

public interface SortableList {

    void addFirst(String value);

    int print(PrintStream out);

    List<String> snapshot();

    BubblePass newPass();
}
