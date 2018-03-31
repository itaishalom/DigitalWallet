package wallet.node;//###############

import wallet.Wallet;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

import static test.Main.RUN_FAULT_NODE;
import static wallet.node.Functions.*;
import static wallet.node.Message.*;

/**
 * This is the MyServerObject. This object holds the ip and port
 * for every server known to the file manager
 *
 * @author itaishalom
 */
public class Node {
    private int mPortInput;
    int mNumber;
    //  static DatagramSocket broadCasterSocket = null;
    Node[] mAllNodes;
    int[] mOkNumber;
    private int[] mOk2Number;
    int[] mComplaintNumber;
    int[] mComplaintResponseNumber;
    private String[][] values1;
    private String[][] values2;
    private boolean[] valuesAreReady;
    private int[] mCompareNumbers;
    private ConfirmValues[] confirmValuesThread;
    protected boolean[] mIOk;
    int mFaults;
    private WaitForOk[] waitForOks;
    private boolean[] haveIFinished;
    NetworkCommunication communication;
    int mNumberOfValues;
    boolean[] ProtocolDone;
    private String[] g_values;
    private int numOfGValues;
    private int mClientPort;
    private Thread waitForGsTread;

    public Node(int num, int port, int faultsNumber, int clientPort) {
        mClientPort = clientPort;
        haveIFinished = new boolean[TOTAL_PROCESS_VALUES];
        valuesAreReady = new boolean[TOTAL_PROCESS_VALUES];
        ProtocolDone = new boolean[TOTAL_PROCESS_VALUES];
        values1 = new String[TOTAL_PROCESS_VALUES][];
        values2 = new String[TOTAL_PROCESS_VALUES][];
        confirmValuesThread = new ConfirmValues[TOTAL_PROCESS_VALUES];
        waitForOks = new WaitForOk[TOTAL_PROCESS_VALUES];
        mComplaintNumber = new int[TOTAL_PROCESS_VALUES];
        mOkNumber = new int[TOTAL_PROCESS_VALUES];
        mOk2Number = new int[TOTAL_PROCESS_VALUES];
        mComplaintResponseNumber = new int[TOTAL_PROCESS_VALUES];
        mIOk = new boolean[TOTAL_PROCESS_VALUES];
        mCompareNumbers = new int[TOTAL_PROCESS_VALUES];
        mPortInput = port;
        mNumber = num;
        Thread listner = new NodeServerListener();
        listner.start();
        startBroadcastReceiver();
        mFaults = faultsNumber;
        mNumberOfValues = (mFaults * 3) + 1;
        communication = new NetworkCommunication();
        g_values = new String[mNumberOfValues];
        numOfGValues = 0;
    }


