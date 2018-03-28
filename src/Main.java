import wallet.PolynomialRegression;
import wallet.Wallet;

import java.util.Random;

/**
 * Created by Itai on 26/02/2018.
 */
public class Main {
    static Random rand;

    public static void main(String[] args) {
        Wallet myWallet = new Wallet(1);
        rand = new Random();
        int key = rand.nextInt(100)+1;
        int value = rand.nextInt(100)+1;
        System.out.println("###########  value: Storing (" + key + "," + value + ") ##############");
        myWallet.store(key, value);

        testBadValue(1, key, value, myWallet);
        testGoodValue(2, key, value, myWallet);
        testBadValue(3, key, value, myWallet);
        testGoodValue(4, key, value, myWallet);

        testBadValue(5, key, value, myWallet);
        testGoodValue(6, key, value, myWallet);

    }

    private static void testBadValue(int testNum, int key, int value, Wallet myWallet) {
        int badKey = rand.nextInt(100);
        System.out.println("Test " + testNum + " : ######### Reconstruct value: Try to get with BAD key " + badKey + " ##############");
        int badValue = myWallet.retrieve(badKey);
        if (badValue == value) {
            System.err.println("######### BAD RESULT - Got: " + badValue + "==" + value + " ##############");
        } else {
            System.out.println("######### OK! ##############");
        }
    }


    private static void testGoodValue(int testNum, int key, int value, Wallet myWallet) {
        System.out.println("Test " + testNum + " : ######### Reconstruct value: Try to get with key " + key + " ##############");
        int shouldBeValue = myWallet.retrieve(key);
        if (shouldBeValue == value) {
            System.out.println("######### OK ##############");
        } else {
            System.err.println("######### BAD RESULT - retrieve(" + key + ") = " + shouldBeValue + " ##############");
        }
    }

}
