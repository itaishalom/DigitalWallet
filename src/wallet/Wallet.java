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
        int n = 3*f;
        nodes = new Node[n];
        for(int i = 0; i < n; i++ ){
            nodes[i] = new Node(i+1,firstPort,f);
            firstPort++;
        }
        for(int i = 0; i < n; i++ ){
            nodes[i].setNodes(nodes);
        }
        dealer = new Dealer(firstPort,f,nodes);
    }

    @Override
    public void store(String key, Object value) {
        dealer.startProcess(key);
    }

    @Override
    public Object retrieve(String key) {
        return null;
    }
}
