CREATE STREAM customers (
    customer_id VARCHAR KEY,
    firstname VARCHAR,
    lastname VARCHAR,
    birthdate BIGINT)
WITH (kafka_topic='ksqldb.streams.customers', PARTITIONS=1, REPLICAS=1, value_format='JSON');

CREATE STREAM orders (
    order_id VARCHAR KEY,
    customer_id VARCHAR,
    order_date BIGINT,
    total_amount DOUBLE,
    vat_amount DOUBLE)
WITH (kafka_topic='ksqldb.streams.orders', PARTITIONS=1, REPLICAS=1, value_format='JSON');

----------------

CREATE TABLE order_events AS
    SELECT order_id,
           LATEST_BY_OFFSET(customer_id) AS customer_id,
           LATEST_BY_OFFSET(order_date) AS order_date,
           LATEST_BY_OFFSET(total_amount) AS total_amount,
           LATEST_BY_OFFSET(vat_amount) AS vat_amount
    FROM orders
    GROUP BY order_id
    EMIT CHANGES;

CREATE TABLE customer_events AS
    SELECT customer_id,
           LATEST_BY_OFFSET(firstname) AS firstname,
           LATEST_BY_OFFSET(lastname) AS lastname,
           LATEST_BY_OFFSET(birthdate) AS birthdate
    FROM customers
    GROUP BY customer_id
    EMIT CHANGES;

CREATE TABLE orders_enriched AS
    SELECT order_id,
           order_date,
           total_amount,
           vat_amount,
           customers.customer_id,
           firstname,
           lastname,
           birthdate
    FROM order_events orders
        INNER JOIN customer_events customers ON customers.customer_id = orders.customer_id
    EMIT CHANGES;

----------------

INSERT INTO customers (customer_id, firstname, lastname, birthdate) values ('2345', 'sand', 'six',  176552789);
INSERT INTO customers (customer_id, firstname, lastname, birthdate) values ('1234', 'doug', 'six',  212279189);
INSERT INTO customers (customer_id, firstname, lastname, birthdate) values ('3456', 'tim',  'six', 1054128389);
INSERT INTO customers (customer_id, firstname, lastname, birthdate) values ('4567', 'kath', 'six', 1101619589);
INSERT INTO customers (customer_id, firstname, lastname, birthdate) values ('5678', 'lily', 'six', 1237915589);

INSERT INTO orders (order_id, customer_id, order_date, total_amount, vat_amount) values ('12345', '1234', 1654452589, 12.34, 0.80);
INSERT INTO orders (order_id, customer_id, order_date, total_amount, vat_amount) values ('12346', '2345', 1654453589, 23.34, 1.90);
INSERT INTO orders (order_id, customer_id, order_date, total_amount, vat_amount) values ('12347', '3456', 1654451589, 21.43, 2.20);
INSERT INTO orders (order_id, customer_id, order_date, total_amount, vat_amount) values ('12348', '4567', 1654450589, 43.23, 2.34);

----------------

-- View the results
SELECT *
FROM orders_enriched;

----------------

-- Lets update Tim's birthdate
INSERT INTO customers (customer_id, firstname, lastname, birthdate) values ('3456', 'tim',  'six', 1054128400);

-- View the results
SELECT *
FROM orders_enriched;

----------------

-- Lets update the amounts of Tim's order
INSERT INTO orders (order_id, customer_id, order_date, total_amount, vat_amount) values ('12347', '3456', 1654451589, 22.66, 2.40);

-- View the results
SELECT *
FROM orders_enriched;
