package wallet.node;

import java.util.Random;

import static wallet.node.Functions.broadcast;
import static wallet.node.Functions.generatePrime;
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
    private boolean[][] okCounter;
    private Thread[] waitForOks;


    public Dealer(int port, int f, Node[] nodes) {
        super(0, port, f);
        okCounter = new boolean[2][3 * f];
        waitForOks = new Thread[2];
        mFaults = f;
        mRandom = new Random();
        mNodes = nodes;
        boundForRandom = generatePrime(mFaults);
    }

    public void startProcess(Object ob) {
        q = createArrayOfCoefs();
        q[0] = 2; // decide what to do
        p = createArrayOfCoefs();
        p[0] = 1;
        for (Node node_i : mNodes) {  // Iterate over all Nodes
            calculateAndPrivateSendValues(node_i.mNumber, node_i.getPort());
        }
    }

    private void calculateAndPrivateSendValues(int nodeNumber, int nodePort) {
        String answer = buildInitialValues(nodeNumber, q, p);
        Message msg = new Message(this.mNumber, KEY, PRIVATE, INITIAL_VALUES, answer);
        sendMessageToNode(nodePort, msg);
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
                switch (msg.getmSubType()) {
                    case COMPLAINT: {
                        String data = msg.getmInfo();
                        String[] nodes = data.split("\\|");
                        int i = Integer.parseInt(nodes[0]);
                        int j = Integer.parseInt(nodes[1]);
                        long result1 = computePolynomial(q, i) * computePolynomial(p, j);
                        long result2 = computePolynomial(p, i) * computePolynomial(q, j);
                        Message newMsg = new Message(mNumber, msg.getProcessType(), BROADCAST, COMPLAINT_ANSWER, +i + "," + j + "|" + result1 + "," + result2);
                        broadcast(newMsg, broadCasterSocket);
                        break;
                    }
                    case OK: {
                        okCounter[msg.getProcessType()][msg.getmFrom() - 1] = true;
                        if (waitForOks[msg.getProcessType()] == null) {
                            waitForOks[msg.getProcessType()] = new WaitForOkDealer(msg.getProcessType());
                            waitForOks[msg.getProcessType()].start();
                        }
                        break;
                    }
                }
            }
        }
    }

    public class WaitForOkDealer extends WaitForOk {

        public WaitForOkDealer(int processType) {
            super(processType);
        }

        @Override
        public void run() {
            try {
                attemptNumbers++;
                int counter = 0;
                while (counter < n - f) {
                    Thread.sleep(5000);
                    counter = 0;
                    for (int i = 0; i < okCounter[mProcessType].length; i++) {
                        if (okCounter[mProcessType][i])
                            counter++;
                    }
                    if (attemptNumbers == TOTAL_ATTEMPTS) {
                        return;
                    }
                }
                for (int i = 0; i < okCounter[mProcessType].length; i++) {
                    if (!okCounter[mProcessType][i]) {
                        String answer = buildInitialValues(i + 1, q, p);
                        Message msg = new Message(mNumber, mProcessType, BROADCAST, NO_OK_ANSWER, (i + 1) + "|" + answer);
                        broadcast(msg, broadCasterSocket);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
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


}
