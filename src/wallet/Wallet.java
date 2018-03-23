package wallet;
import wallet.node.Dealer;
import wallet.node.Node;


/**
 * Created by Itai on 26/02/2018.
 */
public class Wallet implements WalletInterface {
    private Node[] nodes;
    private Dealer dealer;
    public Wallet(int f){
        int firstPort = 8090;
        int n = 3*f+1;
        nodes = new Node[n];
        for(int i = 0; i < n-1; i++ ){
            nodes[i] = new Node(i+1,firstPort,f);
            firstPort++;
        }
        dealer = new Dealer(firstPort,f);
        nodes[n-1] = dealer;
        for(int i = 0; i < n; i++ ){
            nodes[i].setNodes(nodes);
        }

    }

    @Override
    public void store(int key, int value) {
        dealer.startProcess(key,value);
    }

    @Override
    public Object retrieve(int key) {
        return null;
    }
}
