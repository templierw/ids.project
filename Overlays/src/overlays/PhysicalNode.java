package overlays;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class PhysicalNode extends Thread{
    // virtual

    private static final String EXCHANGE_NAME = "o";

    public int id;
    public RoutingTable table;
    private Channel channel;
    private String queueName;

    private ExecutorService services;
    static private Semaphore mustGossip;

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
                    while(true) {
                        mustGossip.acquire();
                        System.out.println("gossiping");
                        gossip(); // flood with your initial info
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        this.services.execute(new Runnable() {  
            public void run() {  
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {

                    String[] msg = new String(delivery.getBody(), "UTF-8").split(":");

                    if (msg[0].compareTo("hello") == 0) {
                        mustGossip.release();
                    }
        
                    if (msg[0].compareTo("table") == 0) {

                        Route r = new Route(
                            Integer.parseInt(msg[1]), // to
                            Integer.parseInt(msg[2]), // nbHop
                            Integer.parseInt(msg[3])  // gate
                        );
                        if (id != r.to) // don't need the route to get to myself ...
                            if (table.updateTable(r)) // flood only if new
                                mustGossip.release();
                    }
        
                    if (msg[0].compareTo("msg") == 0) {

                        int to = Integer.parseInt(msg[1]);
        
                        if (to == id) {
                            System.out.println("[" + id + "]" + "received " + msg[3] + " from " + msg[2]);
                        
                        } else {
                            System.out.println("routing " + msg[3] + " from " + msg[2] + " to " + to);
                            send(msg[3], to, Integer.parseInt(msg[2]));
                        }
                    }
                };
        
                try {
                    channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
                    
        
                } catch (Exception e) {
                    System.err.println("lalaAn error occured... Program exiting");
                    System.exit(1);
                }
                  
            }  
        });
        
    }

    private void gossip() { 
        for (Integer n : this.table.getNeighbours()) {
            for (Route r : this.table.table) {
                if (n != r.to) {
                    String msg = "table:" + r.to + ":" + r.nbHop + ":" + this.id;
                    
                    try {
                        channel.basicPublish(EXCHANGE_NAME, "link" + n, null, msg.getBytes());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        //mustGossip.set(false);
    }

    private void hello() { 
        for (Integer n : this.table.getNeighbours()) {  
            try {
                String msg = "hello:" + this.id; 
                channel.basicPublish(EXCHANGE_NAME, "link" + n, null, msg.getBytes());
            } catch (Exception e) {
                e.printStackTrace();        
            }                        
        }
        //mustGossip.release();
    }

    public void close() {
        try {
            this.channel.close();
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    public void send(String msg, int to, int from) {
        services.execute(new Runnable() {
            public void run() {
                Route nextHop = table.getRouteTo(to);
        
                int gate;
                if (nextHop == null)
                    gate = 1;
        
                else gate = nextHop.gate;
        
                String toSend = "msg:" + to + ":" + from + ":" + msg;
        
                try {
                    channel.basicPublish(EXCHANGE_NAME, "link" + gate, null, toSend.getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
