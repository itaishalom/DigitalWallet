package wallet.node;

import static wallet.node.Functions.broadcast;
import static wallet.node.Message.*;
import static wallet.node.Message.COMPLAINT_ANSWER;
import static wallet.node.Message.OK;

/**
 * Created by Itai on 24/03/2018.
 */
public class Client extends Dealer {
    public Client(int num, int port, int f) {
        super(num, port, f);
    }

    @Override
    protected void startBroadcastReceiver() {
  /*      container = new BroadcastReceiverClient();
        broadcastReceiver = new Thread(container);
        broadcastReceiver.start();*/
    }


    public void startProcess(int key) {
        container = new BroadcastReceiverClient();
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
            calculateAndPrivateSendValues(node_i.mNumber, node_i.getPort(), KEY, CLIENT_1);
        }
        waitForProcessEnd wait = new waitForProcessEnd(key, CLIENT_2, CLIENT_1, "Sending key' bi-polynomial");
        wait.start();
    }


    public class BroadcastReceiverClient extends BroadcastReceiver {

        BroadcastReceiverClient() {
            super();
        }

        @Override
        public void run() {
            running = true;
            while (running) {
                Message msg = getMessageFromBroadcast();
                switch (msg.getmSubType()) {

                    case PROTOCOL_COMPLETE:
                        ProtocolDone[msg.getProcessType()] = true;
                   /*     if (!ProtocolDone[msg.getProcessType()]) {
                            ProtocolDone[msg.getProcessType()] = true;
                            printResults(msg.getProcessType(), Integer.valueOf(msg.getmInfo()));*/
                        System.out.println("protocol done");

                        break;
                    case COMPLAINT: {
                        mComplaintNumber[msg.getProcessType()]++;
                        String data = msg.getmInfo();
                        String[] nodes = data.split("\\|");
                        int i = Integer.parseInt(nodes[0]);
                        int j = Integer.parseInt(nodes[1]);
                        if (j == mNumber)
                            mComplaintNumber[msg.getProcessType()]--;
                        long result1 = computePolynomial(q[msg.getProcessType()], i) * computePolynomial(p[msg.getProcessType()], j);
                        long result2 = computePolynomial(p[msg.getProcessType()], i) * computePolynomial(q[msg.getProcessType()], j);
                        Message newMsg = new Message(mNumber, msg.getProcessType(), BROADCAST, COMPLAINT_ANSWER, +i + "," + j + "|" + result1 + "," + result2);
                        broadcast(newMsg, broadCasterSocket);
                        break;
                    }
                    case OK: {
                        mOkNumber[msg.getProcessType()]++;
                        okCounter[msg.getProcessType()][msg.getmFrom() - 1] = true;
                        if (waitForOks[msg.getProcessType()] == null) {
                            waitForOks[msg.getProcessType()] = new WaitForOkDealer(msg.getProcessType());
                            waitForOks[msg.getProcessType()].start();
                        }
                        break;
                    }
                    case COMPLAINT_ANSWER: {
                        mComplaintResponseNumber[msg.getProcessType()]++;
                        break;
                    }
                }
            }
        }
    }

}
