package overlay;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import overlay.exception.RouteException;
import overlay.frames.Message;
import overlay.utils.MessageBuffer;
import overlay.utils.Neighbour;
import overlay.utils.ShellColour;

public class VirtualNode extends Thread{
    // virtual
    public int id, nbNodes;

    private MessageBuffer recvBuff, sendBuff;
    private ExecutorService services;

    public PhysicalNode physLayer;

    public VirtualNode(int id, String[] physNeigh, boolean verbose) {

        this.id = id;
        this.nbNodes = physNeigh.length;
        
        this.recvBuff = new MessageBuffer(10);
        this.sendBuff = new MessageBuffer(10);
        
        this.services = Executors.newFixedThreadPool(2);

        this.physLayer = new PhysicalNode(id, physNeigh, recvBuff, sendBuff, verbose);
        services.execute(this.physLayer);

        services.execute(new Runnable() {  
            public void run() { 
                while(true) {
                    Message recvMsg = recvBuff.getMessage();
                    System.out.println(ShellColour.ANSI_GREEN +
                        "\n\t[" + recvMsg.getSender() + "]: " + recvMsg.readMessage() +
                        ShellColour.ANSI_RESET
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
            this.getNeighbour(right)
        );
        
        msg.writeMessage(message);

        this.sendBuff.putMessage(msg);
             
    }

    public void close() {
        this.physLayer.close();
    }

    public void printNeighbours() {
        System.out.println(
            "right: " + this.getNeighbour(true) +
            "\nleft: "+ this.getNeighbour(false)
        );
        for(Neighbour n : physLayer.neigh)
            n.display();
    }

    private int getNeighbour(boolean right) {
        int neigh = -1, nodes = this.physLayer.getMaxHost();
        if (nodes == 1) return this.id;
        
        boolean ok = false;
        for(int i = 1; i <= nodes & !ok; i++) {
            neigh = (right? 
                        (this.id + i) % nodes : 
                        (nodes + this.id - i) % nodes
                    );
            try {
                if (neigh == id) ok = true;
                else ok = !this.physLayer.table.isDeadRoute(neigh);
            } catch (RouteException e) {
                e.printStackTrace();
            }
        }
        return neigh;
    }
}
