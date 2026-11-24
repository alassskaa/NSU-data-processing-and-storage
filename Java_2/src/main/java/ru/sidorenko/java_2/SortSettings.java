package ru.sidorenko.java_2;

public record SortSettings(int threads, long betweenStepsMillis, long insideStepMillis) {

    public SortSettings {
        if (threads < 1) {
            throw new IllegalArgumentException(
                    "Количество сортирующих нитей должно быть положительным"
            );
        }
        if (betweenStepsMillis < 0 || insideStepMillis < 0) {
            throw new IllegalArgumentException(
                    "Задержки не могут быть отрицательными"
            );
        }
    }
}
