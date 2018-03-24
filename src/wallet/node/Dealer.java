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
     Random mRandom;
     int boundForRandom;
     int[][] q;
     int[][] p;
     boolean[][] okCounter;
     Thread[] waitForOks;
    Thread broadcastReceiver;
    BroadcastReceiver container;

    public Dealer(int num, int port, int f) {
        super(num, port, f);
        q = new int[2][];
        p = new int[2][];
        okCounter = new boolean[TOTAL_PROCESS_VALUES][(3 * f) + 1];
        waitForOks = new Thread[TOTAL_PROCESS_VALUES];
        mFaults = f;
        mRandom = new Random();
        boundForRandom = generatePrime(mFaults);
    }

    public boolean isStoreDone(){
        return  ProtocolDone[VALUE];
    }

    public void startProcess(int key, int value) {
        System.out.println("#############  Begin store key #############");
        q[KEY] = createArrayOfCoefs();
        q[KEY][0] = key; // decide what to do
        p[KEY] = createArrayOfCoefs();
        p[KEY][0] = 1;
        for (Node node_i : mAllNodes) {  // Iterate over all Nodes
            calculateAndPrivateSendValues(node_i.mNumber, node_i.getPort(), KEY);
        }
        waitForProcessEnd wait = new waitForProcessEnd(value);
        wait.start();
    }

    public class waitForProcessEnd extends Thread {
        int attemptNumbers = 0;
        int TOTAL_ATTEMPTS = 5;
        int mValue;

        waitForProcessEnd(int value) {
            mValue = value;
        }

        @Override
        public void run() {
            while (!ProtocolDone[KEY]) {
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                attemptNumbers++;
                if (attemptNumbers == TOTAL_ATTEMPTS) {
                    System.out.println("Failed Storing key");
                    return;
                }
            }
            System.out.println("############# Begin store value #############");
            q[VALUE] = createArrayOfCoefs();
            q[VALUE][0] = mValue; // decide what to do
            p[VALUE] = createArrayOfCoefs();
            p[VALUE][0] = 1;
            for (Node node_i : mAllNodes) {  // Iterate over all Nodes
                calculateAndPrivateSendValues(node_i.mNumber, node_i.getPort(), VALUE);
            }
        }
    }


    protected void calculateAndPrivateSendValues(int nodeNumber, int nodePort, int proccessNumber) {
        String answer = buildInitialValues(nodeNumber, q[proccessNumber], p[proccessNumber]);
        Message msg = new Message(this.mNumber, proccessNumber, PRIVATE, INITIAL_VALUES, answer);
        communication.sendMessageToNode(nodePort, msg);
    }

    public void switchReciever(){
        container.shutdown();
        super.startBroadcastReceiver();
    }


    @Override
    protected void startBroadcastReceiver() {
        container = new BroadcastReceiverDealer();
        broadcastReceiver = new Thread(container);
        broadcastReceiver.start();
    }

    public class BroadcastReceiverDealer extends BroadcastReceiver {

        BroadcastReceiverDealer() {
            super();
        }

        @Override
        public void run() {
            while (running) {
                Message msg = getMessageFromBroadcast();
                switch (msg.getmSubType()) {
                    case PROTOCOL_COMPLETE:
                        if (!ProtocolDone[msg.getProcessType()]) {
                            ProtocolDone[msg.getProcessType()] = true;
                            printResults(msg.getProcessType(), Integer.valueOf(msg.getmInfo()));

                        }
                        break;
                    case COMPLAINT: {
                        mComplaintNumber[msg.getProcessType()]++;
                        String data = msg.getmInfo();
                        String[] nodes = data.split("\\|");
                        int i = Integer.parseInt(nodes[0]);
                        int j = Integer.parseInt(nodes[1]);
                        if (j == mNumber)
                            mComplaintNumber[msg.getProcessType()]--;
                        long result1 = computePolynomial(q[msg.getProcessType()], i) * computePolynomial(p[msg.getProcessType()], j);
                        long result2 = computePolynomial(p[msg.getProcessType()], i) * computePolynomial(q[msg.getProcessType()], j);
                        Message newMsg = new Message(mNumber, msg.getProcessType(), BROADCAST, COMPLAINT_ANSWER, +i + "," + j + "|" + result1 + "," + result2);
                        broadcast(newMsg, broadCasterSocket);
                        break;
                    }
                    case OK: {
                        mOkNumber[msg.getProcessType()]++;
                        okCounter[msg.getProcessType()][msg.getmFrom() - 1] = true;
                        if (waitForOks[msg.getProcessType()] == null) {
                            waitForOks[msg.getProcessType()] = new WaitForOkDealer(msg.getProcessType());
                            waitForOks[msg.getProcessType()].start();
                        }
                        break;
                    }
                    case COMPLAINT_ANSWER: {
                        mComplaintResponseNumber[msg.getProcessType()]++;
                        break;
                    }
                }
            }
        }
    }

    public class WaitForOkDealer extends WaitForOk {

        public WaitForOkDealer(int processType) {
            super(processType, 0);
        }

        @Override
        public void run() {
            try {
                attemptNumbers++;
                int counter = 0;
                while (counter < mNumberOfValues - mFaults) {
                    Thread.sleep(5000);
                    counter = 0;
                    for (int i = 0; i < okCounter[okProcNumber].length; i++) {
                        if (okCounter[okProcNumber][i])
                            counter++;
                    }
                    if (attemptNumbers == TOTAL_ATTEMPTS) {
                        System.out.println("Dealer failed");
                        return;
                    }
                }
                boolean isProtocolDone = true;
                for (int i = 0; i < okCounter[okProcNumber].length; i++) {
                    if (!okCounter[okProcNumber][i]) {
                        String answer = buildInitialValues(i + 1, q[okProcNumber], p[okProcNumber]);
                        Message msg = new Message(mNumber, okProcNumber, BROADCAST, NO_OK_ANSWER, (i + 1) + "|" + answer);
                        broadcast(msg, broadCasterSocket);
                        isProtocolDone = false;
                    }
                }

                if (isProtocolDone && !ProtocolDone[okProcNumber]) {
                    System.out.println("shit2");
                    ProtocolDone[okProcNumber] = true;
                    printResults(okProcNumber, 1);

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

    protected void buildResponse(StringBuilder response, int i, int[] q, int[] secondPoly) {
        long val1 = computePolynomial(q, i);
        for (int n = 0; n < mAllNodes.length; n++) {
            long val2 = computePolynomial(secondPoly, mAllNodes[n].mNumber);
            if (n == mAllNodes.length - 1)
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
    protected long computePolynomial(int[] arr, int x) {
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
    protected int[] createArrayOfCoefs() {
        int[] arr = new int[mFaults + 1];
        for (int i = 1; i < arr.length; i++) {
            arr[i] = mRandom.nextInt(boundForRandom);
        }
        return arr;
    }


}
