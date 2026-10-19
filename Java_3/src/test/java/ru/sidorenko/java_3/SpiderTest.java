package ru.sidorenko.java_3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SpiderTest {

    private final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();
    private HttpServer server;
    private ExecutorService serverExecutor;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
            serverExecutor.close();
        }
    }

    @Test
    void collectsAllMessagesSorted() throws IOException {
        startServer(Map.of(
                "/", page("root", "a", "b"),
                "/a", page("delta", "c"),
                "/b", page("alpha"),
                "/c", page("charlie")
        ), 0);

        assertEquals(List.of("alpha", "charlie", "delta", "root"), crawl());
    }

    @Test
    void acceptsSuccessorsWithAndWithoutSlash() throws IOException {
        startServer(Map.of(
                "/", page("root", "a", "/b"),
                "/a", page("a"),
                "/b", page("b")
        ), 0);

        assertEquals(List.of("a", "b", "root"), crawl());
    }

    @Test
    void requestsEveryPathOnce() throws IOException {
        startServer(Map.of(
                "/", page("root", "a", "b", "c"),
                "/a", page("a", "d"),
                "/b", page("b", "d", "a"),
                "/c", page("c", "d"),
                "/d", page("d", "/")
        ), 0);

        assertEquals(List.of("a", "b", "c", "d", "root"), crawl());
        hits.forEach((path, count) -> assertEquals(1, count.get(), path));
    }

    @Test
    void skipsMissingPaths() throws IOException {
        startServer(Map.of(
                "/", page("root", "a", "missing"),
                "/a", page("a")
        ), 0);

        assertEquals(List.of("a", "root"), crawl());
    }

    @Test
    void requestsRunInParallel() throws IOException {
        Map<String, String> pages = new HashMap<>();
        String[] children = new String[10];
        for (int i = 0; i < children.length; i++) {
            children[i] = String.valueOf(i);
            pages.put("/" + i, page("child " + i));
        }
        pages.put("/", page("root", children));
        startServer(pages, 1000);

        long start = System.nanoTime();
        List<String> messages = crawl();
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertEquals(11, messages.size());
        assertTrue(millis < 3000, "обход занял " + millis + " мс");
    }

    private List<String> crawl() {
        URI baseUri = URI.create("http://localhost:" + server.getAddress().getPort());
        try (Spider spider = new Spider(baseUri)) {
            return spider.crawl();
        }
    }

    private void startServer(Map<String, String> pages, long delayMillis) throws IOException {
        serverExecutor = Executors.newVirtualThreadPerTaskExecutor();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setExecutor(serverExecutor);
        server.createContext("/", exchange -> respond(exchange, pages, delayMillis));
        server.start();
    }

    private void respond(HttpExchange exchange, Map<String, String> pages, long delayMillis)
            throws IOException {
        String path = exchange.getRequestURI().getPath();
        hits.computeIfAbsent(path, p -> new AtomicInteger()).incrementAndGet();

        String body = pages.get(path);
        if (body == null) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }
        if (delayMillis > 0 && !path.equals("/")) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String page(String message, String... successors) {
        String list = Arrays.stream(successors)
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(","));
        return "{\"message\":\"" + message + "\",\"successors\":[" + list + "]}";
    }
}
