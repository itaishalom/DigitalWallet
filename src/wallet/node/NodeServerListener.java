package wallet.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Created by Itai on 27/02/2018.
 */
public class NodeServerListener extends Thread {
    int mPort;
    int DefaultTimeout = 5000;

    NodeServerListener(int port) {
        mPort = port;
    }

    @Override
    public void run() {
        try {
            Socket socket = null;
            ServerSocket serverSocket = null;

            serverSocket = new ServerSocket(mPort);

            serverSocket.setSoTimeout(DefaultTimeout);
            int counter = 0;
            Thread a = null;
            while (true) {
                counter++;
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
