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

In addition, if you want to reset the Kafka Stream, you'll have to clear the state folder. During Stream instance startup, KafkaStream logs its configuration. Search for the `state.dir` log entry and remove this folder.

```shell
...
state.dir = /var/folders/9k/bqsqjt1x2pqgy4kbwq03z8sh0000gp/T//kafka-streams
...

rm -fr /var/folders/9k/bqsqjt1x2pqgy4kbwq03z8sh0000gp/T//kafka-streams
```

Projects are written in Java using Quarkus framework, so to start them you can run this command from every sur-project directory:

```shell
mvn quarkus:dev
```

At anytime you can inspect the content of the Topics using the AKHQ instance:

```http request
http://localhost:9082/ui/docker-kafka-server/topic
```

## Sub-projects

### Avro schema

This project contains 3 sub-projects:

* avro-shared-lib
* avro-producer
* avro-streams

#### avro-shared-lib

This project holds the **Avro** schema in the following directory:

```shell
avro-schema-parent/avro-shared-lib/src/main/avro
```

When this project is built using the standard Quarkus Maven commands (like `mvn package`) java classes will be generated from these `.avsc` files.

Generated classes representing the objects of the project.

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

#### avro-producer

This project provides two Rest Endpoints:

* `/v1/orders`
* `/v1/customers`

Each producing messages of their entities in the respective topics:

* `avro.customers` for the Customer Endpoint
* `avro.orders` for the Order Endpoint

I've introduced CloudEvent metadata in the Query and in the Response from these Endpoints. So for a query like the one you have in the sample HTTP Query file `avro-schema-parent/avro-producer/src/test/http/add.customer.http`:

```json
{
  "metadata": {"id": "1234580", "source": "manu", "type": "mine", "dataContentType": "onetype", "subject": "customer", "timestamp": "2022-05-27T09:14:20.543Z"},
  "customerId": "54320",
  "firstname": "Jeanne Michelle",
  "lastname": "DUPONT",
  "addresses": [
    {"addressId": "1234", "road": "12 rue de la liberté", "postalCode": "59280", "city": "ARMENTIERES", "country": "FRANCE"}
  ]
}

```

you will get a response like this one: 

```json
{
  "id": "1234580",
  "specVersion": "1.0",
  "source": "manu",
  "type": "mine",
  "dataContentType": "onetype",
  "dataSchema": "http://localhost:9081/subjects/avro.customers-value/versions",
  "subject": "customer",
  "extensions": {},
  "data": null,
  "timeStamp": "2022-05-27T09:14:20.543Z"
}
```

#### avro-streams

This stream, gets the new `Order` containing the `customerId` to create a new `EnrichedOrder` with the customer as a sub-object.

The `EnrichedOrder` will be pushed to the new topic `avro.order-aggregated`.

As we use a GlobalKTable to store and search the `Customer` there will be another topic `avro-order-aggregator-avro.orders-store-changelog` created to materialize this table.

You then, have an `InteractiveQuery` to search the `Order` (for example with the id: `12352`) with the `Customer` inside. This query is exposed ath the following endpoint:

```http request
http://localhost:10002/orders/data/{orderId}
```

In the sample HTTP files, you have 1 Customer POST requests, and 3 for the Order.

If you run, on your local machine the first request (the Customer request and the first Order request), you will be able to retrieve the Aggregated Order at this endpoint:

```http request
http://localhost:10002/orders/data/12352
```

Now if you run the next Order request, you will create a record for the Order 12351 with only two OrderLines. Try the following Endpoint to look at this Order:

```http request
http://localhost:10002/orders/data/12351
```

Then you can run the last Order request with another version of the same Order, this time with 3 OrderLines. Look at the final version of the Order at this Endpoint:

```http request
http://localhost:10002/orders/data/12351
```

### KsqlDB

To be done

### Plain Json

This project contains 3 sub-projects:

* plain-shared-lib
* plain-producer
* plain-streams

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
