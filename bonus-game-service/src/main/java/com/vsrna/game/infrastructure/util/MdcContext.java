package com.vsrna.game.infrastructure.util;

import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;

/**
 * AutoCloseable wrapper for SLF4J MDC.
 * Usage: try (var mdc = MdcContext.of("roomId", id, "round", "1")) { ... }
 */
public final class MdcContext implements AutoCloseable {

    private final List<String> keys = new ArrayList<>();

    private MdcContext() {}

    public static MdcContext of(String... keysAndValues) {
        MdcContext ctx = new MdcContext();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            String key = keysAndValues[i];
            String value = keysAndValues[i + 1];
            if (value != null) {
                MDC.put(key, value);
                ctx.keys.add(key);
            }
        }
        return ctx;
    }

    @Override
    public void close() {
        keys.forEach(MDC::remove);
    }
}
