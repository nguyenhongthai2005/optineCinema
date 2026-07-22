package com.opticine.entity;

public enum WorkAssignmentType {
    TICKET_CHECKING("Soát vé", StaffPosition.TICKET_CHECKER),
    COUNTER_SALES("Bán tại quầy", StaffPosition.COUNTER_SALES);

    private final String label;
    private final StaffPosition requiredPosition;

    WorkAssignmentType(String label, StaffPosition requiredPosition) {
        this.label = label;
        this.requiredPosition = requiredPosition;
    }

    public String getLabel() {
        return label;
    }

    public StaffPosition getRequiredPosition() {
        return requiredPosition;
    }
}
