/**
 * Created by Itai on 26/02/2018.
 */
public interface WalletInterface {

    public void store(String key,Object value);

    public Object retrieve(String key);
}
