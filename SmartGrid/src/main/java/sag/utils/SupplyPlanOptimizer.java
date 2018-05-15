package sag.utils;

import sag.model.CostMatrix;

class SupplyPlanOptimizer {
    public static double[][] optimize(final CostMatrix costMat) {
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

        int clients = costMat.numberOfClients();
        //double[] c = new double[];

        return new double[0][];
    }
}
