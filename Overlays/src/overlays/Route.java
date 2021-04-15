package overlays;

public class Route {

    public int to;
    public int nbHop;
    public int gate;
    public boolean alive;

    public Route(int to, int nbHop, int gate, boolean alive) {
        this.to = to;
        this.nbHop = nbHop;
        this.gate = gate; // neighbour with shortest route
        this.alive = alive;
    }

    public String toString() {
        return (
            "{to: "+ this.to +"} {hops: " + this.nbHop +
            "} {gate:"+ this.gate + "} {" + (this.alive? "ALIVE" : "DEAD") + "}"
        );
    }
    
}
