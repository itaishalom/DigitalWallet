package wallet;

import wallet.node.Client;
import wallet.node.Dealer;
import wallet.node.Node;


/**
 * Created by Itai on 26/02/2018.
 */
public class Wallet implements WalletInterface {
    private Node[] nodes;
    private Dealer dealer;
    private Client client;
    private int n;
    private int firstPort = 8090;
    private int mFaults;
    private boolean isDealerRegularNode = false;


    public Wallet(int f) {

        mFaults = f;
        n = 3 * f + 1;
        int clientPort = firstPort + n ;
        nodes = new Node[n];
        for (int i = 0; i < n - 1; i++) {
            nodes[i] = new Node(i + 1, firstPort, f,clientPort);
            firstPort++;
        }
        dealer = new Dealer(n, firstPort, f,clientPort);
        nodes[n - 1] = dealer;
        for (int i = 0; i < n; i++) {
            nodes[i].setNodes(nodes);
        }

    }

    public boolean isRunning() {
        if(dealer !=null ){
            return (!(dealer.isStoreDone()));
        }
        return client != null && client.processStatus == Client.ProccesStatus.ACTIVE;
    }

    @Override
    public void store(int key, int value) {
        if(client !=null){
            client.killClientReceiver();
        }
        dealer.startProcess(key, value);
    }

    @Override
    public int retrieve(int key) {
        while (!dealer.isStoreDone()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(!isDealerRegularNode) {
            dealer.switchReciever();
            isDealerRegularNode = true;
        }
        if(client == null) {
            client = new Client(n + 1, ++firstPort, mFaults);
            client.setNodes(nodes);
        }
        client.startProcess(key);
        return client.getValue();
    }
}
