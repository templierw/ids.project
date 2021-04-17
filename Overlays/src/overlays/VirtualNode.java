package overlays;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import overlays.exception.RouteException;
import overlays.frames.Message;

public class VirtualNode extends Thread{
    // virtual
    public int id, nbNodes;

    private MessageBuffer recvBuff, sendBuff;
    private ExecutorService services;

    public PhysicalNode physLayer;

    public VirtualNode(int id, String[] physNeigh) {

        this.id = id;
        this.nbNodes = physNeigh.length;
        
        this.recvBuff = new MessageBuffer(10);
        this.sendBuff = new MessageBuffer(10);
        
        this.services = Executors.newFixedThreadPool(2);

        this.physLayer = new PhysicalNode(id, physNeigh, recvBuff, sendBuff);
        services.execute(this.physLayer);

        services.execute(new Runnable() {  
            public void run() { 
                while(true) {
                    Message recvMsg = recvBuff.getMessage();
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
            this.getNeighbour(right, false)
        );
        
        msg.writeMessage(message);

        this.sendBuff.putMessage(msg);
             
    }

    public void close() {
        this.physLayer.close();
    }

    public void printNeighbours() {
        System.out.println(
            "right: " + this.getNeighbour(true, false) +
            "\nleft: "+ this.getNeighbour(false, false)
        );
        for(Neighbour n : physLayer.neigh)
            n.display();
    }

    private int getNeighbour(boolean right, boolean init) {
        boolean ok = init;
        int neigh = -1, nodes = this.physLayer.getNbAlive() + 1;

        if (nodes == 1) return this.id;

        System.out.println(nodes);
        for(int i = 1; i < nodes & !ok; i++) {
            neigh = (right? 
                        (this.id + i) % nodes : 
                        (nodes + id - i) % nodes
                    );
            try {
                ok = !this.physLayer.table.isDeadRoute(neigh);
            } catch (RouteException e) {
                e.printStackTrace();
            }
            }
            
        return neigh;
    }
}
