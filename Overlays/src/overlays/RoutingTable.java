package overlays;

import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import overlays.exception.RouteException;

public class RoutingTable {

    private int id;
    public LinkedList<Route> table;
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

    public boolean updateTable(Route r) {

        boolean to_broadcast = false;

        if (r.to == this.id)
            return to_broadcast;

        if (this.hasRouteTo(r.to)) {
            Route myRoute = this.getRouteTo(r.to);

            if (myRoute.nbHop > r.nbHop + 1) {
                try {
                    this.w.lock();
                    myRoute.gate = r.gate;
                    myRoute.nbHop = r.nbHop + 1;

                } finally {
                    this.w.unlock();
                    System.out.println("r w");
                }

                to_broadcast = true;
            }

        } else {
            r.nbHop += 1;
            try {
                this.w.lock();
                this.table.add(r);

            } finally {
                this.w.unlock();
            }
            to_broadcast = true;
        }

        return to_broadcast;
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
            System.out.println("released r");

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
        Route toKill = null;

        try {
            this.r.lock();

            for (Route r : this.table)
                if (r.to == to) {
                    toKill = r;
                    break;
                }

        } finally {
            this.r.unlock();
        }

        if (toKill == null)
            throw new RouteException("No such route to kill...");

        else {
            try {
                this.w.lock();
                toKill.alive = false;
            
            } finally {
                this.w.unlock();
            }
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
