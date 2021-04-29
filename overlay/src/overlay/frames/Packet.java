package overlay.frames;

import java.io.Serializable;

public class Packet implements Serializable {

    private static final long serialVersionUID = -7173279238153886092L;
    
    public enum PacketType {
        TABLE,
        MSG,
        HELLO,
        BYE,
        PING
    }

    public PacketType type;
    public int from;
    public int to;
    public Sendable data;
    
}
