package overlays;

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
        try {
            this.physLayer.send(message, this.leftNeighbour, this.id);
        } catch (RouteException e) {
            System.err.println(e.getMessage());
        }
    }

    public void sendRight(String message){
        try {
            this.physLayer.send(message, this.rightNeighbour, this.id);
        } catch (RouteException e) {
            System.err.println(e.getMessage());
        }
    }

    public void close() {
        this.physLayer.close();
    }
}
