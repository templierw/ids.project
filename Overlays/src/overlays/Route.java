package overlays;

public class Route {

    public int to;
    public int nbHop;
    public int gate;

    public Route(int to, int nbHop, int gate) {
        this.to = to;
        this.nbHop = nbHop;
        this.gate = gate;
    }

    public String toString() {
        return (
            "{to: "+ this.to +"} {hops: " + this.nbHop +"} {gate:"+ this.gate + "}"
        );
    }
    
}
