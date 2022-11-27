package com.cyecize.gatewayserver.api.server;

import com.cyecize.gatewayserver.error.CannotParseRequestException;
import com.cyecize.gatewayserver.error.EmptyRequestException;
import com.cyecize.gatewayserver.util.HttpProtocolUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.StandardConstants;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@ToString
@Slf4j
public class Connection {
    @Getter
    private final Socket socket;

    private boolean isClosed;

    private final boolean isSsl;

    private List<String> requestLines;

    private boolean isRequestLinesRead;

    private Map<String, String> headers;

    private final List<Closeable> closeables = new ArrayList<>();

    public Connection(Socket socket, boolean isSsl) {
        this.socket = socket;
        this.isSsl = isSsl;
    }

    public void close() {
        if (this.isClosed) {
            return;
        }

        try {
            this.socket.close();
        } catch (IOException e) {
            log.warn("Error occurred while closing connection.", e);
        }

        this.closeables.forEach(Closeable::close);

        this.isClosed = true;
    }

    public Object getHostOrIp() {
        if (this.isSsl) {
            return ((ExtendedSSLSession) ((SSLSocket) this.socket).getHandshakeSession()).getRequestedServerNames()
                    .stream()
                    .filter(sniServerName -> sniServerName.getType() == StandardConstants.SNI_HOST_NAME)
                    .map(sniServerName -> ((SNIHostName) sniServerName).getAsciiName())
                    .findFirst().orElse("N/A");
        }

        return socket.getInetAddress();
    }

    public void addCloseable(Closeable closeable) {
        this.closeables.add(closeable);
    }

    public boolean readRequestLines() throws IOException {
        if (this.isRequestLinesRead) {
            return true;
        }

        try {
            this.requestLines = HttpProtocolUtils.parseMetadataLines(this.socket.getInputStream(), true);
            this.isRequestLinesRead = true;
        } catch (CannotParseRequestException ex) {
            log.warn("Error while reading HTTP request lines (ip: {}). {}", this.getHostOrIp(), ex.getMessage());
            this.close();
            return false;
        } catch (EmptyRequestException ex) {
            // Ignore this exception!
            // Web browsers often send empty socket connection when caching is enabled, mainly for favicon.ico
            // We do not want to keep this connection alive and flood our thread pool.
            this.close();
            return false;
        }

        return true;
    }

    public List<String> getRequestLines() {
        Objects.requireNonNull(this.requestLines, "Please read request lines first");
        return this.requestLines;
    }

    public String getHost() {
        this.setHeaders();
        return HttpProtocolUtils.getHost(this.socket, this.headers);
    }

    @SneakyThrows
    public int getContentLength() {
        this.setHeaders();
        return HttpProtocolUtils.getContentLength(this.socket.getInputStream(), this.headers);
    }

    private void setHeaders() {
        if (this.headers != null) {
            return;
        }
        Objects.requireNonNull(this.requestLines, "Please read request lines first");
        this.headers = HttpProtocolUtils.getHeaders(this.requestLines);
    }

    @FunctionalInterface
    public static interface Closeable {
        void close();
    }
}
