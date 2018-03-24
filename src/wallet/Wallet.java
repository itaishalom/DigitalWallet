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
    private int firstPort= 8090;
    private int mFaults ;

    public Wallet(int f) {
        mFaults = f;
        n = 3 * f + 1;
        nodes = new Node[n];
        for (int i = 0; i < n - 1; i++) {
            nodes[i] = new Node(i + 1, firstPort, f);
            firstPort++;
        }
        dealer = new Dealer(n,firstPort, f);
        nodes[n - 1] = dealer;
        for (int i = 0; i < n; i++) {
            nodes[i].setNodes(nodes);
        }

    }

    @Override
    public void store(int key, int value) {
        dealer.startProcess(key, value);
    }

    @Override
    public Object retrieve(int key) {
        while (!dealer.isStoreDone()){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        dealer.switchReciever();
        client = new Client(n+1,++firstPort,mFaults);
        client.setNodes(nodes);
        client.startProcess(key);
        return null;
    }
}
