package org.douglas.kafka.plain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;
import org.douglas.kafka.plainjson.Customer;
import org.douglas.kafka.plainjson.EnrichedOrder;
import org.douglas.kafka.plainjson.Order;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class TopologyProducer {
    private static final Logger LOG = Logger.getLogger(TopologyProducer.class);

    static final String CUSTOMER_STORE = "customer-store";
    static final String CUSTOMER_TOPIC = "customer";
    static final String ORDER_TOPIC = "order";
    static final String ORDER_AGGREGATED_TOPIC = "order-aggregated";

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        ObjectMapperSerde<Customer> customerSerde = new ObjectMapperSerde<>(Customer.class);
        ObjectMapperSerde<EnrichedOrder> aggregationSerde = new ObjectMapperSerde<>(EnrichedOrder.class);

        KeyValueBytesStoreSupplier storeSupplier = Stores.persistentKeyValueStore(CUSTOMER_STORE);

        GlobalKTable<String, Customer> customerGlobalKTable = builder.globalTable(
                CUSTOMER_TOPIC,
                Consumed.with(Serdes.String(), customerSerde));

        builder.stream(ORDER_TOPIC, Consumed.with(Serdes.String(), Serdes.String()))
                .join(
                        customerGlobalKTable,
                        this::getKey,
                        this::buildComposedOrder)
                .groupByKey()
                .aggregate(
                        this::buildNewEnrichedOrder,
                        this::fillEnrichedOrder,
                        Materialized.<String, EnrichedOrder> as(storeSupplier)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(aggregationSerde))
                .toStream()
                .to(
                        ORDER_AGGREGATED_TOPIC,
                        Produced.with(Serdes.String(), aggregationSerde));

        return builder.build();
    }

    String getKey(String orderId, String orderAsString) {
        Order order;
        try {
            order = OBJECT_MAPPER.readValue(orderAsString, Order.class);
            return order.customerId();
        } catch (JsonProcessingException e) {
            LOG.error("Error deserializing Order", e);
        }

        return null;
    }

    ComposedOrder buildComposedOrder(String orderAsString, Customer customer) {
        Order order = null;
        try {
            order = OBJECT_MAPPER.readValue(orderAsString, Order.class);
        } catch (JsonProcessingException e) {
            LOG.error("Error deserializing Order", e);
        }

        return new ComposedOrder(customer, order);
    }

    private EnrichedOrder buildNewEnrichedOrder() {
        return new EnrichedOrder(null, null, null, null, null);
    }

    private EnrichedOrder fillEnrichedOrder(String key, ComposedOrder value, EnrichedOrder aggregation) {
        return value.toAggregated();
    }
}
