package ru.sidorenko.java_1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Server {

    private final Selector selector;
    private final ExecutorService generatorPool;
    private final int generatorThreads;
    private final CertificateService certificateService;


    private final LinkedBlockingQueue<KeyRequest> requestQueue =
            new LinkedBlockingQueue<>();

    private final Map<String, CompletableFuture<
            CertificateService.KeyMaterial>> generations =
            new ConcurrentHashMap<>();


    private final Map<String, List<ClientConnection>> waitingClients =
            new HashMap<>();


    private final ArrayDeque<String> completedNames =
            new ArrayDeque<>();

    private Server(
            int port,
            int generatorThreads,
            Path signingKeyFile,
            String issuerName
    ) throws Exception {

        this.generatorThreads = generatorThreads;

        selector = Selector.open();

        certificateService =
                new CertificateService(
                        signingKeyFile,
                        issuerName
                );

        generatorPool =
                Executors.newFixedThreadPool(
                        generatorThreads
                );

        ServerSocketChannel serverChannel =
                ServerSocketChannel.open();

        serverChannel.configureBlocking(false);

        serverChannel.bind(
                new InetSocketAddress(port)
        );

        serverChannel.register(
                selector,
                SelectionKey.OP_ACCEPT
        );

        System.out.println(
                "Сервер запущен"
        );

        System.out.println(
                "Порт: " + port
        );

        System.out.println(
                "Потоков генерации: " +
                        generatorThreads
        );

        startGeneratorThreads();
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 4) {

            System.out.println(
                    "Использование:"
            );

            System.out.println(
                    "Server <port> <generatorThreads> " +
                            "<signingKeyFile> <issuerName>"
            );

            System.out.println();

            System.out.println(
                    "Пример:"
            );

            System.out.println(
                    "Server 9000 4 signing.key MyServer"
            );

            return;
        }

        int port =
                Integer.parseInt(args[0]);

        int generatorThreads =
                Integer.parseInt(args[1]);

        if (generatorThreads <= 0) {

            throw new IllegalArgumentException(
                    "Количество потоков должно быть больше 0"
            );
        }

        Path signingKeyFile = Path.of(args[2]);

        String issuerName = args[3];

        Server server = new Server(
                        port,
                        generatorThreads,
                        signingKeyFile,
                        issuerName);

        server.run();
    }


    private void startGeneratorThreads() {

        for (int i = 0; i < generatorThreads; i++) {

            generatorPool.submit(() -> {

                while (!Thread.currentThread().isInterrupted()) {

                    try {

                        KeyRequest request =
                                requestQueue.take();

                        String name =
                                request.getName();

                        CompletableFuture<
                                CertificateService.KeyMaterial> future =
                                generations.get(name);

                        if (future == null) {
                            continue;
                        }

                        try {

                            CertificateService.KeyMaterial result =
                                    certificateService.generate(name);

                            future.complete(result);

                        } catch (Exception e) {

                            future.completeExceptionally(e);
                        }

                        synchronized (completedNames) {

                            completedNames.add(name);
                        }

                        selector.wakeup();

                    } catch (InterruptedException e) {

                        Thread.currentThread().interrupt();

                        return;
                    }
                }
            });
        }
    }

    private void run() throws IOException {

        while (true) {

            processCompletedGenerations();

            selector.select();

            processCompletedGenerations();

            Set<SelectionKey> keys =
                    selector.selectedKeys();

            Iterator<SelectionKey> iterator =
                    keys.iterator();

            while (iterator.hasNext()) {

                SelectionKey key =
                        iterator.next();

                iterator.remove();

                if (!key.isValid()) {
                    continue;
                }

                try {

                    if (key.isAcceptable()) {
                        acceptClient(key);
                    }

                    if (key.isReadable()) {
                        readFromClient(key);
                    }

                    if (key.isWritable()) {
                        writeToClient(key);
                    }

                } catch (IOException e) {

                    closeClient(key);
                }
            }
        }
    }

    private void acceptClient(
            SelectionKey key
    ) throws IOException {

        ServerSocketChannel serverChannel =
                (ServerSocketChannel) key.channel();

        SocketChannel client =
                serverChannel.accept();

        if (client == null) {
            return;
        }

        client.configureBlocking(false);

        ClientConnection connection =
                new ClientConnection(client);

        client.register(
                selector,
                SelectionKey.OP_READ,
                connection
        );

        System.out.println(
                "Клиент подключился: " +
                        client.getRemoteAddress()
        );
    }

    private void readFromClient(
            SelectionKey key
    ) throws IOException {

        ClientConnection connection =
                (ClientConnection) key.attachment();

        SocketChannel client =
                connection.channel;

        int bytesRead =
                client.read(connection.readBuffer);


        if (bytesRead == -1) {

            closeClient(key);
            return;
        }


        if (bytesRead == 0) {
            return;
        }

        connection.readBuffer.flip();

        while (connection.readBuffer.hasRemaining()) {

            byte b =
                    connection.readBuffer.get();


            if (b == 0) {

                String name =
                        connection.getName();

                if (name.isEmpty()) {

                    closeClient(key);
                    return;
                }

                handleRequest(
                        connection,
                        name
                );

                return;
            }


            if ((b & 0x80) != 0) {

                System.out.println(
                        "Получено не-ASCII имя"
                );

                closeClient(key);
                return;
            }

            connection.nameBytes.write(b);
        }

        connection.readBuffer.clear();
    }


    private void handleRequest(
            ClientConnection connection,
            String name
    ) {


        waitingClients
                .computeIfAbsent(
                        name,
                        ignored -> new ArrayList<>()
                )
                .add(connection);


        CompletableFuture<
                CertificateService.KeyMaterial> future =
                generations.get(name);

        if (future != null) {


            System.out.println(
                    "Повторный запрос для: " + name
            );


            if (future.isDone()) {

                synchronized (completedNames) {

                    completedNames.add(name);
                }

                selector.wakeup();
            }

            return;
        }


        CompletableFuture<
                CertificateService.KeyMaterial> newFuture =
                new CompletableFuture<>();

        CompletableFuture<
                CertificateService.KeyMaterial> existing =
                generations.putIfAbsent(
                        name,
                        newFuture
                );

        if (existing == null) {

            System.out.println(
                    "Начинаем генерацию для: " + name
            );

            requestQueue.add(
                    new KeyRequest(name)
            );

        } else {

            future = existing;
        }
    }

    private void processCompletedGenerations() {

        while (true) {

            String name;

            synchronized (completedNames) {

                name =
                        completedNames.poll();
            }

            if (name == null) {
                return;
            }

            CompletableFuture<
                    CertificateService.KeyMaterial> future =
                    generations.get(name);

            if (future == null) {
                continue;
            }

            List<ClientConnection> clients =
                    waitingClients.remove(name);

            if (clients == null) {
                continue;
            }

            if (future.isCompletedExceptionally()) {

                for (ClientConnection client : clients) {

                    try {
                        client.close();
                    } catch (IOException ignored) {
                    }
                }

                continue;
            }

            CertificateService.KeyMaterial material;

            try {

                material =
                        future.join();

            } catch (Exception e) {

                for (ClientConnection client : clients) {

                    try {
                        client.close();
                    } catch (IOException ignored) {
                    }
                }

                continue;
            }

            ByteBuffer response =
                    Protocol.createResponse(
                            material
                    );

            for (ClientConnection client : clients) {

                ByteBuffer clientResponse =
                        response.duplicate();

                byte[] bytes =
                        new byte[clientResponse.remaining()];

                clientResponse.get(bytes);

                client.writeQueue.add(
                        ByteBuffer.wrap(bytes)
                );

                SelectionKey clientKey =
                        client.channel.keyFor(selector);

                if (clientKey != null &&
                        clientKey.isValid()) {

                    clientKey.interestOps(
                            SelectionKey.OP_WRITE
                    );
                }
            }

            System.out.println(
                    "Результат для " + name +
                            " отправлен ожидающим клиентам"
            );
        }
    }

    private void writeToClient(
            SelectionKey key
    ) throws IOException {

        ClientConnection connection =
                (ClientConnection) key.attachment();

        SocketChannel client =
                connection.channel;

        while (!connection.writeQueue.isEmpty()) {

            ByteBuffer buffer =
                    connection.writeQueue.peek();

            int written =
                    client.write(buffer);

            if (written == 0) {
                break;
            }

            if (!buffer.hasRemaining()) {

                connection.writeQueue.poll();
            }
        }

        if (connection.writeQueue.isEmpty()) {

            closeClient(key);
        }
    }

    private void closeClient(
            SelectionKey key
    ) {

        try {

            key.cancel();
            key.channel().close();

        } catch (IOException ignored) {
        }
    }

    private static class ClientConnection {

        private final SocketChannel channel;

        private final ByteBuffer readBuffer =
                ByteBuffer.allocate(4096);

        private final ByteArrayOutputStream nameBytes =
                new ByteArrayOutputStream();

        private final ArrayDeque<ByteBuffer> writeQueue =
                new ArrayDeque<>();

        private ClientConnection(
                SocketChannel channel
        ) {
            this.channel = channel;
        }

        private String getName() {

            return new String(
                    nameBytes.toByteArray(),
                    StandardCharsets.US_ASCII
            );
        }

        private void close() throws IOException {

            channel.close();
        }
    }
}