package ru.sidorenko.java_1;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class SigningKeyGenerator {

    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            System.out.println(
                    "Использование: SigningKeyGenerator <файл>"
            );
            return;
        }

        Path outputFile = Path.of(args[0]);

        System.out.println(
                "Генерация ключа подписи..."
        );

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");

        generator.initialize(4096);

        KeyPair keyPair =
                generator.generateKeyPair();

        Files.write(
                outputFile,
                keyPair.getPrivate().getEncoded()
        );

        System.out.println(
                "Ключ сохранён в: " + outputFile
        );
    }
}