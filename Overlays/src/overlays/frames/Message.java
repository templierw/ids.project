package overlays.frames;

import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 206657426723855743L;

    private String content;
    private int sender, receiver;

    public Message(int sender, int receiver) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = "";
    }

    public void writeMessage(String msg) {
        this.content = msg;
    }

    public String readMessage() {
        return this.content;
    }

    public int getSender() {
        return this.sender;
    }

    public int getReceiver() {
        return this.receiver;
    }

}
