package com.cyecize.gatewayserver.config;

import com.cyecize.gatewayserver.api.options.Options;
import com.cyecize.gatewayserver.api.options.RouteOption;
import com.cyecize.ioc.annotations.Bean;
import com.cyecize.ioc.annotations.PostConstruct;
import com.cyecize.ioc.annotations.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.StandardConstants;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class SSLContextBeanConfig {
    private final Options options;

    /**
     * Mapping of host to the desired SSL certificate alias.
     * <p>
     * Eg.
     * abc.com -> abc
     * www.abc.com -> abc
     * <p>
     * xyz.com -> xyzAlias
     * abc.xyz.com -> xyzSubdomainAlias
     * <p>
     * yyy.com -> yyy
     * 134.134.122.19 -> defaultCertificate
     */
    private final Map<String, String> hostAliasMap = new HashMap<>();

    @PostConstruct
    void init() {
        if (!this.options.isStartHttps()) {
            return;
        }

        final String defaultAlias = this.options.getDefaultCertificateAlias();

        for (RouteOption option : this.options.getRouteOptions()) {
            if (!option.getScheme().isSSLCompatible()) {
                continue;
            }
            if (option.getCertificateAlias() == null && defaultAlias == null) {
                throw new IllegalArgumentException(String.format(
                        "Route option [%s] is missing https certificate alias. "
                                + "Add alias, add default alias or disable https server",
                        option.getHost()
                ));
            }

            final String certAlias = Objects.requireNonNullElse(option.getCertificateAlias(), defaultAlias);
            log.info("Binding host {} to certificate with alias {}.", option.getHost(), certAlias);

            this.hostAliasMap.put(option.getHost(), certAlias);
            for (String subdomain : option.getSubdomains()) {
                this.hostAliasMap.put(subdomain + "." + option.getHost(), certAlias);
            }
        }
    }

    @Bean
    public SSLContext buildSSLContext() throws Exception {
        if (!this.options.isStartHttps()) {
            return null;
        }

        if (this.options.getDebuggingOptions() != null && this.options.getDebuggingOptions().isLogSSL()) {
            System.setProperty("javax.net.debug", "ssl");
        }

        log.info("HTTPS is enabled, building SSL Context");

        final String filePath = this.options.getKeystoreFileDir() + this.options.getKeystoreFileName();

        final char[] pass = Objects.requireNonNullElse(this.options.getKeystorePassword(), "").toCharArray();
        final KeyStore ks = KeyStore.getInstance("JKS");

        try (final FileInputStream fis = new FileInputStream(filePath)) {
            ks.load(fis, pass);
        } catch (IOException ex) {
            throw new IOException(String.format("Cannot load keystore file '%s'", filePath), ex);
        }

        javax.net.ssl.KeyManager[] kms = new javax.net.ssl.KeyManager[]{
                new MyKeyManager(ks, pass, this.hostAliasMap, this.options.getDefaultCertificateAlias())
        };

        final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        final javax.net.ssl.TrustManager[] tms = tmf.getTrustManagers();
        final SSLContext sslContext = SSLContext.getInstance("TLS");

        sslContext.init(kms, tms, null);

        return sslContext;
    }

    private static class MyKeyManager extends X509ExtendedKeyManager implements X509KeyManager {
        private final KeyStore keyStore;
        private final char[] password;
        private final Map<String, String> hostAliasMap;
        private final String defaultAlias;

        public MyKeyManager(KeyStore keystore,
                            char[] password,
                            Map<String, String> hostAliasMap,
                            String defaultAlias) {
            this.keyStore = keystore;
            this.password = password;
            this.hostAliasMap = hostAliasMap;
            this.defaultAlias = defaultAlias;
        }

        public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
            //Get hostname from SSL handshake
            ExtendedSSLSession session = (ExtendedSSLSession) engine.getHandshakeSession();
            return this.getCertAlias(session);
        }

        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            SSLSocket sock = (SSLSocket) socket;
            ExtendedSSLSession session = (ExtendedSSLSession) sock.getHandshakeSession();

            return this.getCertAlias(session);
        }

        public PrivateKey getPrivateKey(String alias) {
            try {
                return (PrivateKey) keyStore.getKey(alias, password);
            } catch (Exception e) {
                return null;
            }
        }

        public X509Certificate[] getCertificateChain(String alias) {
            try {
                java.security.cert.Certificate[] certs = keyStore.getCertificateChain(alias);
                if (certs == null || certs.length == 0) return null;
                X509Certificate[] x509 = new X509Certificate[certs.length];
                for (int i = 0; i < certs.length; i++) {
                    x509[i] = (X509Certificate) certs[i];
                }
                return x509;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public String[] getServerAliases(String keyType, Principal[] issuers) {
            throw new UnsupportedOperationException("Method getServerAliases() not yet implemented.");
        }

        public String[] getClientAliases(String keyType, Principal[] issuers) {
            throw new UnsupportedOperationException("Method getClientAliases() not yet implemented.");
        }

        public String chooseClientAlias(String keyTypes[], Principal[] issuers, Socket socket) {
            throw new UnsupportedOperationException("Method chooseClientAlias() not yet implemented.");
        }

        public String chooseEngineClientAlias(String[] strings, Principal[] prncpls, SSLEngine ssle) {
            throw new UnsupportedOperationException("Method chooseEngineClientAlias() not yet implemented.");
        }

        private String getCertAlias(ExtendedSSLSession session) {
            String hostName = null;
            for (SNIServerName name : session.getRequestedServerNames()) {
                if (name.getType() == StandardConstants.SNI_HOST_NAME) {
                    hostName = ((SNIHostName) name).getAsciiName();
                    break;
                }
            }

            if (hostName == null) {
                throw new IllegalStateException("Session does not have SNI_HOST_NAME. This should not happen!");
            }

            if (!this.hostAliasMap.containsKey(hostName)) {
                if (this.defaultAlias == null) {
                    throw new IllegalStateException(String.format("No SSL certificate for host %s.", hostName));
                }

                log.warn("Resorting to default SSL certificate alias for unknown host {},", hostName);
                return this.defaultAlias;
            }

            return this.hostAliasMap.get(hostName);
        }
    }
}
