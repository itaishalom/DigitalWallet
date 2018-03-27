import wallet.PolynomialRegression;
import wallet.Wallet;

/**
 * Created by Itai on 26/02/2018.
 */
public class Main {
    public static void main(String[] args){
        Wallet myWallet = new Wallet(1);
        myWallet.store(1,32);
        System.out.println("######### Reconstruct value: " + myWallet.retrieve(2) +" ##############");
        System.out.println("######### Reconstruct value: " + myWallet.retrieve(1) +" ##############");
    }




}
