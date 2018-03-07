import wallet.PolynomialRegression;
import wallet.Wallet;

/**
 * Created by Itai on 26/02/2018.
 */
public class Main {
    public static void main(String[] args){
/*        double[] x = new double[]{0,1,2,3,4};
        double[] y = new double[]{0,1,6,9,16};
        PolynomialRegression p = new PolynomialRegression(x,y,2);
        System.out.println(p.R2());*/

        Wallet myWallet = new Wallet(1);
        myWallet.store("key","value");
    }
}
