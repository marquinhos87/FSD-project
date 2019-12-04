import java.util.List;

public class Msg {

    private int timeStamp;
    private int serverID;
    private String msg;

    public Msg(String msg) {
        this.msg = msg;
    }

    public Msg(int timeStamp, int serverID, String msg) {
        this.serverID = serverID;
        this.timeStamp = timeStamp;
        this.msg = msg;
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
