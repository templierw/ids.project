package overlays;

import java.io.Serializable;

public class Packet implements Serializable {

    private static final long serialVersionUID = -7173279238153886092L;
    
    enum PacketType {
        TABLE,
        MSG,
        HELLO,
        BYE
    }

    PacketType type;
    int from;
    int to;
    int nbHop;
    String msg;
    
}
