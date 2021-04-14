package overlays.frames;

import java.io.Serializable;

public class Packet implements Serializable {

    private static final long serialVersionUID = -7173279238153886092L;
    
    public enum PacketType {
        TABLE,
        MSG,
        HELLO,
        BYE
    }

    public PacketType type;
    public int from;
    public int to;
    public int nbHop;
    public Message msg;
    
}
