package overlays;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import overlays.frames.Message;

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
                    Message recvMsg = inBuff.getMessage();
                    System.out.println(
                        "\n\t[" + recvMsg.getSender() + "]: " + recvMsg.readMessage() 
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

        Message msg = new Message (
            this.id,
            (right? this.rightNeighbour : this.leftNeighbour)
        );
        
        msg.writeMessage(message);

        this.outBuff.putMessage(msg);
             
    }

    public void close() {
        this.physLayer.close();
    }
}
