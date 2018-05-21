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

class SupplyPlanOptimizer {
    public static CostMatrix optimize(final CostMatrix costMat, final List<ClientOffer> offers) {
        /*
         * param N;
         * param cTransp {1..N,1..N};
         * param dem {1..N};
         * param prod {1..N};
         *
         * var xTransp {1..N,1..N} >= 0;
         *
         * minimize f_celu: sum {i in 1..N, j in 1..N} xTransp[i,j]*cTransp[i,j];
         *
         * subject to ogrZap {j in 1..N}: sum{i in 1..N} xTransp[i,j] >= dem[j];
         * subject to ogrProd {i in 1..N}: sum{j in 1..N} xTransp[i,j] <= prod[i];
         */

        final int n = costMat.clientsNumber();

        LPOptimizationRequest or = new LPOptimizationRequest();
        or.setC(costMat.costVector);
        or.setG(generateG(n));
        or.setH(generateH(offers));
        or.setLb(new double[n*n]);

        LPPrimalDualMethod opt = new LPPrimalDualMethod();
        opt.setLPOptimizationRequest(or);

        try {
            opt.optimize();
        } catch (JOptimizerException exc){
            // Tymczasowo do testów
            throw new RuntimeException(exc.getMessage());
        }

        return new CostMatrix(opt.getOptimizationResponse().getSolution());
    }

    private static double[] generateH(List<ClientOffer> offers) {
        Stream<ClientOffer> offersStream = offers.stream();
        DoubleStream demands = offersStream.mapToDouble(o -> -o.demand());
        DoubleStream productions = offersStream.mapToDouble(ClientOffer::production);
        return DoubleStream.concat(demands, productions).toArray();

//        Arrays.setAll(h, index -> {
//            ClientOffer offer = offers.get(index);
//            return index < offers.size()?
//                -offer.demand() :
//                +offer.production();
//        });
    }

    private static double[][] generateG(final int clientsNumber) {
        IntStream indexStream = IntStream.range(0, clientsNumber);

        Stream<double[]> demandConstraints =
            indexStream
                .mapToObj(row ->
                    indexStream
                        .mapToDouble(idx -> (idx - row) % clientsNumber == 0? -1.0 : 0.0)
                        .toArray()
                );

        Stream<double[]> productionConstraints =
            indexStream
                .mapToObj(row ->
                    indexStream
                        .mapToDouble(idx ->
                            idx >= clientsNumber * row &&
                            idx < (clientsNumber + 1) * row? 1.0 : 0.0
                        )
                        .toArray()
                );

        return Stream
            .concat(demandConstraints, productionConstraints)
            .toArray(double[][]::new);
    }
}
