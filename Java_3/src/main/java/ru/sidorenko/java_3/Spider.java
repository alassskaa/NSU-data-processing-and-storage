package ru.sidorenko.java_3;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Spider implements AutoCloseable {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final URI baseUri;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final Queue<String> messages = new ConcurrentLinkedQueue<>();

    public Spider(URI baseUri) {
        this.baseUri = baseUri;
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public List<String> crawl() {
        visited.add("/");
        visit("/");

        List<String> result = new ArrayList<>(messages);
        Collections.sort(result);
        return result;
    }

    private void visit(String path) {
        Page page = fetch(path);
        if (page == null) {
            return;
        }
        messages.add(page.message());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String successor : page.successors()) {
                String next = toPath(successor);
                if (visited.add(next)) {
                    executor.submit(() -> visit(next));
                }
            }
        }
    }

    private Page fetch(String path) {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    System.err.printf("%s: код ответа %d%n", path, response.statusCode());
                    return null;
                }
                return mapper.readValue(response.body(), Page.class);
            } catch (IOException e) {
                System.err.printf("%s: попытка %d не удалась: %s%n", path, attempt, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private static String toPath(String successor) {
        return successor.startsWith("/") ? successor : "/" + successor;
    }

    @Override
    public void close() {
        client.close();
    }
}
