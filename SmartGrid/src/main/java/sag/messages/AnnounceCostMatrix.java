package sag.messages;

import sag.model.CostMatrix;

public class AnnounceCostMatrix {
    public AnnounceCostMatrix(final CostMatrix costMatrix) {
        this.costMatrix = costMatrix;
    }

    public final CostMatrix costMatrix;
}
