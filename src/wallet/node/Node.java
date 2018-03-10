package wallet.node;//###############
// FILE : Server.java
// WRITER : Itai Shalom, itaishalom, 301371696 
// EXERCISE : oop ex3 2011
// DESCRIPTION: The MyServer object.
//###############

import wallet.PolynomialRegression;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.List;

import static wallet.node.Functions.broadcast;
import static wallet.node.Functions.interpolate;
import static wallet.node.Functions.listAllBroadcastAddresses;
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
    protected static DatagramSocket broadCasterSocket = null;
    private Node[] mAllNodes;
    private int mOkNumber = 0;
    private int mComplaintNumber = 0;
    private int mComplaintResponseNumber = 0;
    private String[] values1;
    private String[] values2;
    private boolean valuesAreReady = false;
    private int mCompareNumbers = 0;
    private ConfirmValues confirmValuesThread = null;
    private  int mIOk = 0;
    private int mFaults;

    public Node(int num, int port, int faultsNumber) {
        mPortInput = port;
        mNumber = num;
        Thread listner = new NodeServerListener();
        listner.start();
        startBroadcastReceiver();
        mFaults = faultsNumber;
    }

    protected void startBroadcastReceiver() {
        Thread broadcastReceiver = new BroadcastReceiver();
        broadcastReceiver.start();
    }

    public void setNodes(Node[] nodes) {
        mAllNodes = nodes;
    }


    public void sendMessageToNode(Node node, Message msg) {
        String testServerName = "localhost";
        try {
            Socket socket = openSocket(testServerName, node.mPortInput);
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
                if (msg.isComplaint()) {
                    mComplaintNumber++;
                    continue;
                }
                if (msg.isOK()) {
                    mOkNumber++;
                    //   confirmValuesThread.join();
/*
                    if (values1 != null && values2 != null) {
                        int n = values1.length + 1;
                        int f = values1.length / 3;
                    }*/
                }
                System.out.println("I am " + mNumber + " and I got " + msg.toString());
                if (msg.isComplaintAnswer() && msg.getmFrom() == 0) {
                    mComplaintResponseNumber++;
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
                        values1[j - 1] = s_i_j;
                        values2[j - 1] = s_j_i;
                        mComplaintNumber--;
                    }
                    if (mNumber == j) {
                        mComplaintNumber--;
                        System.out.println("Setting straight the values");
                        values2[i - 1] = s_i_j;
                        values1[i - 1] = s_j_i;
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

        @Override
        public void run() {
            //Assume all the process is done
            while (mComplaintNumber - mComplaintResponseNumber > 0) {
                try {
                    attemptNumbers++;
                    if (attemptNumbers == TOTAL_ATTEMPTS) {
                        for (int i = 0; i < values2.length; i++) {
                            values1[i] = "0";
                            values2[i] = "0";
                        }
                        return; // Or send faile
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (mNumber == 1) {
                System.out.println("here");
            }
            values1 = interpolate(values1,mFaults,false);
            values2 = interpolate(values2,mFaults,false);
            for (int i = 0; i < values1.length; i++) {
                if (!values1[i].equals("0") || !values2[i].equals("0")) {
                    Message msg = new Message(mNumber, BROADCAST, OK, "DONE");
                    broadcast(msg,broadCasterSocket);
                    mIOk = 1;
                    WaitForOk waitForOk = new WaitForOk();
                    waitForOk.run();
                    break;
                }
            }
        }
    }


    public class WaitForOk extends Thread {
        int attemptNumbers = 0;
        int TOTAL_ATTEMPTS = 3;

        @Override
        public void run() {
            //Assume all the process is done
            int n = values1.length;
            int f = values1.length / 3;
            while (mOkNumber + mIOk < n - f) {
                try {
                    attemptNumbers++;
                    if (attemptNumbers == TOTAL_ATTEMPTS) {
                        for (int i = 0; i < values2.length; i++) {
                            values1[i] = "0";
                            values2[i] = "0";
                            System.out.println(mNumber + " Failed to save values");
                        }
                        return; // Or send faile
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(mNumber + " Success to save values");
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
                        handleInitialValues(msg.getmInfo());
                    }
                    if (msg.isCompare()) {
                        mCompareNumbers++;
                        handleCompares(msg.getmInfo(), msg.getmFrom());
                        int waitForCompares = (values1.length) - 1;
                        if (waitForCompares == mCompareNumbers) {
                            if (confirmValuesThread == null) {
                                confirmValuesThread = new ConfirmValues();
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


        private void handleInitialValues(String info) {
            valuesAreReady = false;
            String[] parts = info.split("\\|");
            String[] val1 = parts[0].split(",");

            String[] val2 = parts[1].split(",");



            values1 = interpolate(val1,mFaults,false);
            values2 = interpolate(val2,mFaults,false);
            valuesAreReady = true;
            for (Node node : mAllNodes) {
                if (node.mNumber == mNumber)
                    continue;
                String toSend = values1[node.mNumber - 1] + "|" + values2[node.mNumber - 1];
                System.out.println("sending from " + mNumber + " to " + node.mNumber + ": " + toSend);
                Message msg = new Message(mNumber, PRIVATE, COMPARE, toSend);
                sendMessageToNode(node, msg);
            }
        }

        private void handleCompares(String info, int from) {
            String[] parts = info.split("\\|");
            while (!valuesAreReady) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (values1[from - 1].equals(parts[1]) && values2[from - 1].equals(parts[0])) {
                System.out.println("all good");
            } else {        // Should send complaint
                mComplaintNumber++;
                if (from > mNumber) {
                    Message msg = new Message(mNumber, BROADCAST, COMPLAINT, mNumber + "|" + from);
                    System.out.println("I am " + mNumber + " And I send broadcast");
                    broadcast(msg,broadCasterSocket);
                }
            }
        }
    }
}
