package com.github.yinzhouzhou.sdn.grpc.utils.tls;

import com.google.common.annotations.Beta;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Base64;
import javax.net.ssl.KeyManagerFactory;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public final class JksFileParse {

    private final Logger logger = LoggerFactory.getLogger(JksFileParse.class);

    private String keystoreFilePath;
    private String password;

    private JksFileParse(String keystoreFilePath, String password) {
        this.keystoreFilePath = keystoreFilePath;
        this.password = password;
    }

    public static JksFileParse createJksParse(String keystoreFilePath, String password) {
        return new JksFileParse(keystoreFilePath, password);
    }

    public PrivateKey getPrivateKey(String alias) throws KeyStoreException,
        IOException,
        CertificateException,
        NoSuchAlgorithmException,
        UnrecoverableKeyException {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream inputStream = new FileInputStream(keystoreFilePath)) {
            ks.load(inputStream, password.toCharArray());
            return (PrivateKey) ks.getKey(alias, password.toCharArray());
        }
    }

    // keytool -v -importkeystore -srckeystore <keystore1.jks> -srcstoretype jks -srcstorepass <123456>
    // -destkeystore keystore1.pfx -deststoretype pkcs12 -deststorepass <123456> -destkeypass <123456>

    // openssl pkcs12 -in keystore1.pfx -nocerts -nodes -out server.key
    public void readAndExportPrivateKey(String aliasName,
                                        String exportedPublicKeyFile)
        throws KeyStoreException, IOException,
        CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        PrivateKey privateKey = getPrivateKey(aliasName);
        String encoded =  Base64.getEncoder().encodeToString(privateKey.getEncoded());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-----BEGIN PRIVATE KEY-----\n");
        stringBuilder.append(encoded);
        stringBuilder.append("\n");
        stringBuilder.append("-----END PRIVATE KEY-----");
        FileUtils.write(new File(exportedPublicKeyFile), stringBuilder.toString(), StandardCharsets.UTF_8);
        logger.info("export privateKey {} success.", exportedPublicKeyFile);
    }

    // 公屏打印 keytool -list -rfc -keystore <keystore1.jks> -storepass <123456>
    // 导出cer证书 keytool -export -keystore <keystore1.jks> -storepass <123456> -file <abcd.cer> -alias <1>
    public void readAndExportCertificate(String alias,
                                         String exportedPublicKeyFile)
        throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore keystore = KeyStore.getInstance("JKS");
        try (FileInputStream inputStream = new FileInputStream(keystoreFilePath)) {
            keystore.load(inputStream, password.toCharArray());
            Certificate cert = keystore.getCertificate(alias);
            String encoded = Base64.getEncoder().encodeToString(cert.getEncoded());
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("-----BEGIN CERTIFICATE-----\n");
            stringBuilder.append(encoded);
            stringBuilder.append("\n");
            stringBuilder.append("-----END CERTIFICATE-----");
            FileUtils.write(new File(exportedPublicKeyFile), stringBuilder.toString(), StandardCharsets.UTF_8);
        }
        logger.info("export Cer {} success.", exportedPublicKeyFile);
    }


    public KeyManagerFactory getPrivateKeyManagerFactory(String alias)
        throws KeyStoreException,
        IOException,
        CertificateException,
        NoSuchAlgorithmException,
        UnrecoverableKeyException {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream inputStream = new FileInputStream(keystoreFilePath)) {
            //1.使用秘钥库给对方进行身份验证
            ks.load(inputStream, password.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password.toCharArray());
            return kmf;
        }
    }
}
