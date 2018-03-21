package wallet.node;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.*;

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
}
