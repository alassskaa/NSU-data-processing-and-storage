package ru.sidorenko.java_1;

import java.nio.ByteBuffer;

public class Protocol {

    private Protocol() {
    }

    public static ByteBuffer createResponse(
            CertificateService.KeyMaterial keyMaterial
    ) {

        byte[] privateKey =
                keyMaterial.getPrivateKey();

        byte[] publicKey =
                keyMaterial.getPublicKey();

        byte[] certificate =
                keyMaterial.getCertificate();

        int totalSize =
                4 +
                        4 +
                        4 +
                        privateKey.length +
                        publicKey.length +
                        certificate.length;

        ByteBuffer buffer =
                ByteBuffer.allocate(totalSize);

        buffer.putInt(privateKey.length);
        buffer.putInt(publicKey.length);
        buffer.putInt(certificate.length);

        buffer.put(privateKey);
        buffer.put(publicKey);
        buffer.put(certificate);

        buffer.flip();

        return buffer;
    }

    public static Response readResponse(
            ByteBuffer buffer
    ) {


        if (buffer.remaining() < 12) {
            return null;
        }

        buffer.mark();

        int privateKeyLength =
                buffer.getInt();

        int publicKeyLength =
                buffer.getInt();

        int certificateLength =
                buffer.getInt();


        if (privateKeyLength < 0 ||
                publicKeyLength < 0 ||
                certificateLength < 0) {

            return null;
        }

        int required =
                privateKeyLength +
                        publicKeyLength +
                        certificateLength;


        if (buffer.remaining() < required) {

            buffer.reset();

            return null;
        }

        byte[] privateKey =
                new byte[privateKeyLength];

        byte[] publicKey =
                new byte[publicKeyLength];

        byte[] certificate =
                new byte[certificateLength];

        buffer.get(privateKey);
        buffer.get(publicKey);
        buffer.get(certificate);

        return new Response(
                privateKey,
                publicKey,
                certificate
        );
    }

    public static class Response {

        private final byte[] privateKey;

        private final byte[] publicKey;

        private final byte[] certificate;

        public Response(
                byte[] privateKey,
                byte[] publicKey,
                byte[] certificate
        ) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.certificate = certificate;
        }

        public byte[] getPrivateKey() {
            return privateKey;
        }

        public byte[] getPublicKey() {
            return publicKey;
        }

        public byte[] getCertificate() {
            return certificate;
        }
    }
}