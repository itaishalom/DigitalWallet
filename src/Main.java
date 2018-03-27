import wallet.PolynomialRegression;
import wallet.Wallet;

import java.util.Random;

/**
 * Created by Itai on 26/02/2018.
 */
public class Main {
    public static void main(String[] args) {
        Wallet myWallet = new Wallet(1);
        Random rand = new Random();
        int key = rand.nextInt(100);
        int value = rand.nextInt(100);

        System.out.println("###########  value: Storing (" + key + "," + value + ") ##############");
        myWallet.store(key, value);
        int badKey = rand.nextInt(100);

        while (badKey == key) {
            badKey = rand.nextInt(100);
        }

        System.out.println("Test 1 : ######### Reconstruct value: Try to get with bad key " + badKey + " ##############");
        int badValue = myWallet.retrieve(badKey);
        if (badValue == value) {
            System.err.println("######### BAD RESULT - Got: "+badValue+"=="+value+" ##############");
        } else {
            System.out.println("######### OK! ##############");
        }
        System.out.println("Test 2 : ######### Reconstruct value: Try to get with key " + key + " ##############");
        int shouldBeValue = myWallet.retrieve(key);
        if (shouldBeValue == value) {
            System.out.println("######### OK ##############");
        } else {
            System.err.println("######### BAD RESULT - retrieve(" + key + ") = " + shouldBeValue + " ##############");
        }


        badKey = rand.nextInt(100);
        while (badKey == key) {
            badKey = rand.nextInt(100);
        }
        System.out.println("Test 3 : ######### Reconstruct value: Try to get with bad key " + badKey + " ##############");
        badValue = myWallet.retrieve(badKey);
        if (badValue == value) {
            System.err.println("######### BAD RESULT - Got: "+badValue+"=="+value+" ##############");
        } else {
            System.out.println("######### OK! ##############");
        }

        System.out.println("Test 4 : ######### Reconstruct value: Try to get with key " + key + " ##############");
        shouldBeValue = myWallet.retrieve(key);
        if (shouldBeValue == value) {
            System.out.println("######### OK ##############");
        } else {
            System.err.println("######### BAD RESULT - retrieve(" + key + ") = " + shouldBeValue + " ##############");
        }
    }


}
