package sag.model;

import java.util.ArrayList;
import java.util.stream.IntStream;

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

    /**
     * Liczba klientów liczona na podstawie wymiarów macierzy kosztów.
     * @return Liczba klientów
     */
    public int clientsNumber() {
        return (int) Math.sqrt(costVector.length);
    };

    /**
     * Statyczne tworzenie pustej macierzy kosztów.
     * @return Pusta macierz kosztów.
     */
    public static CostMatrix empty() {
        return new CostMatrix(new double[0]);
    }

    /**
     * Sumuje ilość medium dostarczoną do klienta o podanym indeksie w macierzy kosztów.
     * Przy liczeniu sumy nie uwzględnia się ilości medium otrzymanej od samego siebie (przekątna macierzy).
     * @param clientIndex Indeks odbiorcy medium w macierzy kosztów.
     * @return Łączna ilość otrzymanego medium.
     */
    public double received(int clientIndex) {
        if (costVector.length == 0) {
            return 0.0;
        } else {
            int n = clientsNumber();
            return IntStream.range(0, n)
                    .map(i -> n * i + clientIndex)
                    .filter(idx -> idx != n * clientIndex + clientIndex)
                    .mapToDouble(i -> costVector[i])
                    .sum();
        }
    }

    /**
     * Sumuje ilość medium wyprodukowaną przez klienta o podanym indeksie w macierzy kosztów.
     * Przy liczeniu sumy nie uwzględnia się ilości medium przesłanej samemu sobie (przekątna macierzy).
     * @param clientIndex Indeks producenta medium w macierzy kosztów.
     * @return Łączna ilość wyprodukowanego medium.
     */
    public double sent(int clientIndex) {
        if (costVector.length == 0) {
            return 0.0;
        } else {
            int n = clientsNumber();
            return IntStream.range(0, n)
                .map(i -> n * clientIndex + i)
                .filter(idx -> idx != n * clientIndex + clientIndex)
                .mapToDouble(i -> costVector[i])
                .sum();
        }
    }

    /*
     * Oblicza odległość euclidesową na płaszczyźnie.
     */
    private double distance(final ClientLocation first, final ClientLocation second) {
        return Math.sqrt(Math.pow(first.x() - second.x(), 2) + Math.pow(first.y() - second.y(), 2));
    }
}
