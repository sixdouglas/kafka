package org.douglas.kafka.avro.streams;

import java.util.Optional;
import java.util.OptionalInt;

public class GetDataResult {

    private static GetDataResult NOT_FOUND = new GetDataResult(null, null, null);

    private final Object result;
    private final String host;
    private final Integer port;

    private GetDataResult(Object result, String host, Integer port) {
        this.result = result;
        this.host = host;
        this.port = port;
    }

    public static GetDataResult found(Object data) {
        return new GetDataResult(data, null, null);
    }

    public static GetDataResult foundRemotely(String host, int port) {
        return new GetDataResult(null, host, port);
    }

    public static GetDataResult notFound() {
        return NOT_FOUND;
    }

    public Optional<Object> getResult() {
        return Optional.ofNullable(result);
    }

    public Optional<String> getHost() {
        return Optional.ofNullable(host);
    }

    public OptionalInt getPort() {
        return port != null ? OptionalInt.of(port) : OptionalInt.empty();
    }
}
