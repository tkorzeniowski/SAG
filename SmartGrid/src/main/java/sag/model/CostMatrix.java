package sag.model;

import java.util.ArrayList;

/**
 * Reprezentuje macierz odległości (euclidesowej) pomiędzy klientami.
 * Odległości liczone są pomiędzy wszystkimi klientami należącymi do danej sieci.
 */
public class CostMatrix {

    public final double[][] costMatrix;

    /**
     * Konstruktor macierzy odległości, wyznaczanych na podstawie położeń klientów.
     * @param locations Lista klientów oraz ich lokacji.
     */
    public CostMatrix(final ArrayList<ClientLocation> locations) {
        int noClients = locations.size();
        double[][] cm = new double[noClients][noClients];

        for (int row = 0; row < noClients; ++row) {
            for (int col = 0; col < noClients; ++col) {
                if (row < col) {
                    cm[row][col] = distance(locations.get(row), locations.get(col));
                    cm[col][row] = cm[row][col];
                } else if (row == col) {
                    cm[row][col] = 0.0;
                }
            }
        }

        costMatrix = cm;
    }

    /*
     * Oblicza odległość euclidesową na płaszczyźnie.
     */
    private double distance(final ClientLocation first, final ClientLocation second) {
        return Math.sqrt(Math.pow(first.x() - second.x(), 2) + Math.pow(first.y() - second.y(), 2));
    }
}
