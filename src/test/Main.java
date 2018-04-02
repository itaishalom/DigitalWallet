package test;
import wallet.Wallet;

import java.util.Random;

/**
 * Created by Itai on 26/02/2018.
 */
public class Main {
    private static Random rand;
    public static boolean RUN_FAULT_NODE = false;
    public static void main(String[] args) {
        Wallet myWallet = new Wallet(1);
        rand = new Random();
        int key = rand.nextInt(100)+1;
        int value = rand.nextInt(100)+1;
        System.out.println("###########  value: Storing (" + key + "," + value + ") ##############");
        myWallet.store(key, value);


        while(myWallet.isRunning()){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("#############Testing with all nodes good ################");

        testGoodValue(2, key, value, myWallet);
        testBadValue(1, key, value, myWallet);

        while(myWallet.isRunning()){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("###########  Node 1: will be corrupted now ##############");
        RUN_FAULT_NODE = true;

        testBadValue(3, key, value, myWallet);

        testGoodValue(4, key, value, myWallet);

        while(myWallet.isRunning()){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("###########  Node 1: will be OK ##############");
        RUN_FAULT_NODE = false;


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
