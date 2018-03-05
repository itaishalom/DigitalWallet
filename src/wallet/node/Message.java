package wallet.node;

/**
 * Created by Itai on 03/03/2018.
 */
public class Message {
    public static final String PRIVATE = "private";
    public static final String BROADCAST = "broadcast";

    public static final String INITIAL_VALUES = "initial_values";
    public static final String COMPARE = "compare";
    public static final String OK = "ok";
    public static final String COMPLAINT = "complaint";

    public static final String LOCAL_HOST = "localhost";

    private int mFrom;
    private String mType;
    private String mSubType;
    private String mInfo;

    public Message(int from, String type, String subType, String info){
        mFrom = from;
        mType = type;
        mSubType = subType;
        mInfo = info;
    }

    public int getmFrom() {
        return mFrom;
    }

    public String getmType() {
        return mType;
    }

    public String getmSubType() {
        return mSubType;
    }

    public String getmInfo() {
        return mInfo;
    }

    public Message(String text){
        String[] split = text.split(" ~ ");

        mFrom = Integer.valueOf(split[0]);
        mType = split[1];
        mSubType = split[2];
        mInfo = split[3];

    }

    @Override
    public String toString(){
        return mFrom +" ~ " + mType +" ~ " + mSubType + " ~ " + mInfo;
    }

    public boolean isPrivate(){
        return mType.equals(PRIVATE);
    }
    public boolean isValues(){
        return mSubType.equals(INITIAL_VALUES);
    }
    public boolean isCompare(){
        return mSubType.equals(COMPARE);
    }
}
