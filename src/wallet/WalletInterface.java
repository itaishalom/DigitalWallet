package wallet;

/**
 * Created by Itai on 26/02/2018.
 */
public interface WalletInterface {

    public void store(int key,int value);

    public int retrieve(int key);
}
