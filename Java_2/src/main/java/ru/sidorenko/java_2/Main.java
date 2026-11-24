package ru.sidorenko.java_2;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class Main {

    private static final int MAX_RECORD_LENGTH = 80;

    private Main() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        PrintStream out = new PrintStream(
                new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8
        );

        ListKind kind;
        SortSettings settings;
        try {
            Arguments arguments = new Arguments(args, 4);
            kind = ListKind.parse(arguments.text(0, ListKind.LINKED.key()));
            settings = new SortSettings(
                    arguments.intNumber(1, 4, "threads"),
                    arguments.number(2, 1000, "betweenMs"),
                    arguments.number(3, 1000, "insideMs")
            );
        } catch (IllegalArgumentException e) {
            out.println(e.getMessage());
            printUsage(out);
            return;
        }

        SortableList list = kind.create();
        SorterPool pool = new SorterPool(list, settings);
        pool.start();

        out.printf(
                "Список: %s, сортирующих нитей: %d, задержка между шагами: %d мс, внутри шага: %d мс%n",
                kind.title(),
                settings.threads(),
                settings.betweenStepsMillis(),
                settings.insideStepMillis()
        );
        out.println("Вводите строки. Пустая строка выводит список, Ctrl+D завершает работу.");

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8)
        );
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                printState(out, list, pool);
            } else {
                addLine(list, line);
            }
        }

        pool.stop();
        printStatistics(out, kind, settings, pool);
    }

    private static void addLine(SortableList list, String line) {
        List<String> parts = LineSplitter.split(line, MAX_RECORD_LENGTH);
        for (int i = parts.size() - 1; i >= 0; i--) {
            list.addFirst(parts.get(i));
        }
    }

    private static void printState(PrintStream out, SortableList list, SorterPool pool) {
        out.println("----- список -----");
        int count = list.print(out);
        out.printf(
                "----- записей: %d, шагов сортировки: %d -----%n",
                count,
                pool.totalSteps()
        );
    }

    private static void printStatistics(
            PrintStream out,
            ListKind kind,
            SortSettings settings,
            SorterPool pool
    ) {
        double elapsedMillis = pool.elapsedMillis();
        out.printf(Locale.ROOT, "Время работы: %.1f с%n", elapsedMillis / 1000);
        out.println("Шагов по нитям: " + Arrays.toString(pool.stepsPerThread()));
        out.println("Всего шагов: " + pool.totalSteps());
        out.printf(
                Locale.ROOT,
                "Теоретическая оценка (%s): %.1f%n",
                kind.title(),
                kind.expectedSteps(settings, elapsedMillis)
        );
    }

    private static void printUsage(PrintStream out) {
        out.println("Использование:");
        out.println("Main [linked|array] [threads] [betweenMs] [insideMs]");
        out.println("По умолчанию: linked 4 1000 1000");
    }
}
