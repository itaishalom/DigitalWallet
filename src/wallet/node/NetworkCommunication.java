package wallet.node;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

/**
 * Created by Itai on 21/03/2018.
 */
public class NetworkCommunication {

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

     void broadcast(Message broadcastMessage ) {
        List<InetAddress> broadcastList = null;
          //   System.out.println("Broadcast: " + broadcastMessage);
        try {
            broadcastList = listAllBroadcastAddresses();

            if (broadcastList != null && broadcastList.size() > 0) {
                InetAddress address = broadcastList.get(0);
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                byte[] buffer = broadcastMessage.toString().getBytes();

                DatagramPacket packet
                        = new DatagramPacket(buffer, 0, buffer.length, address, 4445);

                socket.send(packet);
                //   socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<InetAddress> listAllBroadcastAddresses() throws SocketException {
        List<InetAddress> broadcastList = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces
                = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            networkInterface.getInterfaceAddresses().stream()
                    .map(InterfaceAddress::getBroadcast)
                    .filter(Objects::nonNull)
                    .forEach(broadcastList::add);
        }
        return broadcastList;
    }


}
