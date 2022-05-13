package org.douglas.kafka.plain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.Record;
import org.douglas.kafka.plainjson.Address;
import org.douglas.kafka.plainjson.Customer;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@ApplicationScoped
public class CustomerGenerator {

    static final String CUSTOMER_TOPIC = "customer";

    private static final Logger LOG = Logger.getLogger(CustomerGenerator.class);

    final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    final static List<Customer> CUSTOMERS = List.of(
            new Customer(null, UUID.randomUUID().toString(), "Albert", "Einstein", List.of(new Address(UUID.randomUUID().toString(), "12 rue nationale", "59000", "Lille", "FRANCE"))),
            new Customer(null, UUID.randomUUID().toString(), "Frank", "Einstein", List.of(new Address(UUID.randomUUID().toString(), "12 rue nationale", "62000", "Arras", "FRANCE"))),
            new Customer(null, UUID.randomUUID().toString(), "Marie", "Curie", List.of(new Address(UUID.randomUUID().toString(), "12 rue nationale", "75000", "Paris", "FRANCE"))),
            new Customer(null, UUID.randomUUID().toString(), "Pierre", "Curie", List.of(new Address(UUID.randomUUID().toString(), "12 rue nationale", "75000", "Paris", "FRANCE"))),
            new Customer(null, UUID.randomUUID().toString(), "Louis", "Pasteur", List.of(new Address(UUID.randomUUID().toString(), "12 rue nationale", "80000", "Amiens", "FRANCE"))),
            new Customer(null, UUID.randomUUID().toString(), "Elon", "Musk", List.of(new Address(UUID.randomUUID().toString(), "12 rue nationale", "14000", "Caen", "FRANCE"))),
            new Customer(null, UUID.randomUUID().toString(), "Stephen", "Hawking", List.of(new Address(UUID.randomUUID().toString(), "12 rue nationale", "29000", "Quimper", "FRANCE")))
    );

    @Outgoing(CUSTOMER_TOPIC)
    public Multi<Record<String, String>> customers() {
        return Multi.createFrom()
                .ticks().every(Duration.ofMillis(500))
                .onOverflow().drop()
                .map(aLong -> CUSTOMERS.get(Math.toIntExact(aLong)))
                .map(this::getCustomerRecord);
    }

    private Record<String, String> getCustomerRecord(Customer customer) {
        LOG.infov("Actual customer: {0}", customer);
        try {
            return Record.of(customer.customerId(), mapper.writeValueAsString(customer));
        } catch (JsonProcessingException e) {
            LOG.error("Error during serialisation of Customer: " + customer.customerId(), e);
        }
        return Record.of(customer.customerId(), "");
    }
}
