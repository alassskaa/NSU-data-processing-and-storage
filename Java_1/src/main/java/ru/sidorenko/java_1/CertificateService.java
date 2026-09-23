package ru.sidorenko.java_1;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

public class CertificateService {

    private static final int RSA_KEY_SIZE = 8192;

    private final PrivateKey signingKey;
    private final String issuerName;

    public CertificateService(Path signingKeyFile, String issuerName) throws Exception {

        Security.addProvider(new BouncyCastleProvider());

        this.signingKey = loadPrivateKey(signingKeyFile);
        this.issuerName = issuerName;
    }

    public KeyMaterial generate(String clientName) throws Exception {

        System.out.println(
                "Генерация ключей для: " + clientName +
                        " (" + Thread.currentThread().getName() + ")"
        );

        KeyPairGenerator keyPairGenerator =
                KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(RSA_KEY_SIZE);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        SubjectPublicKeyInfo publicKeyInfo =
                SubjectPublicKeyInfo.getInstance(
                        keyPair.getPublic().getEncoded()
                );

        X500Name issuer = new X500Name(
                "CN=" + issuerName
        );

        X500Name subject = new X500Name(
                "CN=" + clientName
        );

        Date notBefore = new Date();

        Date notAfter = new Date(
                notBefore.getTime() +
                        365L * 24 * 60 * 60 * 1000
        );

        BigInteger serialNumber =
                new BigInteger(160, ThreadLocalRandom.current());

        X509v3CertificateBuilder certificateBuilder =
                new X509v3CertificateBuilder(
                        issuer,
                        serialNumber,
                        notBefore,
                        notAfter,
                        subject,
                        publicKeyInfo
                );

        ContentSigner signer =
                new JcaContentSignerBuilder("SHA256withRSA")
                        .setProvider("BC")
                        .build(signingKey);

        X509CertificateHolder certificateHolder =
                certificateBuilder.build(signer);

        X509Certificate certificate =
                new JcaX509CertificateConverter()
                        .setProvider("BC")
                        .getCertificate(certificateHolder);

        System.out.println(
                "Готово: " + clientName
        );

        return new KeyMaterial(
                keyPair.getPrivate().getEncoded(),
                keyPair.getPublic().getEncoded(),
                certificate.getEncoded()
        );
    }

    private PrivateKey loadPrivateKey(Path file) throws Exception {

        byte[] encodedKey = Files.readAllBytes(file);

        PKCS8EncodedKeySpec keySpec =
                new PKCS8EncodedKeySpec(encodedKey);

        KeyFactory keyFactory =
                KeyFactory.getInstance("RSA");

        return keyFactory.generatePrivate(keySpec);
    }

    public static class KeyMaterial {

        private final byte[] privateKey;
        private final byte[] publicKey;
        private final byte[] certificate;

        public KeyMaterial(
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