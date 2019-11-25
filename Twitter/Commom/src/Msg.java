import java.util.List;

public class Msg {

    private int timeStamp;
    private int serverID;
    private String msg;
    private List<String> hastags;

    public Msg(int timeStamp, int serverID, String msg, List<String> hastags) {
        this.serverID = serverID;
        this.timeStamp = timeStamp;
        this.msg = msg;
        this.hastags = hastags;
    }

    public String getMsg() {
        return this.msg;
    }

    public int getTimeStamp() {
        return this.timeStamp;
    }

    public int getServerID() {
        return this.serverID;
    }
}
