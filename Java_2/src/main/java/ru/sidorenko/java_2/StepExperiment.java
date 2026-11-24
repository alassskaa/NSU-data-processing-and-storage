package ru.sidorenko.java_2;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class StepExperiment {

    private static final long SEED = 24215;
    private static final int RECORD_LENGTH = 8;

    private StepExperiment() {
    }

    public static void main(String[] args) throws InterruptedException {
        PrintStream out = new PrintStream(
                new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8
        );

        List<SortSettings> runs = new ArrayList<>();
        long betweenMillis;
        long insideMillis;
        int size;
        long seconds;
        try {
            Arguments arguments = new Arguments(args, 5);
            int[] threadCounts = parseThreadCounts(arguments.text(0, "1,2,4,8"));
            betweenMillis = arguments.number(1, 100, "betweenMs");
            insideMillis = arguments.number(2, 100, "insideMs");
            size = arguments.intNumber(3, 50, "size");
            seconds = arguments.number(4, 10, "seconds");
            if (size < 0 || seconds <= 0) {
                throw new IllegalArgumentException(
                        "Размер списка не может быть отрицательным, а длительность должна быть положительной"
                );
            }
            for (int threads : threadCounts) {
                runs.add(new SortSettings(threads, betweenMillis, insideMillis));
            }
        } catch (IllegalArgumentException e) {
            out.println(e.getMessage());
            printUsage(out);
            return;
        }

        out.printf(
                "Задержка между шагами: %d мс, внутри шага: %d мс, записей: %d, прогон: %d с%n",
                betweenMillis,
                insideMillis,
                size,
                seconds
        );
        out.println("Теория для собственного списка: N * T / (между + внутри)");
        out.println("Теория для synchronizedList:    min(N * T / (между + внутри), T / внутри)");
        out.println();
        out.printf(
                "%-6s %-7s %10s %10s %12s  %-10s %s%n",
                "нитей", "список", "теория", "факт", "факт/теория", "записи", "шагов по нитям"
        );

        List<String> records = randomRecords(size);
        for (SortSettings settings : runs) {
            for (ListKind kind : ListKind.values()) {
                SortableList list = kind.create();
                records.forEach(list::addFirst);

                SorterPool pool = new SorterPool(list, settings);
                pool.start();
                Thread.sleep(seconds * 1000);
                pool.stop();

                double expected = kind.expectedSteps(settings, pool.elapsedMillis());
                long measured = pool.totalSteps();
                out.printf(
                        Locale.ROOT,
                        "%-6d %-7s %10.1f %10d %12.3f  %-10s %s%n",
                        settings.threads(),
                        kind.key(),
                        expected,
                        measured,
                        measured / expected,
                        sameRecords(records, list.snapshot()) ? "сохранены" : "ИСКАЖЕНЫ",
                        Arrays.toString(pool.stepsPerThread())
                );
            }
        }
    }

    private static int[] parseThreadCounts(String text) {
        try {
            return Arrays.stream(text.split(","))
                    .map(String::trim)
                    .mapToInt(Integer::parseInt)
                    .toArray();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Список количеств нитей должен иметь вид 1,2,4,8: " + text
            );
        }
    }

    private static List<String> randomRecords(int size) {
        Random random = new Random(SEED);
        List<String> records = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            StringBuilder record = new StringBuilder();
            for (int j = 0; j < RECORD_LENGTH; j++) {
                record.append((char) ('a' + random.nextInt(26)));
            }
            records.add(record.toString());
        }
        return records;
    }

    private static boolean sameRecords(List<String> expected, List<String> actual) {
        List<String> left = new ArrayList<>(expected);
        List<String> right = new ArrayList<>(actual);
        left.sort(null);
        right.sort(null);
        return left.equals(right);
    }

    private static void printUsage(PrintStream out) {
        out.println("Использование:");
        out.println("StepExperiment [threads,...] [betweenMs] [insideMs] [size] [seconds]");
        out.println("По умолчанию: 1,2,4,8 100 100 50 10");
    }
}
