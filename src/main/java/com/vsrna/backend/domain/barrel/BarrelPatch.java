package com.vsrna.backend.domain.barrel;

import java.math.BigDecimal;

public record BarrelPatch(BigDecimal weight) {
    public static BarrelPatch weight(BigDecimal weight) {
        return new BarrelPatch(weight);
    }
}
