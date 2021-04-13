package overlays;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import overlays.Packet.PacketType;

public class PhysicalNode extends Thread{
    // virtual

    private static final String EXCHANGE_NAME = "o";

    public int id;
    public RoutingTable table;
    private Channel channel;
    private String queueName;

    private ExecutorService services;
    static private Semaphore mustGossip;

    private AtomicBoolean exit = new AtomicBoolean(false);

    public PhysicalNode(int id, String[] neighbours) {
        
        this.id = id;
        this.table = new RoutingTable(id, neighbours);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        this.services = Executors.newFixedThreadPool(3);
        mustGossip = new Semaphore(0);

        try {
            this.channel = factory.newConnection().createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, "topic");
            this.queueName = channel.queueDeclare().getQueue();
            
            channel.queueBind(this.queueName, EXCHANGE_NAME, "link" + this.id);

        } catch (Exception e) {
            System.err.println("An error occured... Program exiting");
            System.exit(1);
        }
    }

    public void run() {

        this.hello();

        services.execute(new Runnable() {  
            public void run() { 
                try {
                    boolean loop = true;
                    while(loop) {
                        mustGossip.acquire();
                        if (!exit.get())
                            gossip();
                        loop = !exit.get();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        this.services.execute(new Runnable() {  
            public void run() {  

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {

                    try {
                        Packet recvPck = unmarshallPacket(delivery.getBody());

                        switch (recvPck.type) {
                            case TABLE:
                                if (id != recvPck.to) {
                                    // don't need the route to get to myself ...
                                    Route r = new Route(recvPck.to, recvPck.nbHop, recvPck.from);
                                    if (table.updateTable(r)) // flood only if new
                                        mustGossip.release();
                                
                                } break;

                            case HELLO:
                                mustGossip.release();
                                break;

                            case MSG:                
                                if (recvPck.to == id)
                                    System.out.println(
                                        "[" + id + "]" + "received " + recvPck.msg + " from " + recvPck.from
                                    );
                                
                                else {
                                    System.out.println("routing from " + recvPck.from + " to " + recvPck.to);
                                    try {
                                        send(recvPck);
                                    } catch (RouteException e) {
                                        System.err.println(e.getMessage());
                                    }
                                } break;
                        
                            default:
                                throw new PacketException("Invalid Packet Type");
                        }
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };
        
                try {
                    channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
        
                } catch (Exception e) {
                    System.err.println("An error occured... Program exiting");
                    System.exit(1);
                }
            }  
        });
    }

    private void gossip() { 
        for (Integer n : this.table.getNeighbours()) {
            for (Route r : this.table.table) {
                if (n != r.to) {

                    Packet pck = new Packet();
                    pck.type = PacketType.TABLE;
                    pck.from = this.id;
                    pck.to = r.to;
                    pck.nbHop = r.nbHop;
                    
                    try {
                        byte[] bytes = marshallPacket(pck);
                        channel.basicPublish(EXCHANGE_NAME, "link" + n, null, bytes);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void hello() { 
        for (Integer n : this.table.getNeighbours()) {  
            
            Packet pck = new Packet();
            pck.type = PacketType.HELLO;
            pck.from = this.id;
            
            try {
                byte[] bytes = marshallPacket(pck);
                channel.basicPublish(EXCHANGE_NAME, "link" + n, null, bytes);
            } catch (Exception e) {
                e.printStackTrace();        
            }                        
        }
    }

    public void close() {
        try {
            exit.set(true);
            this.channel.close();
            this.services.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    public void send(Packet pck) throws RouteException {

        if (!table.hasRouteTo(pck.to))
            throw new RouteException("Table [" + id + "] has no route to <" + pck.to + ">");

        else services.execute(new Runnable() {
            public void run() {

                Route nextHop = table.getRouteTo(pck.to);
        
                try {
                    byte[] bytes = marshallPacket(pck);

                    channel.basicPublish(EXCHANGE_NAME, "link" + nextHop.gate, null, bytes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private byte[] marshallPacket(Packet pck) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);

        out.writeObject(pck);
        out.flush();
        
        return bos.toByteArray();
    }

    private Packet unmarshallPacket(byte[] bytes) throws Exception {
        ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(bytes));

        return (Packet)in.readObject();
    }
}
