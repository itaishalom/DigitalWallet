package wallet.node;//###############
// FILE : Server.java
// WRITER : Itai Shalom, itaishalom, 301371696 
// EXERCISE : oop ex3 2011
// DESCRIPTION: The MyServer object.
//###############

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunctionLagrangeForm;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import wallet.PolynomialRegression;

import static wallet.node.Message.COMPARE;
import static wallet.node.Message.PRIVATE;

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


    String[] values1;
    String[] values2;

    public Node(int num, int port) {
        mPortInput = port;
        mNumber = num;
        Thread listner = new NodeServerListener();
        listner.start();
        /*try {
            mOutputSocket = new Socket(InetAddress.getLocalHost().getHostAddress(),mPortOutput);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
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
/*    public void sendMessage(Node node){
        OutputStream outstream = null;
        try {
            outstream = mOutputSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        PrintWriter out = new PrintWriter(outstream);

        String toSend = "String to send";

        out.print(toSend );
    }*/

    public static void broadcast(
            String broadcastMessage, InetAddress address) throws IOException {
        socket = new DatagramSocket();
        socket.setBroadcast(true);

        byte[] buffer = broadcastMessage.getBytes();

        DatagramPacket packet
                = new DatagramPacket(buffer, buffer.length, address, 4445);
        socket.send(packet);
        socket.close();
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
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleInitialValues(String info) {
            String[] parts = info.split("\\|");
            values1 = parts[0].split(",");

            values2 = parts[1].split(",");
            interpolate(values1);
            interpolate(values2);
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
            while (values1 == null || values2 == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (values1[from - 1].equals(parts[1]) && values2[from - 1].equals(parts[0])) {
                System.out.println("all good");
            }
        }

        private void interpolate(String[] vals) {
            double[] y = new double[vals.length];
            for (int i = 0; i < vals.length; i++) {
                y[i] = Double.parseDouble(vals[i]);
            }
    //        y[y.length-1] = y[y.length-1] *3;
            double[] x = new double[vals.length];
            for (int i = 0; i < x.length; i++) {
                x[i] = i + 1;
            }
            int f = mAllNodes.length / 3;
            PolynomialRegression p = new PolynomialRegression(x,y,f);

            if (p.R2() <0.99) {
                System.out.println("bad polynomial");
            } else {
                System.out.println("good polynomial");
            }
        }

    }
}
