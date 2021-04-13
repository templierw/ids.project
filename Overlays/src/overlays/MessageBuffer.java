package overlays;

import java.util.concurrent.Semaphore;

public class MessageBuffer {

    private int in, out;
    private Semaphore lock, full, empty;
    private Packet[] buffer;

    public MessageBuffer(int maxSize) {

        this.buffer = new Packet[maxSize];
        this.lock = new Semaphore(1);
        this.full = new Semaphore(0);
        this.empty = new Semaphore(maxSize);
        this.in = this.out = 0;

    }

    public void putMessage(Packet pck) {
        try {
            empty.acquire();
            lock.acquire();
            this.buffer[in] = pck;
            in = (in + 1) % this.buffer.length;
            
            lock.release();
            full.release();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Packet getMessage() {
        Packet pck = null;
        try {
            full.acquire();
            lock.acquire();
            pck = this.buffer[out];
            out = (out + 1) % this.buffer.length;
            
            lock.release();
            empty.release();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return pck;
    }
    
}
