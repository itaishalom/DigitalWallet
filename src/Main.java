import wallet.PolynomialRegression;
import wallet.Wallet;

/**
 * Created by Itai on 26/02/2018.
 */
public class Main {
    public static void main(String[] args){
      double[] x = new double[]{0,1,2,3,4,5,6,7};
        double[] y = new double[]{0,1,4,9,16,25,36,49};
        PolynomialRegression p = new PolynomialRegression(x,y,2);
        System.out.println(p.predict(8));
        System.out.println(p.R2());
        Wallet myWallet = new Wallet(1);
        myWallet.store(1,32);
        myWallet.retrieve(1);
    }




}
