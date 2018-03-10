package wallet.node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.util.Random;

import static wallet.node.Functions.broadcast;
import static wallet.node.Message.*;

/**
 * Created by Itai on 26/02/2018.
 */
public class Dealer extends Node {
    private int mFaults;
    private Random mRandom;
    Node[] mNodes;
    private int boundForRandom;
    private int[] q;
    private int[] p;

    public Dealer(int port, int f, Node[] nodes) {
        super(0, port,f);
        mFaults = f;
        mRandom = new Random();
        mNodes = nodes;
        boundForRandom = generatePrime();
    }

    public void startProcess(Object ob) {

        q = createArrayOfCoefs();
        q[0] = 2; // decide what to do
        p = createArrayOfCoefs();
        p[0] = 1;
        for (Node mNode : mNodes) {  // Iterate over all Nodes
            String answer = buildInitialValues(mNode.mNumber, q, p);
            Message msg = new Message(this.mNumber, PRIVATE, INITIAL_VALUES, answer);
            sendMessageToNode(mNode, msg);
        }
    }

    @Override
    protected void startBroadcastReceiver() {
        Thread broadcastReceiver = new BroadcastReceiverDealer();
        broadcastReceiver.start();
    }

    public class BroadcastReceiverDealer extends BroadcastReceiver {

        private BroadcastReceiverDealer() {
            super();

        }

        @Override
        public void run() {
            running = true;
            while (running) {

                Message msg = getMessageFromBroadcast();

                if (msg.isComplaint()) {
                    String data = msg.getmInfo();
                    String[] nodes = data.split("\\|");
                    int i = Integer.parseInt(nodes[0]);
                    int j = Integer.parseInt(nodes[1]);
                    long result1 = computePolynomial(q, i) * computePolynomial(p, j);
                    long result2 = computePolynomial(p, i) * computePolynomial(q, j);
                    Message newMsg = new Message(mNumber, BROADCAST, COMPLAINT_ANSWER,  + i + "," + j + "|" + result1 + "," + result2);

                        broadcast(newMsg,broadCasterSocket);

                }
            }
        }
    }


    private String buildInitialValues(int i, int[] firstPoly, int[] secondPoly) {
        StringBuilder response = new StringBuilder();
        buildResponse(response, i, firstPoly, secondPoly);
        response.append("|");
        buildResponse(response, i, secondPoly, firstPoly);
        return response.toString();
    }

    private void buildResponse(StringBuilder response, int i, int[] q, int[] secondPoly) {
        long val1 = computePolynomial(q, i);
        for (int n = 0; n < mNodes.length; n++) {
            long val2 = computePolynomial(secondPoly, mNodes[n].mNumber);
            if (n == mNodes.length - 1)
                response.append(val1 * val2);
            else
                response.append(val1 * val2).append(",");
        }
    }

    /**
     * Computes the polynomial arr[0]*x^0 + .. + arr[f]*x^f
     *
     * @param arr - array of coefficients
     * @param x   - the value to calculate with
     * @return - The value of the polynomial in x.
     */
    private long computePolynomial(int[] arr, int x) {
        long res = 0;
        for (int i = 0; i < arr.length; i++) {
            res += arr[i] * Math.pow(x, i);
        }
        return res;
    }

    /**
     * Randomly creates array of coefficients to simulate a polynomial
     *
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
     *
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
