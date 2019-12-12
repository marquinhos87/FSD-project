package chirper.shared;

import java.util.Collection;

public class MsgGetReply
{
    public final String[] chirps;

    public MsgGetReply(Collection< ? extends CharSequence > chirps)
    {
        this.chirps = chirps.toArray(String[]::new);
    }
}
