import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunctionLagrangeForm;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import wallet.Polynomial;
import wallet.Wallet;

import java.util.Arrays;

/**
 * Created by Itai on 26/02/2018.
 */
public class Main {
    public static void main(String[] args){
        double[] x = new double[]{0,1,2,3,4};
        double[] y = new double[]{0,1,4,9,16};

        P interp = new LinearInterpolator();
        PolynomialSplineFunction f = interp.interpolate(x, y);
        System.out.println(Arrays.toString(f.getPolynomials()));
        Arrays.stream(f.getPolynomials()).forEach(System.out::println);

        Wallet myWallet = new Wallet(3);
        myWallet.store("key","value");
    }
}
