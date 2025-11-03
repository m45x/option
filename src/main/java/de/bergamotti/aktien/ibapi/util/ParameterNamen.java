package de.bergamotti.aktien.ibapi.util;

public enum ParameterNamen {
    IS_STOPLOSS_ACTIVE("isStoplossActive"),
    IS_STOPLOSS_ABSOLUT_ACTIVE("isStoplossAbsolutActive"),
    WERT_ABSOLUT_STOPLOSS("wertAbsolutStoploss");

    private final String key;

    ParameterNamen(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return key;
    }

}