package org.douglas.kafka.plain;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.providers.connectors.InMemoryConnector;

import java.util.HashMap;
import java.util.Map;

import static org.douglas.kafka.plain.CustomerGenerator.CUSTOMER_TOPIC;
import static org.douglas.kafka.plain.OrderGenerator.ORDER_TOPIC;

public class KafkaTestResourceLifecycleManager implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        Map<String, String> props = InMemoryConnector.switchOutgoingChannelsToInMemory(CUSTOMER_TOPIC);
        props.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory(ORDER_TOPIC));
        return new HashMap<>(props);
    }

    @Override
    public void stop() {
        InMemoryConnector.clear();
    }
}
