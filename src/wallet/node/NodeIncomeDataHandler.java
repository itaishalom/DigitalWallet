package wallet.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Created by Itai on 27/02/2018.
 */
public class NodeIncomeDataHandler extends Thread {
    Socket mSocket;
    public NodeIncomeDataHandler (Socket incomeSocket){
        mSocket = incomeSocket;
    }


    @Override
    public void run() {

    }

}
