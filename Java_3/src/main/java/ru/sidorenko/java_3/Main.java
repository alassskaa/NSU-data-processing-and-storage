package ru.sidorenko.java_3;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public final class Main {

    private static final int DEFAULT_PORT = 8080;

    private Main() {
    }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(
                new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8
        );

        int port;
        try {
            port = parsePort(args);
        } catch (IllegalArgumentException e) {
            out.println(e.getMessage());
            printUsage(out);
            return;
        }

        URI baseUri = URI.create("http://localhost:" + port);

        long start = System.nanoTime();
        List<String> messages;
        try (Spider spider = new Spider(baseUri)) {
            messages = spider.crawl();
        }
        double seconds = (System.nanoTime() - start) / 1e9;

        messages.forEach(out::println);
        out.printf(
                Locale.ROOT,
                "----- сообщений: %d, время обхода: %.1f с -----%n",
                messages.size(),
                seconds
        );
    }

    private static int parsePort(String[] args) {
        if (args.length > 1) {
            throw new IllegalArgumentException("Слишком много аргументов: " + args.length);
        }
        if (args.length == 0) {
            return DEFAULT_PORT;
        }
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Порт должен быть целым числом: " + args[0]);
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Недопустимый порт: " + port);
        }
        return port;
    }

    private static void printUsage(PrintStream out) {
        out.println("Использование:");
        out.println("Main [port]");
        out.println("По умолчанию: " + DEFAULT_PORT);
    }
}
