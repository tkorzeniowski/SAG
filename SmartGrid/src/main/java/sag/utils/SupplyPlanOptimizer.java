package sag.utils;

import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.optimizers.LPOptimizationRequest;
import com.joptimizer.optimizers.LPPrimalDualMethod;
import sag.model.ClientOffer;
import sag.model.CostMatrix;

import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Obliczanie optymalnego planu dostaw wyznaczanego na podstawie listy ofert
 * oraz kosztów przesyłu pomiędzy klientami.
 * Głównym zadaniem optymalizatora jest rozwiązanie zadania programowania liniowego
 * poprzez minimalizację funkcji postaci: c^T * x + d
 * przy ograniczeniach większościowych G * x <= h
 * oraz ograniczeniach dolnych wartości zmiennych lb.
 */
public class SupplyPlanOptimizer {
    /**
     * Realizacja optymalizacji planu dostaw.
     * @param costMat Macierz kosztów dostawy medium pomiędzy klientami,
     * @param offers Lista klientów oraz ich ofert (obejmujących zarówno produkcję jak i zapotrzebowanie)
     * @return Zoptymalizowana macierz kosztów stanowiąca optymalny plan dostaw.
     */
    public static CostMatrix optimize(final CostMatrix costMat, final List<ClientOffer> offers) {

        final int n = costMat.clientsNumber();
        System.out.println("Wyznaczam plan dla klientow " + n);
        // Nie optymalizuj, jeżeli nie ma czego.
        if (n == 0) {
            return CostMatrix.empty();
        }

        // Stworzenie nowego zadania optymalizacji.
        LPOptimizationRequest or = new LPOptimizationRequest();
        // Przypisanie wartości konkretnym wektorom i macierzy zgodnie ze wzorem.
        or.setC(costMat.costVector);
        or.setG(generateG(n));
        or.setH(generateH(offers));
        or.setLb(new double[n*n]);
		or.setTolerance(1.E-12);						

        // Wybór metody optymalizacji.
        LPPrimalDualMethod opt = new LPPrimalDualMethod();
        opt.setLPOptimizationRequest(or);

        try {
            // Faktyczna optymalizacja
            opt.optimize();
        } catch (JOptimizerException exc) {
            // Tymczasowo do testów
            //throw new RuntimeException(exc.getMessage());
			return CostMatrix.empty();
        }

        // Przesłanie zoptymalizowanej macierzy kosztów.
        return new CostMatrix(opt.getOptimizationResponse().getSolution());
    }

    /*
     * Pomocnicza metoda wypełniająca wektor h zgodnie ze wzorem:
     * [-d_1, -d_2, ..., -d_n, p_1, p_2, ..., p_n]^T, gdzie:
     * d_i - zapotrzebowanie na medium i-tego klienta,
     * p_i - produkcja medium przez i-tego klienta.
     */
    private static double[] generateH(List<ClientOffer> offers) {
        DoubleStream demands = offers.stream().mapToDouble(o -> -o.demand());
        DoubleStream productions = offers.stream().mapToDouble(ClientOffer::production);
        return DoubleStream.concat(demands, productions).toArray();
    }

    /*
     * Metoda pomocnicza wypełniająca macierz G. Przykładowe wartości dla 3 klientów:
     *
     * |-1,  0,  0, -1,  0,  0, -1,  0,  0|
     * | 0, -1,  0,  0, -1,  0,  0, -1,  0|     gdzie pierwsze 3 rzędy macierzy reprezentują ograniczenia
     * | 0,  0, -1,  0,  0, -1,  0,  0, -1|     mniejszościowe zapotrzebowania klientów na medium,
     * | 1,  1,  1,  0,  0,  0,  0,  0,  0|     zaś pozostałe - ograniczenia większościowe produkcji
     * | 0,  0,  0,  1,  1,  1,  0,  0,  0|     medium przez klientów, które w powiązaniu z wektorem h
     * | 0,  0,  0,  0,  0,  0,  1,  1,  1|     można powiązać w następujący sposób:
     *
     *  Łączna ilość medium dostarczonego do danego klienta musi być większa bądź równa jego zapotrzebowaniu.
     *  -> ogrZap {j in 1..N}: sum{i in 1..N} xTransp[i,j] >= dem[j];
     *  Producent nie może rozesłać więcej medium niż wyprodukował.
     *  -> ogrProd {i in 1..N}: sum{j in 1..N} xTransp[i,j] <= prod[i];
     */
    private static double[][] generateG(final int clientsNumber) {
        Stream<double[]> demandConstraints =
            indexStream(clientsNumber)
                .mapToObj(row ->
                    indexStream(clientsNumber * clientsNumber)
                        .mapToDouble(idx -> (idx - row) % clientsNumber == 0? -1.0 : 0.0)
                        .toArray()
                );

        Stream<double[]> productionConstraints =
            indexStream(clientsNumber)
                .mapToObj(row ->
                    indexStream(clientsNumber * clientsNumber)
                        .mapToDouble(idx ->
                            idx >= clientsNumber * row &&
                            idx < clientsNumber * (row + 1)? 1.0 : 0.0
                        )
                        .toArray()
                );

        return Stream
            .concat(demandConstraints, productionConstraints)
            .toArray(double[][]::new);
    }

    /*
     * Funkcja pomocnicza tworząca nowy strumień Intów z kolejnymi wartościami od 0 do n.
     */
    private static IntStream indexStream(final int n) {
        return IntStream.range(0, n);
    }
}
