package io.cloudevents;

public record CloudEventMetadata(String id,
                                 String source,
                                 String type,
                                 String dataContentType,
                                 String subject,
                                 String timestamp) {
}










