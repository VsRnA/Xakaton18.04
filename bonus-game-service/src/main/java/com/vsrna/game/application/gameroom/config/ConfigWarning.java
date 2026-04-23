package com.vsrna.game.application.gameroom.config;

public record ConfigWarning(String code, String severity, String message) {

    public static ConfigWarning error(String code, String message) {
        return new ConfigWarning(code, "ERROR", message);
    }

    public static ConfigWarning warn(String code, String message) {
        return new ConfigWarning(code, "WARN", message);
    }

    public static ConfigWarning info(String code, String message) {
        return new ConfigWarning(code, "INFO", message);
    }
}
