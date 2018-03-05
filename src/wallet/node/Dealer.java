package wallet.node;

import java.util.Random;

import static wallet.node.Message.INITIAL_VALUES;
import static wallet.node.Message.PRIVATE;

/**
 * Created by Itai on 26/02/2018.
 */
public class Dealer extends Node {
    private int mFaults;
    private Random mRandom;
    Node[] mNodes;
    private int boundForRandom;

    public Dealer(int port, int f, Node[] nodes) {
        super(0, port);
        mFaults = f;
        mRandom = new Random();
        mNodes = nodes;
        boundForRandom = generatePrime();
    }

    public void startProcess(Object ob) {

        int[] q = createArrayOfCoefs();
        q[0] = 2; // decide what to do
        int[] p = createArrayOfCoefs();
        p[0] = 1;
        for (Node mNode : mNodes) {  // Iterate over all Nodes
            String answer = buildInitialValues(mNode.mNumber, q, p);
            Message msg = new Message(this.mNumber, PRIVATE, INITIAL_VALUES, answer);
            sendMessageToNode(mNode, msg);
        }
    }

    private String buildInitialValues(int i, int[] q, int[] p) {
        StringBuilder response = new StringBuilder();
        buildResponse(response, i, q, p);
        response.append("|");
        buildResponse(response, i, p, q);
        return response.toString();
    }

    private void buildResponse(StringBuilder response, int i, int[] q, int[] p) {
        int val1 = computePolynomial(q, i);
        for (int n = 0; n < mNodes.length; n++) {
            int val2 = computePolynomial(p, mNodes[n].mNumber);
            if (n == mNodes.length - 1)
                response.append(val1 * val2);
            else
                response.append(val1 * val2).append(",");
        }
    }

    /**
     * Computes the polynomial arr[0]*x^0 + .. + arr[f]*x^f
     * @param arr - array of coefficients
     * @param x - the value to calculate with
     * @return - The value of the polynomial in x.
     */
    private int computePolynomial(int[] arr, int x) {
        int res = 0;
        for (int i = 0; i < arr.length; i++) {
            res += arr[i] * Math.pow(x, i);
        }
        return res;
    }

    /**
     * Randomly creates array of coefficients to simulate a polynomial
     * @return - Array of coefficients
     */
    private int[] createArrayOfCoefs() {
        int[] arr = new int[mFaults + 1];
        for (int i = 1; i < arr.length; i++) {
            arr[i] = mRandom.nextInt(boundForRandom);
        }
        return arr;
    }

    /**
     * Calculates the first prime number that is greater than 10*f
     * @return prime number that is greater than 10*f
     */
    private int generatePrime() {
        boolean isPrime = true;
        int n = (10 * mFaults);
        do {
            isPrime = true;
            n++;
            for (long factor = 2; factor * factor <= n; factor++) {

                // if factor divides evenly into n, n is not prime, so break out of loop
                if (n % factor == 0) {
                    isPrime = false;
                    break;
                }
            }
        } while (!isPrime);
        return n;
    }

}
