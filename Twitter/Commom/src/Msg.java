public class Msg {

    private int timeStamp;
    private int serverID;
    private String msg;

    public Msg(int timeStamp, int serverID, String msg) {
        this.serverID = serverID;
        this.timeStamp = timeStamp;
        this.msg = msg;
    }
}
