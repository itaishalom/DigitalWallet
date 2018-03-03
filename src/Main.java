import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunctionLagrangeForm;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import wallet.PolynimialFit;
import wallet.PolynomialRegression;
import wallet.Wallet;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.dense.row.linsol.AdjustableLinearSolver_DDRM;
import java.util.Arrays;

/**
 * Created by Itai on 26/02/2018.
 */
public class Main {
    public static void main(String[] args){
        double[] x = new double[]{0,1,2,3,4};
        double[] y = new double[]{0,1,6,9,16};
        PolynomialRegression p = new PolynomialRegression(x,y,2);
        System.out.println(p.R2());

        Wallet myWallet = new Wallet(3);
        myWallet.store("key","value");
    }
}
