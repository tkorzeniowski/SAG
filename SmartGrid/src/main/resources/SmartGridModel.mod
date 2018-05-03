#reset;
param N;

param cTransp {1..N,1..N};
param zap {1..N};
param prod {1..N};

var xTransp {1..N,1..N} >= 0;





# funkcja celu
minimize f_celu: sum {i in 1..N, j in 1..N} xTransp[i,j]*cTransp[i,j];


subject to ogrZap {j in 1..N}: sum{i in 1..N} xTransp[i,j] >= zap[j];
subject to ogrProd {i in 1..N}: sum{j in 1..N} xTransp[i,j] <= prod[i];

#data SmartGridData.dat;
#option solver cplex;

#solve;

#display xTransp;