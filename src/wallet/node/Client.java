package wallet.node;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import static wallet.node.Client.ProccesStatus.FAILED;
import static wallet.node.Functions.interpolateRobust;
import static wallet.node.Message.*;

/**
 * Created by Itai on 24/03/2018.
 */
public class Client extends Dealer {
    private boolean waitForQValuesStarted = false;
    private String[] QvValues;
    private int qValuesCounter = 0;
    public int reconstuctValue;
    public ProccesStatus processStatus;
    private CalculateQ_V calculateQ_v_thread;
    private boolean broadCastStarted = false;
    private boolean qValueArrived = false;
    waitForProcessEnd wait;
    public enum ProccesStatus {
        DONE,
        ACTIVE,
        FAILED,
    }

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


    public void killClientReceiver() {
        broadCastStarted = false;
        if (container != null)
            container.shutdown();
    }


    public void startProcess(int key) {
        processStatus = ProccesStatus.ACTIVE;
        //     container = new BroadcastReceiverClient();
        if (!broadCastStarted) {
            broadCastStarted = true;
            broadcastReceiver = new Thread(container);
            broadcastReceiver.start();
        }
        System.out.println("#############  Begin retrieve key #############");
        ProtocolDone[RANDOM_VALUES] = false;
        ProtocolDone[KEY_TAG] = false;
        okCounter[RANDOM_VALUES] = new boolean[(3 * mFaults) + 1];
        okCounter[KEY_TAG] = new boolean[(3 * mFaults) + 1];
        waitForQValuesStarted = false;
        qValueArrived = false;
        QvValues = new String[(3 * mFaults) + 1];
        qValuesCounter = 0;
        try {
            if (calculateQ_v_thread != null) {
                calculateQ_v_thread.join();
                calculateQ_v_thread = null;
            }
            if (waitForOks[KEY_TAG] != null) {
                waitForOks[KEY_TAG].join();
                waitForOks[KEY_TAG] = null;
            }
            if (waitForOks[RANDOM_VALUES] != null) {
                waitForOks[RANDOM_VALUES].join();
                waitForOks[RANDOM_VALUES] = null;
            }
            if(wait != null){
                wait.join();
                wait = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    /*  Message msg = new Message(mNumber,2,BROADCAST,OK,"shit");
      broadcast(msg,broadCasterSocket);*/
        sendRefresh(RANDOM_VALUES);
        q[0] = createArrayOfCoefs();
        q[0][0] = mRandom.nextInt(boundForRandom);
        ; // decide what to do
        p[0] = createArrayOfCoefs();

        p[0][0] = mRandom.nextInt(boundForRandom);
        for (Node node_i : mAllNodes) {  // Iterate over all Nodes
            calculateAndPrivateSendValues(node_i.mNumber, node_i.getPort(), KEY, RANDOM_VALUES);
        }

        wait = new waitForProcessEnd(key, KEY_TAG, RANDOM_VALUES, "Sending key' bi-polynomial");
        wait.start();
    }

    public int getValue() {
        while (processStatus == ProccesStatus.ACTIVE) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (processStatus == FAILED) {
            System.out.println("Invalid key");
            return -1;
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

            if (processStatus == ProccesStatus.ACTIVE) {
                try {
                    InputStream is = mSocket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int read;
                    Message msg = null;
                    read = is.read(buffer);
                    String output = new String(buffer, 0, read);
                    // print("To: " + mNumber + " from " + output);
                    System.out.flush();
                    msg = new Message(output);
                    print("Client got: " + msg);
                    if (msg.getmSubType().equals(Qv_VALUE)) {
                        qValueArrived = true;
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
    }

    @Override
    protected void waitForGValues() {
        if (!waitForQValuesStarted) {
            waitForQValuesStarted = true;
            try {
                Thread.sleep(3000);
                if (!qValueArrived) {
                    processStatus = FAILED;
                    print("No q value arrived - terminating the process");
                    return;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    protected int[] getQValue(int processNumber) {
        return q[processNumber - RANDOM_VALUES];
    }

    @Override
    protected int[] getPValue(int processNumber) {
        return p[processNumber - RANDOM_VALUES];
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
                        processStatus = FAILED;
                        return;
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Double val = interpolateRobust(QvValues, mFaults, 0, mNumberOfValues - mFaults);
            if (val != null) {
                reconstuctValue = (int) Math.round(val);
                processStatus = ProccesStatus.DONE;

            } else {
                processStatus = FAILED;
            }
        }
    }
}


