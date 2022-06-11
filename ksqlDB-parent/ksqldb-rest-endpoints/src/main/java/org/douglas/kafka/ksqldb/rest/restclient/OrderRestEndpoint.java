package org.douglas.kafka.ksqldb.rest.restclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
@Path("/restclient/orders")
public class OrderRestEndpoint {
    private final Pattern pattern = Pattern.compile("`[^`]*`");
    private final Map<Integer, List<String>> responseHeaderCache = new HashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    @RestClient
    IKsqlDbClient ksqlDbClient;

    @GET
    @Path("/data/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<String> getOrderData(@PathParam("id") String id) {
        return ksqlDbClient
                .getOrders("{ \"ksql\": \"SELECT * FROM orders_enriched WHERE order_id = '" + id + "';\", \"streamsProperties\": {} }")
                .flatMap(this::mapResult)
                .log();
    }

    @GET
    @Path("/data/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<String> getOrdersData() {
        return ksqlDbClient
                .getOrders("{ \"ksql\": \"SELECT * FROM orders_enriched;\", \"streamsProperties\": {} }")
                .flatMap(this::mapResult)
                .log();
    }

    Multi<String> mapResult(String returnBody) {
        final JsonNode jsonNode;
        try {
            jsonNode = mapper.readTree(returnBody);
        } catch (JsonProcessingException e) {
            return Multi.createFrom().empty();
        }
        if (!jsonNode.isArray())
            return Multi.createFrom().empty();

        final ArrayNode objects = (ArrayNode) jsonNode;
        final String schema = objects.get(0).get("header").get("schema").asText();
        final int hashCode = schema.hashCode();
        final List<String> fields = getFieldList(schema, hashCode);

        List<String> result = new ArrayList<>();
        for (int i = 1; i < objects.size(); i++) {
            ObjectNode newObject = mapper.createObjectNode();
            JsonNode currentValues = objects.get(i);
            final ArrayNode columnsValues = (ArrayNode) currentValues.get("row").get("columns");
            for (int j = 0; j < fields.size(); j++) {
                String field = fields.get(j);
                final JsonNode jsonValue = columnsValues.get(j);
                if (jsonValue.isBoolean())
                    newObject.put(field, jsonValue.asBoolean());
                if (jsonValue.canConvertToExactIntegral() && jsonValue.canConvertToInt())
                    newObject.put(field, jsonValue.asInt());
                else
                    if (jsonValue.canConvertToExactIntegral() && jsonValue.canConvertToLong())
                        newObject.put(field, jsonValue.asLong());
                    else
                        newObject.put(field, jsonValue.asDouble());
                if (jsonValue.isTextual())
                    newObject.put(field, jsonValue.textValue());
            }

            result.add(newObject.toString());
        }

        return Multi.createFrom().items(result.stream());
    }

    private List<String> getFieldList(String schema, int hashCode) {
        final List<String> fields;
        if (responseHeaderCache.containsKey(hashCode)) {
            fields = responseHeaderCache.get(hashCode);
        } else {
            final Matcher matcher = pattern.matcher(schema);
            fields = new ArrayList<>();
            while (matcher.find()) {
                fields.add(snakeToCamel(matcher.group(0).replace("`", "").toLowerCase()));
            }
            responseHeaderCache.put(hashCode, fields);
        }
        return fields;
    }

    String snakeToCamel(String str)
    {
        StringBuilder builder = new StringBuilder(str);

        // Traverse the string character by
        // character and remove underscore
        // and capitalize next letter
        for (int i = 0; i < builder.length(); i++) {

            // Check char is underscore
            if (builder.charAt(i) == '_') {

                builder.deleteCharAt(i);
                builder.replace(
                        i, i + 1,
                        String.valueOf(Character.toUpperCase(builder.charAt(i))));
            }
        }

        // Return in String type
        return builder.toString();
    }
}
