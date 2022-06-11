package org.douglas.kafka.ksqldb.rest.restclient;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class OrderRestEndpointTest {

    @Test
    void testMapping() throws IOException{
        OrderRestEndpoint orderRestEndpoint = new OrderRestEndpoint();
        String input = getJsonFileAsString("org/douglas/kafka/ksqldb/rest/restclient/ksqldb-response.json");
        final Multi<String> objects = orderRestEndpoint.mapResult(input);

        String response = getJsonFileAsString("org/douglas/kafka/ksqldb/rest/restclient/endpoint-response.json");
        final JsonArray responseArray = Json.parse(response).asArray();
        List<String> expectedItems = responseArray.values().stream().map(JsonValue::asObject).map(JsonValue::toString).toList();

        objects.subscribe()
                .withSubscriber(AssertSubscriber.create(5))
                .assertSubscribed()
                .assertCompleted()
                .assertItems(expectedItems.toArray(new String[0]));
    }

    private String getJsonFileAsString(String jsonFileName) throws IOException {
        String text;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(jsonFileName)) {
            text = new BufferedReader(
                    new InputStreamReader(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
        }
        return text;
    }

}