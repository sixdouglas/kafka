package org.douglas.kafka.avro.streams;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class IgnoreAvroProperties
{
    @JsonIgnore abstract org.apache.avro.Schema getSchema();
    @JsonIgnore abstract org.apache.avro.specific.SpecificData getSpecificData();
}