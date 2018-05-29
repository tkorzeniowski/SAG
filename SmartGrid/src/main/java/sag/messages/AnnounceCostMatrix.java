package sag.messages;

import sag.model.CostMatrix;

/**
 * Rodzaj wiadomości przesyłany przez sieć do swojego nadzorcy w celu przekazania
 * obliczonej macierzy kosztów przesyłu medium.
 */
public class AnnounceCostMatrix {
    public AnnounceCostMatrix(final CostMatrix costMatrix) {
        this.costMatrix = costMatrix;
    }

    public final CostMatrix costMatrix;
}
