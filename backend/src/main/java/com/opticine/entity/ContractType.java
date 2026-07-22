package com.opticine.entity;

public enum ContractType {
    SEASONAL("Thời vụ"),
    FULL_TIME("Hợp đồng chính thức");

    private final String label;

    ContractType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
