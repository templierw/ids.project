package overlays;

import overlays.Packet.PacketType;

public class VirtualNode extends Thread{
    // virtual
    private int id;
    private int leftNeighbour;
    private int rightNeighbour;
    public PhysicalNode physLayer;

    public VirtualNode(int id, String[] physNeigh) {

        this.id = id;
        int n = physNeigh.length;
        this.rightNeighbour = (id + 1) % n;
        this.leftNeighbour = (n + id - 1) % n; // Assuming clockwise and increasing indexing (0, 1, .., N)
        this.physLayer = new PhysicalNode(id, physNeigh);
        this.physLayer.start();

    }

    public void sendLeft(String message) {
        this.send(message, false);
    }

    public void sendRight(String message){
        this.send(message, true);
    }

    private void send(String message, boolean right) {

        Packet pck = new Packet();
        pck.type = PacketType.MSG;
        pck.from = this.id;
        pck.to = (right? this.rightNeighbour : this.leftNeighbour);
        pck.msg = message;

        try {
            this.physLayer.send(pck);
        } catch (RouteException e) {
            System.err.println(e.getMessage());
        }        
    }

    public void close() {
        this.physLayer.close();
    }
}
