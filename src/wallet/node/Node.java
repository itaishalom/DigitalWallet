package wallet.node;//###############
// FILE : Server.java
// WRITER : Itai Shalom, itaishalom, 301371696 
// EXERCISE : oop ex3 2011
// DESCRIPTION: The MyServer object.
//###############

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.*;

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
    private Node[] mAllNodes;
    private int[] mOkNumber;
    private int[] mComplaintNumber;
    private int[] mComplaintResponseNumber;
    //  private String[] values1;
    //  private String[] values2;
    private String[][] values1;
    private String[][] values2;
    private boolean valuesAreReady = false;
    private int[] mCompareNumbers;
    private ConfirmValues confirmValuesThread = null;
    private int[] mIOk;
    private int mFaults;
    int[][] listOfOkNodes;

    public Node(int num, int port, int faultsNumber) {
        values1 = new String[2][];
        values2 = new String[2][];

        mComplaintNumber = new int[2];
        mOkNumber = new int[2];
        mComplaintResponseNumber = new int[2];
        mIOk = new int[2];
        mCompareNumbers = new int[2];
        mPortInput = port;
        mNumber = num;
        Thread listner = new NodeServerListener();
        listner.start();
        startBroadcastReceiver();
        mFaults = faultsNumber;
    }

    public int getPort() {
        return this.mPortInput;
    }

    protected void startBroadcastReceiver() {
        Thread broadcastReceiver = new BroadcastReceiver();
        broadcastReceiver.start();
    }

    public void setNodes(Node[] nodes) {
        mAllNodes = nodes;
        listOfOkNodes = new int[2][mAllNodes.length];
    }


    public void sendMessageToNode(int portToSendTo, Message msg) {
        String testServerName = "localhost";
        try {
            Socket socket = openSocket(testServerName, portToSendTo);
            send(socket, msg.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void send(Socket socket, String writeTo) {
        try {
            // write text to the socket
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedWriter.write(writeTo);
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Open a socket connection to the given server on the given port.
     * This method currently sets the socket timeout value to 10 seconds.
     * (A second version of this method could allow the user to specify this timeout.)
     */
    private Socket openSocket(String server, int port) throws Exception {
        Socket socket;

        // create a socket with a timeout
        try {

            InetAddress inteAddress = InetAddress.getByName(server);
            SocketAddress socketAddress = new InetSocketAddress(inteAddress, port);

            // create a socket
            socket = new Socket();

            // this method will block no more than timeout ms.
            int timeoutInMs = 10 * 1000;   // 10 seconds
            socket.connect(socketAddress, timeoutInMs);

            return socket;
        } catch (SocketTimeoutException ste) {
            System.err.println("Timed out waiting for the socket.");
            ste.printStackTrace();
            throw ste;
        }
    }


    @Override
    public boolean equals(Object obj) {
        return obj != null && Node.class.isAssignableFrom(obj.getClass()) && ((((Node) obj).mPortInput) == this.mPortInput);
    }

    public class BroadcastReceiver extends Thread {

        protected DatagramSocket socket;
        protected boolean running;
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
            running = true;
            while (running) {
                Message msg = getMessageFromBroadcast();
                if (msg.getmFrom() == mNumber)
                    continue;
                System.out.println("I am " + mNumber + " and I got " + msg.toString());
                switch (msg.getmSubType()) {
                    case COMPLAINT: {
                        mComplaintNumber[msg.getProcessType()]++;
                        continue;

                    }
                    case OK: {
                        mOkNumber[msg.getProcessType()]++;
                        //   confirmValuesThread.join();
/*
                    if (values1 != null && values2 != null) {
                        int n = values1.length + 1;
                        int f = values1.length / 3;
                    }*/
                        break;
                    }

                    case COMPLAINT_ANSWER: {
                        if (msg.getmFrom() == 0) {
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
                                System.out.println("Setting straight the values");
                                values1[msg.getProcessType()][j - 1] = s_i_j;
                                values2[msg.getProcessType()][j - 1] = s_j_i;
                                mComplaintNumber[msg.getProcessType()]--;
                            }
                            if (mNumber == j) {
                                mComplaintNumber[msg.getProcessType()]--;
                                System.out.println("Setting straight the values");
                                values2[msg.getProcessType()][i - 1] = s_i_j;
                                values1[msg.getProcessType()][i - 1] = s_j_i;
                            }
                        }
                        break;
                    }
                    case NO_OK_ANSWER: {
                        if (mIOk[msg.getProcessType()] == 1) {         // Condition 1
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
                                for (int i = 0; i < values1[msg.getProcessType()].length; i++) {
                                    isInterpolateGood = isInterpolateGood || !values1[msg.getProcessType()][i].equals("0");
                                }
                                if (isInterpolateGood) {
                                    isInterpolateGood = false;
                                    for (int i = 0; i < values2[msg.getProcessType()].length; i++) {
                                        isInterpolateGood = isInterpolateGood || !values2[msg.getProcessType()][i].equals("0");
                                    }
                                    if(isInterpolateGood){
                                        Message ok2Broadcast = new Message(mNumber,msg.getProcessType(),BROADCAST,OK2,"done");
                                        broadcast(ok2Broadcast,socket);
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
        boolean isReady = false;
        int mProcessType = -1;

        public ConfirmValues(int processType) {
            mProcessType = processType;
        }

        @Override
        public void run() {
            //Assume all the process is done
            while (mComplaintNumber[mProcessType] - mComplaintResponseNumber[mProcessType] > 0) {
                try {
                    attemptNumbers++;
                    if (attemptNumbers == TOTAL_ATTEMPTS) {
                        for (int i = 0; i < values2.length; i++) {
                            values1[mProcessType][i] = "0";
                            values2[mProcessType][i] = "0";
                        }
                        return; // Or send faile
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            values1[mProcessType] = interpolate(values1[mProcessType], mFaults, false, false);
            values2[mProcessType] = interpolate(values2[mProcessType], mFaults, false, false);
            for (int i = 0; i < values1.length; i++) {
                if (!values1[mProcessType][i].equals("0") || !values2[mProcessType][i].equals("0")) {
                    Message msg = new Message(mNumber, mProcessType, BROADCAST, OK, "DONE");
                    broadcast(msg, broadCasterSocket);
                    mIOk[msg.getProcessType()] = 1;
                    WaitForOk waitForOk = new WaitForOk(msg.getProcessType());
                    waitForOk.run();
                    break;
                }
            }
        }
    }


    public class WaitForOk extends Thread {
        int attemptNumbers = 0;
        int TOTAL_ATTEMPTS = 3;
        int mProcessType = -1;
        int n;
        int f;

        public int getProcessType() {
            return mProcessType;
        }

        public WaitForOk(int processType) {
            mProcessType = processType;
            n = values1.length;
            f = values1.length / 3;
        }

        @Override
        public void run() {
            //Assume all the process is done

            while (mOkNumber[mProcessType] + mIOk[mProcessType] < n - f) {
                try {
                    attemptNumbers++;
                    if (attemptNumbers == TOTAL_ATTEMPTS) {
                        for (int i = 0; i < values2.length; i++) {
                            values1[mProcessType][i] = "0";
                            values2[mProcessType][i] = "0";
                            System.out.println(mNumber + " Failed to save values");
                        }
                        return; // Or send faile
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
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
                System.out.println("To: " + mNumber + " from " + output);
                System.out.flush();
                msg = new Message(output);

                mSocket.close();

                if (msg.isPrivate()) {
                    if (msg.isValues()) {
                        handleInitialValues(msg);
                    }
                    if (msg.isCompare()) {
                        mCompareNumbers[msg.getProcessType()]++;
                        handleCompares(msg);
                        int waitForCompares = (values1.length) - 1;
                        if (waitForCompares == mCompareNumbers[msg.getProcessType()]) {
                            if (confirmValuesThread == null) {
                                confirmValuesThread = new ConfirmValues(msg.getProcessType());
                                confirmValuesThread.run();
                            }
                        }
                    }
                } else {
                    System.out.println("Incoming Broadcast " + msg.toString());
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
            valuesAreReady = false;
            String[] parts = info.getmInfo().split("\\|");
            String[] val1 = parts[0].split(",");
            if (mNumber == 1)
                val1[0] = "1123"; // fuck node 1
            String[] val2 = parts[1].split(",");


            values1[info.getProcessType()] = interpolate(val1, mFaults, false, false);
            values2[info.getProcessType()] = interpolate(val2, mFaults, false, false);
            valuesAreReady = true;
            for (Node node : mAllNodes) {
                if (node.mNumber == mNumber)
                    continue;
                String toSend = values1[info.getProcessType()][node.mNumber - 1] + "|" + values2[info.getProcessType()][node.mNumber - 1];
                System.out.println("sending from " + mNumber + " to " + node.mNumber + ": " + toSend);
                Message msg = new Message(mNumber, info.getProcessType(), PRIVATE, COMPARE, toSend);
                sendMessageToNode(node.getPort(), msg);
            }
        }

        private void handleCompares(Message compareMsg) {
            String info = compareMsg.getmInfo();
            int from = compareMsg.getmFrom();
            String[] parts = info.split("\\|");
            while (!valuesAreReady) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (values1[compareMsg.getProcessType()][from - 1].equals(parts[1]) && values2[compareMsg.getProcessType()][from - 1].equals(parts[0])) {
                System.out.println("all good");
            } else {        // Should send complaint
                mComplaintNumber[compareMsg.getProcessType()]++;
                if (from > mNumber) {
                    Message msg = new Message(mNumber, compareMsg.getProcessType(), BROADCAST, COMPLAINT, mNumber + "|" + from);
                    System.out.println("I am " + mNumber + " And I send broadcast");
                    broadcast(msg, broadCasterSocket);
                }
            }
        }
    }
}
