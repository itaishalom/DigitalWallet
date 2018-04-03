package wallet.node;

import java.util.Random;

import static wallet.node.Functions.computePolynomial;
import static wallet.node.Functions.createArrayOfCoefs;
import static wallet.node.Functions.generatePrime;
import static wallet.node.Message.*;

/**
 * Created by Itai on 26/02/2018.
 */
public class Dealer extends Node {
    Random mRandom;
    int boundForRandom;
    int[][] q;
    int[][] p;
    boolean[][] okCounter;
    Thread[] waitForOks;
    Thread broadcastReceiver;
    BroadcastReceiver container;
    private boolean isDealerReceiver = true;

    public Dealer(int num, int port, int f, int clientPort) {
        super(num, port, f, clientPort);
        q = new int[2][];
        p = new int[2][];
        okCounter = new boolean[TOTAL_PROCESS_VALUES][(3 * f) + 1];
        waitForOks = new Thread[TOTAL_PROCESS_VALUES];
        mFaults = f;
        mRandom = new Random();
        boundForRandom = generatePrime(mFaults);
    }

    public boolean isStoreDone() {
        return ProtocolDone[VALUE];
    }

    protected void sendRefresh(int process) {
        Message msg = new Message(mNumber, process, BROADCAST, REFRESH, REFRESH);
        communication.broadcast(msg,broadCasterSocket);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void startProcess(int key, int value) {
        if (!isDealerReceiver)
            startBroadcastReceiver();
        //   refresh(KEY);
        sendRefresh(KEY);

        System.out.println("#############  Begin store key #############");
        q[KEY] = createArrayOfCoefs(mFaults, boundForRandom, mRandom);
        q[KEY][0] = key; // decide what to do
        p[KEY] = createArrayOfCoefs(mFaults, boundForRandom, mRandom);
        p[KEY][0] = 1;
        for (Node node_i : mAllNodes) {  // Iterate over all Nodes
            calculateAndPrivateSendValues(node_i.mNumber, node_i.getPort(), KEY, KEY);
        }
        waitForProcessEnd wait = new waitForProcessEnd(value, VALUE, KEY, "Begin store value");
        wait.start();
    }

    public class waitForProcessEnd extends Thread {
        int attemptNumbers = 0;
        int TOTAL_ATTEMPTS = 5;
        int mValue;
        int mProcess;
        int mEndProcess;
        String mInfo;

        waitForProcessEnd(int value, int prosess, int endProcess, String message) {
            mValue = value;
            mProcess = prosess;
            mEndProcess = endProcess;
            mInfo = message;
        }

        @Override
        public void run() {
            while (!ProtocolDone[mEndProcess]) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                attemptNumbers++;
                if (attemptNumbers == TOTAL_ATTEMPTS) {
                    print("Dealer failed");
                    return;
                }
            }
            //    refresh(mProcess);
            sendRefresh(mProcess);
            System.out.println("############# " + mInfo + "  #############");
            q[VALUE] = createArrayOfCoefs(mFaults, boundForRandom, mRandom);
            q[VALUE][0] = mValue; // decide what to do
            p[VALUE] = createArrayOfCoefs(mFaults, boundForRandom, mRandom);
            p[VALUE][0] = 1;
            for (Node node_i : mAllNodes) {  // Iterate over all Nodes
                calculateAndPrivateSendValues(node_i.mNumber, node_i.getPort(), VALUE, mProcess);
            }
        }
    }


    void calculateAndPrivateSendValues(int nodeNumber, int nodePort, int proccessNumber, int messageProcess) {
        String answer = buildInitialValues(nodeNumber, q[proccessNumber], p[proccessNumber]);
        Message msg = new Message(this.mNumber, messageProcess, PRIVATE, INITIAL_VALUES, answer);
        communication.sendMessageToNode(nodePort, msg);
    }

    public void switchReciever() {
        isDealerReceiver = false;
        container.shutdown();
        super.startBroadcastReceiver();
    }


    @Override
    protected void startBroadcastReceiver() {
        isDealerReceiver = true;
        if (container != null) {
            container.shutdown();
        }
        container = new BroadcastReceiverDealer();
        broadcastReceiver = new Thread(container);
        broadcastReceiver.start();
    }


    public class BroadcastReceiverDealer extends BroadcastReceiver {

        BroadcastReceiverDealer() {
            super();
        }


        public void shutdown() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                Message msg = getMessageFromBroadcast();
                if (!running)
                    return;
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
                        long result1 = computePolynomial(getQValue(msg.getProcessType()), i) * computePolynomial(getPValue(msg.getProcessType()), j);
                        long result2 = computePolynomial(getPValue(msg.getProcessType()), i) * computePolynomial(getQValue(msg.getProcessType()), j);
                        Message newMsg = new Message(mNumber, msg.getProcessType(), BROADCAST, COMPLAINT_ANSWER, +i + "," + j + "|" + result1 + "," + result2);
                        communication.broadcast(newMsg,broadCasterSocket);
                        break;
                    }
                    case OK: {
                        mOkNumber[msg.getProcessType()]++;
                        okCounter[msg.getProcessType()][msg.getmFrom() - 1] = true;
                        if (waitForOks[msg.getProcessType()] == null) {
                            synchronized (this) {
                                if (waitForOks[msg.getProcessType()] == null) {
                                    print("wait for ok starting for process " + getProcessFromNumber(msg.getProcessType()));
                                    waitForOks[msg.getProcessType()] = new WaitForOkDealer(msg.getProcessType());
                                    if (!((WaitForOkDealer) waitForOks[msg.getProcessType()]).isRunning())
                                        waitForOks[msg.getProcessType()].start();
                                }
                            }
                        }
                        break;
                    }
                    case COMPLAINT_ANSWER: {
                        mComplaintResponseNumber[msg.getProcessType()]++;
                        break;
                    }
                    case G_VALUES: {
                        waitForGValues();
                        break;
                    }
                }
            }
        }
    }

    protected void waitForGValues() {

    }

    protected int[] getQValue(int processNumber) {
        return q[processNumber];
    }

    protected int[] getPValue(int processNumber) {
        return p[processNumber];
    }

    public class WaitForOkDealer extends WaitForOk {
        boolean running = false;

        public WaitForOkDealer(int processType) {
            super(processType, 0);
            attemptNumbers = 0;
        }

        public boolean isRunning() {
            return running;
        }

        @Override
        public void run() {
            running = true;
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
                        try {
                            String answer = buildInitialValues(i + 1, getQValue(okProcNumber), getPValue(okProcNumber));
                            mComplaintResponseNumber[okProcNumber]++;
                            Message msg = new Message(mNumber, okProcNumber, BROADCAST, NO_OK_ANSWER, (i + 1) + "|" + answer);
                            communication.broadcast(msg,broadCasterSocket);
                            isProtocolDone = false;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (isProtocolDone && !ProtocolDone[okProcNumber]) {
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


}
