# Kafka exploration project

## Table of content

* [Description](#description)
* [Setup](#setup)
* [Sub projects explained](#sub-projects)
  * [avro-schema](#avro-schema)
  * [ksqldb](#ksqldb)
  * [plain-json](#plain-json)

## Description

Three projects so far:

* plain-json
* avro-schema
* ksqlDB

In the first one, `plain-json`, records in topics are serialized as `String`, for key and data.

In the second, `avro-schema`, records in topics are serialized and validated using Avro Schema in the Kafka Registry.

In the third one `ksqlDB`, the records come from the second project, and I'm using KSqlDB to apply operation to these data.

## Setup

### Docker Compose

There are two docker compose files. One with all but `ksqldb` server, the other one with it.

The Docker Compose contains 6 containers:

| type       | instance name   | exposed ports       |
|------------|-----------------|---------------------|
| zookeeper  | zookeper        | -                   |
| kafka      | broker          | 29092 / 9092 / 9101 |
| registry   | schema-registry | 9081                |
| akhq       | akhq            | 9082                |
| ksqldb     | ksqldb-server   | 9083                |
| ksqldb cli | ksql-cli        | -                   |

You can start the first 4 servers using this command:

```shell
docker compose -f docker-compose.yaml up
```

Or, if you want the first 4 and the `ksqldb` server:  

```shell
docker compose -f docker-compose-ksqldb.yaml up
```

To stop the servers, you can just press `ctrl+c` in the console window.

To stop the servers and remove the containers (clean the topics, registry, stream local storage...), use this command from another terminal or in the same after stopping servers as explained before:

```shell
docker compose -f docker-compose.yaml down
```

Projects are written in Java using Quarkus framework, so to start them you can run this command from every sur-project directory:

```shell
mvn quarkus:dev
```

## Sub-projects

### Avro schema

To be done

### KsqlDB

To be done

### Plain Json

This project contains 3 sub-projects:

* plain-shared-lib
* plain-produced
* plain-stream

#### plain-shared-lib

Is a pure java shared library containing classes representing the objects of the project.

```mermaid
classDiagram
    EnrichedOrder *-- "0..1" Customer
    EnrichedOrder *-- "0..*" OrderLine
    Order *-- "0..*" OrderLine
    Customer *-- "0..*" Address
    
    class OrderLine {
        String lineId
        String productId
        BigDecimal quantity
        BigDecimal unitPrice
    } 
    
    class Order {
        String orderId
        String customerId
        Instant orderDate
    }
    
    class Address {
        String addressId
        String road
        String postalCode
        String city
        String country
    }
    
    class Customer {
        String customerId
        String firstname
        String lastname
   }
   
   class EnrichedOrder {
        String orderId
        Instant orderDate
   }
```

#### plain-producer

This project contains two `ApplicationScoped` beans:

* CustomerGenerator
* OrderGenerator

Each producing messages of their entity.

* **Customers**: limited length list of Customer
* **Orders**: emitted every 500ms, are linked to an existing Customer

#### plain-streams

This stream, gets the new `Order` containing the `customerId` to create a new `EnrichedOrder` with the customer as a sub-object. 

The `EnrichedOrder` will be pushed to the new topic `order-aggregated`. 

As we use a GlobalKTable to store and search the `Customer` there will be another topic `plain-stream-customer-store-changelog` created to materialize this table.

You then, have an `InteractiveQuery` to search the `Order` (for example with the id: `9b47765b-ae0c-4940-823a-cc279f4665e5`) with the `Customer` inside. This query is exposed ath the following endpoint:

```
http://localhost:10002/orders/data/9b47765b-ae0c-4940-823a-cc279f4665e5
```

