package org.douglas.kafka.plainjson;

import java.math.BigDecimal;

public record OrderLine(String lineId, String productId, BigDecimal quantity, BigDecimal unitPrice) {
}
