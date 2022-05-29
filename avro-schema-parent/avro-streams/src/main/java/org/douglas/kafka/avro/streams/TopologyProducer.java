package org.douglas.kafka.avro.streams;

import io.confluent.kafka.streams.serdes.avro.ReflectionAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;
import org.douglas.kafka.avro.schema.Customer;
import org.douglas.kafka.avro.schema.EnrichedOrder;
import org.douglas.kafka.avro.schema.Order;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

@ApplicationScoped
public class TopologyProducer {
    private static final Logger LOG = Logger.getLogger(TopologyProducer.class);

    static final String ORDERS_STORE = "avro.orders-store";
    static final String CUSTOMER_TOPIC = "avro.customers";
    static final String ORDER_TOPIC = "avro.orders";
    static final String ORDER_AGGREGATED_TOPIC = "avro.orders-aggregated";

    @ConfigProperty(name = "kafka-streams.schema.registry.url")
    String schemaRegistryUrl;

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        Map<String, String> serdeConfig = new HashMap<>();
        serdeConfig.put(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);

        ReflectionAvroSerde<Order> orderSerde = new ReflectionAvroSerde<>(Order.class);
        orderSerde.configure(serdeConfig, false);
        ReflectionAvroSerde<Customer> customerSerde = new ReflectionAvroSerde<>(Customer.class);
        customerSerde.configure(serdeConfig, false);
        ReflectionAvroSerde<EnrichedOrder> aggregationSerde = new ReflectionAvroSerde<>(EnrichedOrder.class);
        aggregationSerde.configure(serdeConfig, false);

        KeyValueBytesStoreSupplier ordersStore = Stores.persistentKeyValueStore(ORDERS_STORE);

        GlobalKTable<String, Customer> customerGlobalKTable = builder.globalTable(
                CUSTOMER_TOPIC,
                Consumed.with(Serdes.String(), customerSerde));

        builder.stream(ORDER_TOPIC, Consumed.with(Serdes.String(), orderSerde))
                .join(
                        customerGlobalKTable,
                        this::getKey,
                        this::buildComposedOrder)
                .groupByKey()
                .aggregate(
                        this::buildNewEnrichedOrder,
                        this::fillEnrichedOrder,
                        Materialized.<String, EnrichedOrder> as(ordersStore)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(aggregationSerde))
                .toStream()
                .to(
                        ORDER_AGGREGATED_TOPIC,
                        Produced.with(Serdes.String(), aggregationSerde));

        return builder.build();
    }

    String getKey(String orderId, Order order) {
            return order.getCustomerId();
    }

    ComposedOrder buildComposedOrder(Order order, Customer customer) {
        return new ComposedOrder(customer, order);
    }

    private EnrichedOrder buildNewEnrichedOrder() {
        return new EnrichedOrder(null, null, null, Instant.ofEpochSecond(0), null, null);
    }

    private EnrichedOrder fillEnrichedOrder(String key, ComposedOrder value, EnrichedOrder aggregation) {
        return value.toAggregated();
    }
}
