package overlays.utils;

import java.util.concurrent.Semaphore;

import overlays.frames.Message;

public class MessageBuffer {

    private int in, out;
    private Semaphore lock, full, empty;
    private Message[] buffer;

    public MessageBuffer(int maxSize) {

        this.buffer = new Message[maxSize];
        this.lock = new Semaphore(1);
        this.full = new Semaphore(0);
        this.empty = new Semaphore(maxSize);
        this.in = this.out = 0;

    }

    public void putMessage(Message msg) {
        try {
            empty.acquire();
            lock.acquire();
            this.buffer[in] = msg;
            in = (in + 1) % this.buffer.length;
            
            lock.release();
            full.release();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Message getMessage() {
        Message msg = null;
        try {
            full.acquire();
            lock.acquire();
            msg = this.buffer[out];
            out = (out + 1) % this.buffer.length;
            
            lock.release();
            empty.release();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return msg;
    }
    
}
