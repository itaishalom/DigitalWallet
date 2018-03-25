package wallet.node;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import static wallet.node.Functions.broadcast;
import static wallet.node.Functions.interpolateRobust;
import static wallet.node.Message.*;
import static wallet.node.Message.COMPLAINT_ANSWER;
import static wallet.node.Message.OK;

/**
 * Created by Itai on 24/03/2018.
 */
public class Client extends Dealer {
    private String[] QvValues;
    private int qValuesCounter = 0;
    public int reconstuctValue;
    public boolean processDone = false;
    private CalculateQ_V calculateQ_v_thread;

    public Client(int num, int port, int f) {
        super(num, port, f, port);
        QvValues = new String[(3 * f) + 1];
    }

/*
    @Override
    protected void startBroadcastReceiver() {
  */
/*      container = new BroadcastReceiverClient();
        broadcastReceiver = new Thread(container);
        broadcastReceiver.start();*//*

    }
*/


    public void startProcess(int key) {
        //     container = new BroadcastReceiverClient();
        broadcastReceiver = new Thread(container);
        broadcastReceiver.start();
        System.out.println("#############  Begin retrieve key #############");

      /*  Message msg = new Message(mNumber,2,BROADCAST,OK,"shit");
        broadcast(msg,broadCasterSocket);*/

        q[0] = createArrayOfCoefs();
        q[0][0] = mRandom.nextInt(boundForRandom);
        ; // decide what to do
        p[0] = createArrayOfCoefs();
        p[0][0] = mRandom.nextInt(boundForRandom);
        for (Node node_i : mAllNodes) {  // Iterate over all Nodes
            calculateAndPrivateSendValues(node_i.mNumber, node_i.getPort(), KEY, RANDOM_VALUES);
        }
        waitForProcessEnd wait = new waitForProcessEnd(key, KEY_TAG, RANDOM_VALUES, "Sending key' bi-polynomial");
        wait.start();
    }

    public int getValue() {
        int attempts = 0;
        int totalAttepmpts = 5;
        while (!processDone) {
            try {
                attempts++;
                if (attempts == totalAttepmpts) {
                    System.out.println("key is incorrect, returning 0");
                    return 0;
                }
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return reconstuctValue;
    }


    @Override
    protected void startSession(Socket socket) {
        Thread a = new CleintIncomeDataHandler(socket);//, listening, allSocks);    //The session class gets the connected socket to handle
        a.start();    //If true, start the session
    }

    public class CleintIncomeDataHandler extends Thread {
        Socket mSocket;

        private CleintIncomeDataHandler(Socket incomeSocket) {
            mSocket = incomeSocket;
        }

        @Override
        public void run() {
            try {
                if (processDone) {
                    return;
                }
                InputStream is = mSocket.getInputStream();
                byte[] buffer = new byte[1024];
                int read;
                Message msg = null;
                read = is.read(buffer);
                String output = new String(buffer, 0, read);
                // print("To: " + mNumber + " from " + output);
                System.out.flush();
                msg = new Message(output);
                System.out.println("Client got: " + msg);
                if (msg.getmSubType().equals(Qv_VALUE)) {
                    QvValues[msg.getmFrom() - 1] = msg.getmInfo();
                    qValuesCounter++;
                    if (calculateQ_v_thread == null) {
                        calculateQ_v_thread = new CalculateQ_V();
                        calculateQ_v_thread.start();
                    }
                }
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class CalculateQ_V extends Thread {
        int attempts = 0;
        int totalAttepmpts = 5;

        @Override
        public void run() {
            try {
                Thread.sleep(3000);

                while (qValuesCounter < mNumberOfValues - mFaults) {
                    Thread.sleep(3000);
                    attempts++;
                    if (attempts == totalAttepmpts) {
                        System.out.println("Can restore, not enough Q_V values");
                        processDone = true;
                        return;
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Double val = interpolateRobust(QvValues, mFaults, 0, mNumberOfValues - mFaults);
            if (val != null)
                reconstuctValue = (int) Math.round(val);
            processDone = true;
        }
    }
}


