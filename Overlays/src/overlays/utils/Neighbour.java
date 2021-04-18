package overlays.utils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Neighbour {

    public int id;
    public AtomicBoolean isAlive;
    public AtomicInteger TTL;

    public Neighbour(int id) {
        this.id = id;
        this.isAlive = new AtomicBoolean(false);
        this.TTL = new AtomicInteger(0);
    }

    public void display() {
        System.out.println(
            "["+ this.id +"]" + "[" + this.isAlive + "]" + "[" + this.TTL + "]" 
        );
    }
}
