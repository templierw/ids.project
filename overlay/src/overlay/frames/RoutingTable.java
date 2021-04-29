package overlay.frames;

import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import overlay.exception.RouteException;
import overlay.utils.Route;

public class RoutingTable extends Sendable {

    public enum RouteStatus {
        IGNORED,
        ADDED,
        UPDATED,
        DELETED
    }

    private int id;
    public LinkedList<Route> table;
    public int lastAdded;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();

    public RoutingTable(int id, String[] neigbours) {
        this.id = id;
        this.table = new LinkedList<>();
        initTable(neigbours);
    }

    private void initTable(String[] neighbours) {
        for (int i = 0; i < neighbours.length; i++) {
            if (neighbours[i].charAt(0) == '1') {
                try {
                    this.w.lock();
                    this.table.add(new Route(i, 1, i, false));
                } finally {
                    this.w.unlock();
                }
            }
        }
    }

    public RouteStatus updateTable(int from, RoutingTable table) {

        RouteStatus res = RouteStatus.IGNORED;
        this.lastAdded = 0;

        for (Route r : table.table) {
            if (r.to != this.id)
                if (this.hasRouteTo(r.to)) {
                    
                    Route myRoute = this.getRouteTo(r.to);
                    try {
                        this.w.lock();
                        if (myRoute.alive != r.alive) {
                            myRoute.alive = r.alive;
                            if (res != RouteStatus.ADDED)
                                res = RouteStatus.UPDATED;
                        }
        
                    } finally {
                        this.w.unlock();
                    }
        
                    if (myRoute.nbHop > r.nbHop + 1) {
                        try {
                            this.w.lock();
                            myRoute.gate = from;
                            myRoute.nbHop = r.nbHop + 1;
                            if (res != RouteStatus.ADDED)
                                res = RouteStatus.UPDATED;;
        
                        } finally {
                            this.w.unlock();
                        }
                    }
        
                } else {
                    r.nbHop += 1;
                    r.gate = from;
                    try {
                        this.w.lock();
                        this.table.add(r);
                        res = RouteStatus.ADDED;
                        this.lastAdded++;
        
                    } finally {
                        this.w.unlock();
                    }
                }        
            }
        return res;
    }

    public boolean hasRouteTo(int to) {
        boolean hasRoute = false;
        try {
            this.r.lock();
            for (Route r : this.table)
                if (r.to == to) {
                    hasRoute = true;
                    break;
                }

        } finally {
            this.r.unlock();
        }

        return hasRoute;
    }

    public Route getRouteTo(int to) {

        if (!this.hasRouteTo(to))
            return null;

        try {
            this.r.lock();
            for (Route r : this.table) {
                if (r.to == to)
                    return r;
            }

        } finally {
            this.r.unlock();

        }
        return null;
    }

    public void printTable() {
        System.out.println("[" + this.id + "]");

        try {
            this.r.lock();
            for (Route r : this.table)
                System.out.println("\t" + r.toString());

        } finally {
            this.r.unlock();

        }
        System.out.println("----------------------------------");
    }

    public LinkedList<Integer> getNeighbours() {
        LinkedList<Integer> neighbours = new LinkedList<>();

        try {
            this.r.lock();
    
            for (Route r : this.table) {
                if (r.nbHop == 1)
                    neighbours.add(r.to);
            }

        } finally {
            this.r.unlock();

        }

        return neighbours;
    }

    public void killRoute(int to) throws RouteException {
        if (!this.hasRouteTo(to)) return;
        
        try {
            this.w.lock();

            for(int i=0; i < this.table.size(); i++) {
                Route r = this.table.get(i);
                if (r.gate == to) {
                    r.alive = false;
                }
            }

        } finally {
            this.w.unlock();
        }
    }

    public void resurrectRoute(int to) throws RouteException {

        Route toRes = null;
        try {
            this.r.lock();
            for (Route r : this.table)
                if (r.to == to) {
                    toRes = r;
                    break;
                }
        } finally {
            this.r.unlock();
        }

        if (toRes == null)
            throw new RouteException("No such route to resurrect...");

        else {
            try {
                this.w.lock();
                toRes.alive = true;

            } finally {
                this.w.unlock();
            }
        }

    }

    public boolean isDeadRoute(int to) throws RouteException {
        boolean isDead = false;
        try {
            this.r.lock();
            for (Route r : this.table)
                if (r.to == to) {
                    isDead = !r.alive;
                    break;
                }

        } finally {
            this.r.unlock();
        }

        return isDead;
    }


}
