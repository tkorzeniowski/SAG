package sag.model;

import java.util.ArrayList;

/**
 * Reprezentuje macierz odległości (euclidesowej) pomiędzy klientami.
 * Odległości liczone są pomiędzy wszystkimi klientami należącymi do danej sieci.
 */
public class CostMatrix {

    public final double[] costVector;

    /**
     * Konstruktor macierzy odległości, wyznaczanych na podstawie położeń klientów.
     * @param locations Lista klientów oraz ich lokacji.
     */
    public CostMatrix(final ArrayList<ClientLocation> locations) {
        int n = locations.size();
        costVector = new double[n*n];

        for (int row = 0; row < n; ++row) {
            for (int col = 0; col < n; ++col) {
                if (row < col) {
                    costVector[row*n + col] = distance(locations.get(row), locations.get(col));
                } else if (row == col) {
                    costVector[row*n + col] = 0.0;
                } else {
                    costVector[row*n+col] = costVector[col*n + row];
                }
            }
        }
    }

    public CostMatrix(final double[] costVector) {
        this.costVector = costVector;
    }

    public int clientsNumber() {
        return (int) Math.sqrt(costVector.length);
    };

    public static CostMatrix empty() {
        return new CostMatrix(new double[0]);
    }

    /*
     * Oblicza odległość euclidesową na płaszczyźnie.
     */
    private double distance(final ClientLocation first, final ClientLocation second) {
        return Math.sqrt(Math.pow(first.x() - second.x(), 2) + Math.pow(first.y() - second.y(), 2));
    }
}
