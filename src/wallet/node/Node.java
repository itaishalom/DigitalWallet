package wallet.node;//###############
// FILE : Server.java
// WRITER : Itai Shalom, itaishalom, 301371696 
// EXERCISE : oop ex3 2011
// DESCRIPTION: The MyServer object.
//###############

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Arrays;

import static wallet.node.Functions.broadcast;
import static wallet.node.Functions.interpolate;
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
    static DatagramSocket broadCasterSocket = null;
    protected Node[] mAllNodes;
    protected int[] mOkNumber;
    protected int[] mOk2Number;
    protected int[] mComplaintNumber;
    protected int[] mComplaintResponseNumber;
    private String[][] values1;
    private String[][] values2;
    private boolean[] valuesAreReady;
    private int[] mCompareNumbers;
    private ConfirmValues[] confirmValuesThread;
    private boolean[] mIOk;
    private int mFaults;
    private WaitForOk[] waitForOks;
    private boolean[] haveIFinished ;
    NetworkCommunication communication;
    protected int mNumberOfValues;
    protected boolean[] ProtocolDone;


    public Node(int num, int port, int faultsNumber) {
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
    }

    public int getPort() {
        return this.mPortInput;
    }

    protected void startBroadcastReceiver() {
        Thread broadcastReceiver = new Thread(new BroadcastReceiver());
        broadcastReceiver.start();
    }

    public void calculateG() {
        double key = (int) Functions.predict(values1[KEY], mFaults, 0);
        double key2 =(int) Functions.predict(values1[KEY_TAG], mFaults, 0);
        double randPloy =(int) Functions.predict(values1[RANDOM_VALUES], mFaults, 0);
        String info = String.valueOf(randPloy*(key-key2));
        Message msg = new Message(mNumber,KEY_TAG,BROADCAST,G_VALUES,info);
        broadcast(msg,broadCasterSocket);
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
        protected boolean running =true;
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
                if (msg.getmFrom() == mNumber)
                    continue;
                print("I am " + mNumber + " and I got " + msg.toString());
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
                                broadcast(notifyEnd, broadCasterSocket);
                                calculateG();
                            }

                        break;
                    }
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
                            mComplaintNumber[msg.getProcessType()]--;
                        }
                        if (mNumber == j) {
                            mComplaintNumber[msg.getProcessType()]--;
                            print("Setting straight the values");
                            values2[msg.getProcessType()][i - 1] = s_i_j;
                            values1[msg.getProcessType()][i - 1] = s_j_i;
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
                                        broadcast(ok2Broadcast, socket);
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
                serverSocket.setSoTimeout(DefaultTimeout);
                Thread a;
                while (true) {
                    try {
                        socket = serverSocket.accept();
                    } catch (SocketTimeoutException st) {
                        continue;
                    }
                    a = new NodeIncomeDataHandler(socket);//, listening, allSocks);    //The session class gets the connected socket to handle
                    a.start();    //If true, start the session

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public class ConfirmValues extends Thread {
        int attemptNumbers = 0;
        int TOTAL_ATTEMPTS = 3;
        int mProcessType = -1;

        public ConfirmValues(int processType) {
            mProcessType = processType;
        }

        @Override
        public void run() {
            //Assume all the process is done
            while (mComplaintNumber[mProcessType] < mComplaintResponseNumber[mProcessType]) {
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
                    System.out.println(mNumber + " not enough complaint answers");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            values1[mProcessType] = interpolate(values1[mProcessType], mFaults, false, false);
            values2[mProcessType] = interpolate(values2[mProcessType], mFaults, false, false);
            boolean confirmGoodValues = false;
            for (int i = 0; i < mNumberOfValues; i++) {
                confirmGoodValues = confirmGoodValues || !values1[mProcessType][i].equals("0");
            }
            if (!confirmGoodValues) {
                System.out.println("Node " + mNumber + " is out");
                return;
            }
            confirmGoodValues = false;
            for (int j = 0; j < mNumberOfValues; j++) {
                confirmGoodValues = confirmGoodValues || !values2[mProcessType][j].equals("0");
            }
            if (!confirmGoodValues) {
                System.out.println("Node " + mNumber + " is out");
                return;
            }
            if(!mIOk[mProcessType]) {
                Message msg = new Message(mNumber, mProcessType, BROADCAST, OK, "DONE");
                mIOk[msg.getProcessType()] = true;
                broadcast(msg, broadCasterSocket);


                waitForOks[msg.getProcessType()] = new WaitForOk(msg.getProcessType(), 1);
                waitForOks[msg.getProcessType()].start();
            }
        }
    }


    public void print(String s) {
    //    if (mNumber == 1)
            System.out.println(s);
    }

    public class WaitForOk extends Thread {
        int attemptNumbers = 0;
        int TOTAL_ATTEMPTS = 5;
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
        /*    if (okRound == 1) {
                if (mOkNumber[okProcNumber] == mNumberOfValues - 1 && mIOk[okProcNumber] && !ProtocolDone[okProcNumber]) {
                    if(mNumber == 4){
                        System.out.println("c");
                    }
                    printResults(okProcNumber, okRound);
                    ProtocolDone[okProcNumber] = true;
                    haveIFinished[okProcNumber] = true;
                    Message notifyEnd = new Message(mNumber, okProcNumber, BROADCAST, PROTOCOL_COMPLETE, String.valueOf(okRound));
                    broadcast(notifyEnd, broadCasterSocket);
                    return;
                }
            }*/
            if (okRound == 2) {

                printResults(okProcNumber, okRound);
                haveIFinished[okProcNumber] = true;
                ProtocolDone[okProcNumber] = true;
                Message notifyEnd = new Message(mNumber, okProcNumber, BROADCAST, PROTOCOL_COMPLETE, String.valueOf(okRound));
                broadcast(notifyEnd, broadCasterSocket);
                calculateG();
                // System.out.println(Arrays.toString(values1[mProcessType]));
                //   System.out.println(Arrays.toString(values2[mProcessType]));
            }
        }

    }

    public void printResults(int processType, int round) {
        String processName = getProcessFromNumber(processType);
        System.out.println(mNumber + " Saved Values on round: " + round + " on process " + processName + " \n"
                + "va1: " + Arrays.asList(values1[processType]) + "\n"
                + "va2: " + Arrays.asList(values2[processType]));
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
                        handleInitialValues(msg);
                        confirmValuesThread[msg.getProcessType()] = null;
                    }
                    if (msg.isCompare()) {
                        mCompareNumbers[msg.getProcessType()]++;
                        handleCompares(msg);
                        int waitForCompares = (mNumberOfValues) - 1;
                        if (waitForCompares == mCompareNumbers[msg.getProcessType()]) {
                            if (confirmValuesThread[msg.getProcessType()] == null) {
                                confirmValuesThread[msg.getProcessType()] = new ConfirmValues(msg.getProcessType());
                                confirmValuesThread[msg.getProcessType()].start();
                            }
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
          /*  if (mNumber == 1 && info.getProcessType() ==1)
                val1[0] = "1123"; // fuck node 1*/
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
                    broadcast(msg, broadCasterSocket);
                }
            }
        }
    }
}
