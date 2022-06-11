package org.douglas.kafka.ksqldb.rest.javaclient;

import io.confluent.ksql.api.client.Client;
import io.confluent.ksql.api.client.ClientOptions;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

public class KsqlDbClientConfig {

    @ConfigProperty(name = "ksqldb.server.host.name")
    String hostName;

    @ConfigProperty(name = "ksqldb.server.host.port")
    int hostPort;

    @Produces
    @ApplicationScoped
    Client buildKsqlDbClient(){
        ClientOptions options = ClientOptions.create()
                .setHost(hostName)
                .setPort(hostPort);
        return Client.create(options);
    }
}
