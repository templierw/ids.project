package overlays;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Neighbour {

    int id;
    AtomicBoolean isAlive;
    AtomicInteger TTL;

    public Neighbour(int id) {
        this.id = id;
        this.isAlive = new AtomicBoolean(false);
        this.TTL = new AtomicInteger(0);
    }
}
