# narrow-embedded-kafka-rule-to-asserting-tests

Narrow the `test-strategy` prohibition on `@EmbeddedKafka` so it binds on tests that assert Kafka behaviour, leaving a test that needs only the Kafka beans at context startup free to use the cheapest broker that works.
