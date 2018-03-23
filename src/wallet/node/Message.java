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
    public static final String OK2 = "ok2";
    public static final String COMPLAINT = "complaint";
    public static final String COMPLAINT_ANSWER = "complaint_answer";
    public static final String NO_OK_ANSWER = "no_ok_answer";
    public static final String LOCAL_HOST = "localhost";

    public static final int KEY = 0;
    public static final int VALUE = 1;
    public static final int CLIENT_1 = 2;
    public static final int CLIENT_2 = 3;
    public static final int TOTAL_PROCESS_VALUES = 4;



    private int mFrom;
    private String mType;
    private String mSubType;
    private String mInfo;
    private int mProcessType;

    public Message(int from,int processType, String type, String subType, String info) {
        mFrom = from;
        mProcessType = processType;
        mType = type;
        mSubType = subType;
        mInfo = info;
    }

    public int getProcessType(){
        return mProcessType;
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

    public Message(String text) {
        String[] split = text.split(" ~ ");

        mFrom = Integer.valueOf(split[0]);
        mProcessType = Integer.valueOf(split[1]);
        mType = split[2];
        mSubType = split[3];
        mInfo = split[4];
    }

    @Override
    public String toString() {
        return mFrom + " ~ " + mProcessType + " ~ " + mType + " ~ " + mSubType + " ~ " + mInfo;
    }

    public boolean isPrivate() {
        return mType.equals(PRIVATE);
    }

    public boolean isComplaint() {
        return mSubType.equals(COMPLAINT);
    }

    public boolean isOK() {
        return mSubType.equals(OK);
    }

    public boolean isComplaintAnswer() {
        return mSubType.equals(COMPLAINT_ANSWER);
    }

    public boolean isValues() {
        return mSubType.equals(INITIAL_VALUES);
    }

    public boolean isCompare() {
        return mSubType.equals(COMPARE);
    }


}
