package wallet.node;

import java.util.Random;

import static wallet.node.Message.INITIAL_VALUES;
import static wallet.node.Message.PRIVATE;

/**
 * Created by Itai on 26/02/2018.
 */
public class Dealer extends Node {
    private int mFaults;
    private Random mRandom;
    Node[] mNodes;

    public Dealer(int port, int f, Node[] nodes) {
        super(0, port);
        mFaults = f;
        mRandom = new Random();
        mNodes = nodes;
    }

    public void startProcess(Object ob) {
        int[] q = createArrayOfCoefs();
        q[0] = 2; // decide what to do
        int[] p = createArrayOfCoefs();
        p[0] = 1;
        for (Node mNode : mNodes) {  // Iterate over all Nodes
            String answer = buildInitialValues(mNode.mNumber, q, p);
            Message msg = new Message(this.mNumber,PRIVATE,INITIAL_VALUES,answer);
            sendMessageToNode(mNode, msg);
        }
    }

    private String buildInitialValues(int i, int[] q, int[] p) {
        StringBuilder response = new StringBuilder();
        buildResponse( response, i, q,  p);
        response.append("|");
        buildResponse( response, i, p,  q);
        return response.toString();
    }

    private void buildResponse(StringBuilder response,int i, int[] q, int[] p){
        int val1 = compute(q, i);
        for (int n = 0; n < mNodes.length; n++) {
            int val2 = compute(p, mNodes[n].mNumber);
            if (n == mNodes.length - 1)
                response.append(val1 * val2);
            else
                response.append(val1 * val2).append(",");
        }
    }

    private int compute(int[] arr, int x) {
        int res = 0;
        for (int i = 0; i < arr.length; i++) {
            res += arr[i] * Math.pow(x, i);
        }
        return res;
    }


    private int[] createArrayOfCoefs() {
        int[] arr = new int[mFaults+1];
        for (int i = 1; i < arr.length; i++) {
            arr[i] = mRandom.nextInt(10 * mFaults);
        }
        return arr;
    }

}
