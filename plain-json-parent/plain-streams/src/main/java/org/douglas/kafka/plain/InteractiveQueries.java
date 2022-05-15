package org.douglas.kafka.plain;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.douglas.kafka.plainjson.EnrichedOrder;
import org.jboss.logging.Logger;
import org.wildfly.common.net.HostName;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class InteractiveQueries {

    private static final Logger LOG = Logger.getLogger(InteractiveQueries.class);

    @Inject
    KafkaStreams streams;

    public List<PipelineMetadata> getMetaData() {
        return streams.streamsMetadataForStore(TopologyProducer.CUSTOMER_STORE)
                .stream()
                .map(m -> new PipelineMetadata(
                        m.hostInfo().host() + ":" + m.hostInfo().port(),
                        m.topicPartitions()
                                .stream()
                                .map(TopicPartition::toString)
                                .collect(Collectors.toSet())))
                .collect(Collectors.toList());
    }

    public GetDataResult getOrderData(String id) {
        KeyQueryMetadata metadata = streams.queryMetadataForKey(
                TopologyProducer.CUSTOMER_STORE,
                id,
                Serdes.String().serializer());

        if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
            LOG.warnv("Found no metadata for key {0}", id);
            return GetDataResult.notFound();
        } else if (metadata.activeHost().host().equals(HostName.getQualifiedHostName())) {
            LOG.infov("Found data for key {0} locally", id);
            EnrichedOrder result = getWeatherStationStore().get(id);

            if (result != null) {
                return GetDataResult.found(result);
            } else {
                return GetDataResult.notFound();
            }
        } else {
            LOG.infov("Found data for key {0} on remote host {1}:{2}", id, metadata.activeHost().host(), metadata.activeHost().port());
            return GetDataResult.foundRemotely(metadata.activeHost().host(), metadata.activeHost().port());
        }
    }

    private ReadOnlyKeyValueStore<String, EnrichedOrder> getWeatherStationStore() {
        while (true) {
            try {
                return streams.store(StoreQueryParameters.fromNameAndType(TopologyProducer.CUSTOMER_STORE, QueryableStoreTypes.keyValueStore()));
            } catch (InvalidStateStoreException e) {
                // ignore, store not ready yet
            }
        }
    }
}
