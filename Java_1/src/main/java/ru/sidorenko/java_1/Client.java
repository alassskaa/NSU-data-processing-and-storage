package ru.sidorenko.java_1;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Client {

    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.out.println(
                    "Использование:"
            );
            System.out.println(
                    "Client <name> <host> <port> " +
                            "[--delay <seconds>] [--crash]"
            );
            return;
        }

        String name = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);

        int delaySeconds = 0;
        boolean crash = false;

        for (int i = 3; i < args.length; i++) {

            if (args[i].equals("--delay")) {

                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException(
                            "После --delay нужно указать секунды"
                    );
                }

                delaySeconds =
                        Integer.parseInt(args[++i]);

            } else if (args[i].equals("--crash")) {

                crash = true;

            } else {

                throw new IllegalArgumentException(
                        "Неизвестная опция: " + args[i]
                );
            }
        }

        try (Socket socket = new Socket(host, port)) {

            System.out.println(
                    "Подключились к серверу"
            );

            OutputStream output =
                    socket.getOutputStream();

            byte[] nameBytes =
                    name.getBytes(StandardCharsets.US_ASCII);

            output.write(nameBytes);

            output.write(0);

            output.flush();

            System.out.println(
                    "Имя отправлено: " + name
            );

            if (delaySeconds > 0) {

                System.out.println(
                        "Ждём " + delaySeconds +
                                " секунд перед чтением ответа"
                );

                Thread.sleep(
                        delaySeconds * 1000L
                );
            }

            if (crash) {

                System.out.println(
                        "Имитируем падение клиента"
                );

                return;
            }

            byte[] responseBytes =
                    socket.getInputStream().readAllBytes();

            ByteBuffer responseBuffer =
                    ByteBuffer.wrap(responseBytes);

            Protocol.Response response =
                    Protocol.readResponse(responseBuffer);

            if (response == null) {
                throw new IOException(
                        "Некорректный ответ сервера"
                );
            }

            Path keyFile =
                    Path.of(name + ".key");

            Path certificateFile =
                    Path.of(name + ".crt");

            Files.write(
                    keyFile,
                    response.getPrivateKey()
            );

            Files.write(
                    certificateFile,
                    response.getCertificate()
            );

            System.out.println(
                    "Приватный ключ сохранён: " +
                            keyFile
            );

            System.out.println(
                    "Сертификат сохранён: " +
                            certificateFile
            );
        }
    }
}