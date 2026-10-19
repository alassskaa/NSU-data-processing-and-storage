package ru.sidorenko.java_2;

final class Arguments {

    private final String[] values;

    Arguments(String[] values, int maxCount) {
        if (values.length > maxCount) {
            throw new IllegalArgumentException(
                    "Слишком много аргументов: " + values.length
            );
        }
        this.values = values;
    }

    String text(int index, String fallback) {
        return index < values.length ? values[index] : fallback;
    }

    long number(int index, long fallback, String name) {
        if (index >= values.length) {
            return fallback;
        }
        try {
            return Long.parseLong(values[index]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Параметр \"" + name + "\" должен быть целым числом: " + values[index]
            );
        }
    }

    int intNumber(int index, int fallback, String name) {
        long value = number(index, fallback, name);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Параметр \"" + name + "\" слишком велик: " + value
            );
        }
        return (int) value;
    }
}
