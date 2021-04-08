package overlays;

import java.util.LinkedList;

public class RoutingTable {

    int id;
    public LinkedList<Route> table;

    public RoutingTable(int id, String[] neigbours) {
        this.id = id;
        this.table = new LinkedList<>();
        initTable(neigbours);
    }

    private void initTable(String[] neighbours) {
        for (int i = 0; i < neighbours.length; i++) {
            if (neighbours[i].charAt(0) == '1') {
                this.table.add(new Route(i, 1, i));
            }
        }
    }

    public boolean updateTable(Route r) {

        boolean to_broadcast = false;

        if (r.to == this.id)
            return to_broadcast;
        
        if (this.hasRouteTo(r.to)){
            Route myRoute = this.getRouteTo(r.to);

            if (myRoute.nbHop > r.nbHop + 1) {
                myRoute.gate = r.gate;
                myRoute.nbHop = r.nbHop + 1;

                to_broadcast = true;
            }

        } else {
            r.nbHop += 1;
            this.table.add(r);
            to_broadcast = true;
        }

        return to_broadcast;
    }

    public boolean hasRouteTo(int to) {
        for (Route r : this.table) 
            if (r.to == to)
                return true;

        return false;
    }

    public Route getRouteTo(int to) {

        if (!this.hasRouteTo(to))
            return null;

        for (Route r: this.table) {
            if (r.to == to)
                return r;
        }

        return null;
    }
    
    public void printTable() {
        System.out.println("["+ this.id +"]");
        for (Route r : this.table) 
            System.out.println("\t" + 
                r.toString()
            );
        
        System.out.println("----------------------------------");
        
    }

    public LinkedList<Integer> getNeighbours() {
        LinkedList<Integer> neighbours = new LinkedList<>();

        for (Route r : this.table) {
            if (r.nbHop == 1)
                neighbours.add(r.to);
        }

        return neighbours;
    }
}
