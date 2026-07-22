package com.opticine.entity;

public enum StaffPosition {
    TICKET_CHECKER("Soát vé"),
    COUNTER_SALES("Bán tại quầy");

    private final String label;

    StaffPosition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
