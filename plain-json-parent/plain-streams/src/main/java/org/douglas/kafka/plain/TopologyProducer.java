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

        ObjectMapperSerde<Customer> weatherStationSerde = new ObjectMapperSerde<>(Customer.class);
        ObjectMapperSerde<EnrichedOrder> aggregationSerde = new ObjectMapperSerde<>(EnrichedOrder.class);

        KeyValueBytesStoreSupplier storeSupplier = Stores.persistentKeyValueStore(CUSTOMER_STORE);

        GlobalKTable<String, Customer> superModelGlobalKTable = builder.globalTable(
                CUSTOMER_TOPIC,
                Consumed.with(Serdes.String(), weatherStationSerde));

        builder.stream(ORDER_TOPIC, Consumed.with(Serdes.String(), Serdes.String()))
                .join(
                        superModelGlobalKTable,
                        this::getKey,
                        this::buildAggregatedSuperModel)
                .groupByKey()
                .aggregate(
                        this::buildNewAggregated,
                        this::aggregateWithFmsData,
                        Materialized.<String, EnrichedOrder> as(storeSupplier)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(aggregationSerde))
                .toStream()
                .to(
                        ORDER_AGGREGATED_TOPIC,
                        Produced.with(Serdes.String(), aggregationSerde));

        return builder.build();
    }

    String getKey(String codeSm, Object ignored) {
        return codeSm;
    }

    ComposedOrder buildAggregatedSuperModel(String orderAsString, Customer customer) {
        Order fmsSuperModel = null;
        try {
            fmsSuperModel = OBJECT_MAPPER.readValue(orderAsString, Order.class);
        } catch (JsonProcessingException e) {
            LOG.error("Error deserializing FMS SM", e);
        }

        return new ComposedOrder(customer, fmsSuperModel);
    }

    private EnrichedOrder buildNewAggregated() {
        return new EnrichedOrder(null, null, null, null, null);
    }

    private EnrichedOrder aggregateWithFmsData(String key, ComposedOrder value, EnrichedOrder aggregation) {
        return value.toAggregated();
    }
}
