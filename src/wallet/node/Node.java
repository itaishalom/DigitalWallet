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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

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
    private static DatagramSocket socket = null;
    private Node[] mAllNodes;


    private String[] values1;
    private String[] values2;
    private boolean valuesAreReady = false;

    public Node(int num, int port) {
        mPortInput = port;
        mNumber = num;
        Thread listner = new NodeServerListener();
        listner.start();
        startBroadcastReceiver();
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

    private void send(Socket socket, String writeTo) throws Exception {
        try {
            // write text to the socket
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedWriter.write(writeTo);
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }

    }

    public void broadcast(
            Message broadcastMessage) throws IOException {
        List<InetAddress> s = listAllBroadcastAddresses();
        if (s != null && s.size() > 0) {
            InetAddress address = s.get(0);
            socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] buffer = broadcastMessage.toString().getBytes();

            DatagramPacket packet
                    = new DatagramPacket(buffer, 0, buffer.length, address, 4445);

            socket.send(packet);
            //   socket.close();
        }
    }

    List<InetAddress> listAllBroadcastAddresses() throws SocketException {
        List<InetAddress> broadcastList = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces
                = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            networkInterface.getInterfaceAddresses().stream()
                    .map(a -> a.getBroadcast())
                    .filter(Objects::nonNull)
                    .forEach(broadcastList::add);
        }
        return broadcastList;
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
        if (obj == null) {
            return false;
        }
        if (!Node.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        return ((((Node) obj).mPortInput) == this.mPortInput);
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
                if (msg.isComplaintAnswer() && msg.getmFrom() == 0) {
                    System.out.println("Setting straight the values");
                    String theInfo = msg.getmInfo();
                    String[] splitData = theInfo.split("\\|");
                    String[] numOfNodes = splitData[0].split(",");
                    String[] newVals = splitData[1].split(",");
                    String s_i_j = (newVals[0]);
                    String s_j_i = (newVals[1]);
                    int i = Integer.parseInt(numOfNodes[0]);
                    int j = Integer.parseInt(numOfNodes[1]);
                    if (mNumber == i) {
                        values1[j-1] = s_i_j;
                        values2[j-1] = s_j_i;
                    }
                    if (mNumber == j) {
                        values2[i-1] = s_i_j;
                        values1[i-1] = s_j_i;
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
                        handleCompares(msg.getmInfo(), msg.getmFrom());
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


            values1 = interpolate(val1);
            values2 = interpolate(val2);
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
            } else {
                if (from > mNumber) {
                    Message msg = new Message(mNumber, BROADCAST, COMPLAINT, mNumber + "|" + from);

                    try {
                        System.out.println("I am " + mNumber + " And I send broadcast");
                        broadcast(msg);//, s.get(0));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private String[] interpolate(String[] vals) {
            double[] y = new double[vals.length];
            for (int i = 0; i < vals.length; i++) {
                y[i] = Double.parseDouble(vals[i]);
            }
            if (mNumber == 1)         // Fuck node 1
                y[1] += 2.0;
            //        y[y.length-1] = y[y.length-1] *3;
            double[] x = new double[vals.length];
            for (int i = 0; i < x.length; i++) {
                x[i] = i + 1;
            }
            int f = mAllNodes.length / 3;
            PolynomialRegression p = new PolynomialRegression(x, y, f);

            if (p.R2() != 1.0) {
                for (int i = 0; i < vals.length; i++) {
                    vals[i] = "0";
                }
                System.out.println("bad polynomial");
            } else {
                System.out.println("good polynomial");
            }
            return vals;
        }

    }
}
