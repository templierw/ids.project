package overlays;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import overlays.Packet.PacketType;

public class VirtualNode extends Thread{
    // virtual
    private int id;
    private int leftNeighbour;
    private int rightNeighbour;

    private MessageBuffer inBuff, outBuff;
    private ExecutorService services;

    public PhysicalNode physLayer;

    public VirtualNode(int id, String[] physNeigh) {

        this.id = id;
        this.rightNeighbour = (id + 1) % physNeigh.length;
        this.leftNeighbour = (physNeigh.length + id - 1) % physNeigh.length; // Assuming clockwise and increasing indexing (0, 1, .., N)
        
        this.inBuff = new MessageBuffer(10);
        this.outBuff = new MessageBuffer(10);
        
        this.services = Executors.newFixedThreadPool(2);

        this.physLayer = new PhysicalNode(id, physNeigh, inBuff, outBuff);
        services.execute(this.physLayer);

        services.execute(new Runnable() {  
            public void run() { 
                while(true) {
                    Packet recvPck = inBuff.getMessage();
                    System.out.println(
                        "\n\t[" + recvPck.from + "]: " + recvPck.msg 
                    );
                }
            }
        });

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

        this.outBuff.putMessage(pck);
             
    }

    public void close() {
        this.physLayer.close();
        /*Packet pck = new Packet();
        pck.type = PacketType.BYE;
        pck.from = this.id;
        this.outBuff.putMessage(pck);*/
        //this.services.shutdown();
    }
}