    protected void refresh(int process) {
        haveIFinished[process] = false;
        valuesAreReady[process] = false;
        ProtocolDone[process] = false;
        values1[process] = null;
        values2[process] = null;

        numOfGValues = 0;

        try {
            if (confirmValuesThread[process] != null)
                confirmValuesThread[process].join();
            if (waitForOks[process] != null)
                waitForOks[process].join();

            if (waitForGsTread != null) {
                waitForGsTread.join();
                waitForGsTread = null;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        confirmValuesThread[process] = null;
        waitForOks[process] = null;
        mComplaintNumber[process] = 0;
        mOkNumber[process] = 0;
        mOk2Number[process] = 0;
        mComplaintResponseNumber[process] = 0;
        mIOk[process] = false;
        mCompareNumbers[process] = 0;
        if (process == KEY_TAG) {
            g_values = new String[mNumberOfValues];
            numOfGValues = 0;
        }
    }

    public int getPort() {
        return this.mPortInput;
    }

    protected void startBroadcastReceiver() {
        Thread broadcastReceiver = new Thread(new BroadcastReceiver());
        broadcastReceiver.start();
    }

    public void calculateG() {
        long key = Math.round(Functions.predict(values1[KEY], mFaults, 0));
        long key2 = Math.round(Functions.predict(values1[KEY_TAG], mFaults, 0));
        long randPloy = Math.round(Functions.predict(values1[RANDOM_VALUES], mFaults, 0));
        long g_value = randPloy * (key - key2);
        g_values[mNumber - 1] = String.valueOf(g_value);
        numOfGValues++;
        String info = String.valueOf(g_value);
        Message msg = new Message(mNumber, KEY_TAG, BROADCAST, G_VALUES, info);
        communication.broadcast(msg);
    }

    public void setNodes(Node[] nodes) {
        mAllNodes = nodes;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && Node.class.isAssignableFrom(obj.getClass()) && ((((Node) obj).mPortInput) == this.mPortInput);
    }

    public class BroadcastReceiver implements Runnable {
        public void shutdown() {
            running = false;
        }

        protected DatagramSocket socket;
        protected boolean running = true;
        protected byte[] buf = new byte[1024];
        protected Message msg;

        protected BroadcastReceiver() {
            try {
                socket = new MulticastSocket(4445);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        protected Message getMessageFromBroadcast() {
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String info = new String(packet.getData(), 0, packet.getLength());
            msg = new Message(info);
            return msg;
        }

        public void run() {

            while (running) {
                Message msg = getMessageFromBroadcast();
                if (!running)
                    return;
                if (msg.getmFrom() == mNumber)
                    continue;
                switch (msg.getmSubType()) {
                    case COMPLAINT: {
                        mComplaintNumber[msg.getProcessType()]++;
                        continue;
                    }
                    case OK: {
                        mOkNumber[msg.getProcessType()]++;
                        print(mNumber + " oks: " + mOkNumber[msg.getProcessType()] + " mIOk? " + mIOk[msg.getProcessType()]);
                        if (mOkNumber[msg.getProcessType()] == mNumberOfValues - 1 && mIOk[msg.getProcessType()] && !ProtocolDone[msg.getProcessType()]) {

                            printResults(msg.getProcessType(), 1);
                            ProtocolDone[msg.getProcessType()] = true;
                            Message notifyEnd = new Message(mNumber, msg.getProcessType(), BROADCAST, PROTOCOL_COMPLETE, "1");
                            communication.broadcast(notifyEnd);
                            if (msg.getProcessType() == KEY_TAG)
                                calculateG();
                        }

                        break;
                    }
                    case REFRESH:
                        refresh(msg.getProcessType());
                        break;
                    case OK2: {
                        mOk2Number[msg.getProcessType()]++;
                        break;
                    }
                    case COMPLAINT_ANSWER: {
                        mComplaintResponseNumber[msg.getProcessType()]++;
                        String theInfo = msg.getmInfo();
                        String[] splitData = theInfo.split("\\|");
                        String[] numOfNodes = splitData[0].split(",");
                        String[] newVals = splitData[1].split(",");
                        String s_i_j = (newVals[0]);
                        String s_j_i = (newVals[1]);
                        int i = Integer.parseInt(numOfNodes[0]);
                        int j = Integer.parseInt(numOfNodes[1]);
                        if (mNumber == i) {
                            print("Setting straight the values");
                            values1[msg.getProcessType()][j - 1] = s_i_j;
                            values2[msg.getProcessType()][j - 1] = s_j_i;
                        }
                        if (mNumber == j) {
                            mComplaintNumber[msg.getProcessType()]--; // J larger than I and only I adds ++ to his complaint number
                            print("Setting straight the values");
                            values2[msg.getProcessType()][i - 1] = s_i_j;
                            values1[msg.getProcessType()][i - 1] = s_j_i;
                            if(!values1[msg.getProcessType()][i - 1].equals(s_j_i)){
                            }
                            if(!values2[msg.getProcessType()][i - 1].equals( s_i_j)){
                            }
                        }

                        break;
                    }
                    case NO_OK_ANSWER: {
                        if (haveIFinished[msg.getProcessType()])
                            return;
                        if (mIOk[msg.getProcessType()]) {         // Condition 1
                            String theInfo = msg.getmInfo();
                            String[] splitData = theInfo.split("\\|");
                            int theNode = Integer.parseInt(splitData[0]);
                            String[] newValsRow = splitData[1].split(",");
                            String[] newValsCol = splitData[2].split(",");
                            newValsRow = interpolate(newValsRow, mFaults, false, true);
                            newValsCol = interpolate(newValsCol, mFaults, false, true);
                            if (newValsCol != null && newValsRow != null) {     // Condition 2
                                //Set straight the values
                                values1[msg.getProcessType()][theNode - 1] = newValsCol[mNumber - 1];
                                values2[msg.getProcessType()][theNode - 1] = newValsRow[mNumber - 1];
                                interpolate(values1[msg.getProcessType()], mFaults, false, false);
                                interpolate(values2[msg.getProcessType()], mFaults, false, false);
                                boolean isInterpolateGood = false;
                                for (int i = 0; i < mNumberOfValues; i++) {
                                    isInterpolateGood = isInterpolateGood || !values1[msg.getProcessType()][i].equals("0");
                                }
                                if (isInterpolateGood) {
                                    isInterpolateGood = false;
                                    for (int i = 0; i < mNumberOfValues; i++) {
                                        isInterpolateGood = isInterpolateGood || !values2[msg.getProcessType()][i].equals("0");
                                    }
                                    if (isInterpolateGood) {
                                        Message ok2Broadcast = new Message(mNumber, msg.getProcessType(), BROADCAST, OK2, "done");
                                        communication.broadcast(ok2Broadcast);
                                        try {

                                            waitForOks[msg.getProcessType()].join();
                                            if (haveIFinished[msg.getProcessType()])
                                                return;
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        if (waitForOks[msg.getProcessType()].getRound() != 2 && !haveIFinished[msg.getProcessType()]) {
                                            waitForOks[msg.getProcessType()] = new WaitForOk(msg.getProcessType(), 2);
                                            waitForOks[msg.getProcessType()].start();
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case G_VALUES: {
                        g_values[msg.getmFrom() - 1] = msg.getmInfo();
                        numOfGValues++;
                        if (waitForGsTread == null) { //starts a new thread
                            waitForGsTread = new WaitForGsToCalculateInZero();
                            waitForGsTread.start();
                        }
                    }
                }
            }
        }
    }


    public class NodeServerListener extends Thread {
        private int DefaultTimeout = 5000;

        @Override
        public void run() {
            try {
                Socket socket;
                ServerSocket serverSocket;
                serverSocket = new ServerSocket(mPortInput);
                //      System.out.println("Open listener at port : " + mPortInput + " node: " + mNumber);
                serverSocket.setSoTimeout(DefaultTimeout);
                while (true) {
                    try {
                        socket = serverSocket.accept();
                    } catch (SocketTimeoutException st) {
                        continue;
                    }
                    startSession(socket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    protected void startSession(Socket socket) {
        Thread a = new NodeIncomeDataHandler(socket);//, listening, allSocks);    //The session class gets the connected socket to handle
        a.start();    //If true, start the session
    }


    public class WaitForGsToCalculateInZero extends Thread {
        int attemptNumbers = 0;
        int TOTAL_ATTEMPTS = 5;
        int mProcessType = -1;


        @Override
        public void run() {
            //Assume all the process is done
            while (numOfGValues < mNumberOfValues - mFaults) {
                attemptNumbers++;
                if (attemptNumbers == TOTAL_ATTEMPTS) {
                    for (int i = 0; i < mNumberOfValues; i++) {
                        values1[KEY_TAG][i] = "0";
                        values2[KEY_TAG][i] = "0";
                    }
                    System.out.println("Not enough G's to restore");
                    return;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Double res = interpolateRobust(g_values, 2 * mFaults, 0, mNumberOfValues - mFaults);
            if (res == null) {
                print("Node " + mNumber + " failed to reconstruct G polynomial");
                return;
            }
            int result = (int) Math.round(res);
            if (result != 0) {
                print(("G robust interpolation failed, result equlas " + result + " in node " + mNumber));
                // System.out.println(Arrays.asList(g_values));
            } else {
                long value = Math.round(Functions.predict(values1[VALUE], mFaults, 0));
                Message msg = new Message(mNumber, KEY_TAG, PRIVATE, Qv_VALUE, String.valueOf(value));
                communication.sendMessageToNode(mClientPort, msg);
            }
        }
    }

    public class ConfirmValues extends Thread {
        int attemptNumbers = 0;
        int TOTAL_ATTEMPTS = 3;
        int mProcessType = -1;


        ConfirmValues(int processType) {
            mProcessType = processType;
        }

        @Override
        public void run() {
            //Assume all the process is done
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (mComplaintNumber[mProcessType] != mComplaintResponseNumber[mProcessType]) {
                try {
                    attemptNumbers++;
                    if (attemptNumbers == TOTAL_ATTEMPTS) {
                        for (int i = 0; i < mNumberOfValues; i++) {
                            values1[mProcessType][i] = "0";
                            values2[mProcessType][i] = "0";
                        }
                        print(mNumber + " There are " + mComplaintNumber[mProcessType] + " complaints and only " +
                                mComplaintResponseNumber[mProcessType] + " responses - protocol failed");
                        return; // Or send faile
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            print("Complaints = " + mComplaintNumber[mProcessType] + " and answers = " + mComplaintResponseNumber[mProcessType]);
            values1[mProcessType] = interpolate(values1[mProcessType], mFaults, false, false);
            values2[mProcessType] = interpolate(values2[mProcessType], mFaults, false, false);
            boolean confirmGoodValues = false;
            for (int i = 0; i < mNumberOfValues; i++) {
                confirmGoodValues = confirmGoodValues || !values1[mProcessType][i].equals("0");
            }
            if (!confirmGoodValues) {
                print("Node " + mNumber + " is out - failed to confirm values1 in process " + Message.getProcessFromNumber(mProcessType));
                return;
            }
            print("Confirmed 1: " + Message.getProcessFromNumber(mProcessType));
            confirmGoodValues = false;
            for (int j = 0; j < mNumberOfValues; j++) {
                confirmGoodValues = confirmGoodValues || !values2[mProcessType][j].equals("0");
            }
            if (!confirmGoodValues) {
                System.out.println("Node " + mNumber + " is out - failed to confirm values2");
                return;
            }
            print("Confirmed 2: " + Message.getProcessFromNumber(mProcessType));
            if (!mIOk[mProcessType]) {
                Message msg = new Message(mNumber, mProcessType, BROADCAST, OK, "DONE");
                mIOk[msg.getProcessType()] = true;
                communication.broadcast(msg);

                waitForOks[msg.getProcessType()] = new WaitForOk(msg.getProcessType(), 1);
                waitForOks[msg.getProcessType()].start();
            }
        }
    }


    public void print(String s) {
        //    if (mNumber == 1)
     //  System.out.println("this: " + this.mNumber + ": " + s);
    }

    public class WaitForOk extends Thread {
        int attemptNumbers = 0;
        int TOTAL_ATTEMPTS = 7;
        int okProcNumber;
        int okRound;

        public int getProcessType() {
            return okProcNumber;
        }

        public WaitForOk(int processType, int OkNumber) {
            okProcNumber = processType;
            okRound = OkNumber;
        }

        int getRound() {
            return okRound;
        }

        @Override
        public void run() {
            //Assume all the process is done
            int[] checkOkArray;
            if (okRound == 1)
                checkOkArray = mOkNumber;
            else
                checkOkArray = mOk2Number;
            while (checkOkArray[okProcNumber] + 1 < mNumberOfValues - mFaults - 1) {
                if (ProtocolDone[okProcNumber])
                    return;
                try {
                    attemptNumbers++;
                    if (attemptNumbers == TOTAL_ATTEMPTS) {
                        for (int i = 0; i < mNumberOfValues; i++) {
                            values1[okProcNumber][i] = "0";
                            values2[okProcNumber][i] = "0";
                        }
                        System.out.println(mNumber + " Failed to save values in round " + okRound);
                        haveIFinished[okProcNumber] = true;
                        return; // Or send faile
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (okRound == 2) {

                printResults(okProcNumber, okRound);
                haveIFinished[okProcNumber] = true;
                ProtocolDone[okProcNumber] = true;
                Message notifyEnd = new Message(mNumber, okProcNumber, BROADCAST, PROTOCOL_COMPLETE, String.valueOf(okRound));
                communication.broadcast(notifyEnd);
                if (okProcNumber == KEY_TAG)
                    calculateG();
                // System.out.println(Arrays.toString(values1[mProcessType]));
                //   System.out.println(Arrays.toString(values2[mProcessType]));
            }
        }

    }

    public void printResults(int processType, int round) {
       /* if (values1[processType] != null) {
            String processName = getProcessFromNumber(processType);
            System.out.println(mNumber + " Saved Values on round: " + round + " on process " + processName + " \n"
                    + "va1: " + Arrays.asList(values1[processType]) + "\n"
                    + "va2: " + Arrays.asList(values2[processType]));
        }*/
    }


    public class NodeIncomeDataHandler extends Thread {
        Socket mSocket;

        private NodeIncomeDataHandler(Socket incomeSocket) {
            mSocket = incomeSocket;
        }

        @Override
        public void run() {
            try {
                InputStream is = mSocket.getInputStream();
                byte[] buffer = new byte[1024];
                int read;
                Message msg = null;
                read = is.read(buffer);
                String output = new String(buffer, 0, read);
                print("To: " + mNumber + " from " + output);
                System.out.flush();
                msg = new Message(output);
                mSocket.close();
                if (msg.isPrivate()) {
                    if (msg.isValues()) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        handleInitialValues(msg);
                    }
                    if (msg.isCompare()) {
                        mCompareNumbers[msg.getProcessType()]++;
                        handleCompares(msg);
                        if (confirmValuesThread[msg.getProcessType()] == null) {
                            confirmValuesThread[msg.getProcessType()] = new ConfirmValues(msg.getProcessType());
                            confirmValuesThread[msg.getProcessType()].start();
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * First time that the node receives data from the dealer
         *
         * @param info
         */
        private void handleInitialValues(Message info) {
            valuesAreReady[info.getProcessType()] = false;
            String[] parts = info.getmInfo().split("\\|");
            String[] val1 = parts[0].split(",");
            if (mNumber == 1 && info.getProcessType() == RANDOM_VALUES && RUN_FAULT_NODE)
                val1[0] = "1123"; // fuck node 1

            String[] val2 = parts[1].split(",");


            values1[info.getProcessType()] = interpolate(val1, mFaults, false, false);
            values2[info.getProcessType()] = interpolate(val2, mFaults, false, false);
            valuesAreReady[info.getProcessType()] = true;
            for (Node node : mAllNodes) {
                if (node.mNumber == mNumber)
                    continue;
                String toSend = values1[info.getProcessType()][node.mNumber - 1] + "|" + values2[info.getProcessType()][node.mNumber - 1];
                print("sending from " + mNumber + " to " + node.mNumber + ": " + toSend);
                Message msg = new Message(mNumber, info.getProcessType(), PRIVATE, COMPARE, toSend);
                communication.sendMessageToNode(node.getPort(), msg);
            }
        }

        private void handleCompares(Message compareMsg) {
            String info = compareMsg.getmInfo();
            int from = compareMsg.getmFrom();
            String[] parts = info.split("\\|");
            while (!valuesAreReady[compareMsg.getProcessType()]) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (values1[compareMsg.getProcessType()][from - 1].equals(parts[1]) && values2[compareMsg.getProcessType()][from - 1].equals(parts[0])) {
                //   System.out.println("all good");
            } else {        // Should send complaint
                mComplaintNumber[compareMsg.getProcessType()]++;
                if (from > mNumber) {

                    Message msg = new Message(mNumber, compareMsg.getProcessType(), BROADCAST, COMPLAINT, mNumber + "|" + from);
                    print("I am " + mNumber + " And I send broadcast");
                    communication.broadcast(msg);

                }
            }
        }
    }
}
