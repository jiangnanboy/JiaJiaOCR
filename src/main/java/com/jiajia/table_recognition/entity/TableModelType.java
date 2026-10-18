package com.jiajia.table_recognition.entity;

public enum TableModelType {

    PPSTRUCTURE_EN("ppstructure_en"),
    PPSTRUCTURE_ZH("ppstructure_zh"),
    SLANET_PLUS("slanet_plus");

    private final String modelName;

    TableModelType(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }

}
