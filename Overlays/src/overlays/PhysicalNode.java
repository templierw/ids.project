package overlays;


import java.util.Scanner;

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

    public boolean ready;

    public PhysicalNode(int id, String[] neighbours) {
        
        this.id = id;
        this.table = new RoutingTable(id, neighbours);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

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

        this.gossip(); // flood with your initial info
        
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {

            String[] msg = new String(delivery.getBody(), "UTF-8").split(":");

            if (msg[0].compareTo("table") == 0) {
                Route r = new Route(
                    Integer.parseInt(msg[1]),
                    Integer.parseInt(msg[2]),
                    Integer.parseInt(msg[3])
                );
                if (this.id != r.to) // don't need the route to get to myself ...
                    if (this.table.updateTable(r)) // flood only if new
                        this.gossip();
            }

            if (msg[0].compareTo("msg") == 0) {
                int to = Integer.parseInt(msg[1]);

                if (to == this.id) {
                    System.out.println("[" + this.id + "received " + msg[3] + " from " + msg[2]);
                
                } else {
                    System.out.println("routing " + msg[3] + " from " + msg[2] + " to " + to);
                    this.send(msg[3], to, Integer.parseInt(msg[2]));
                }
            }
        };

        try {
            channel.basicConsume(this.queueName, true, deliverCallback, consumerTag -> { });
            
            if (ready && this.id == 0) {
                Scanner s = new Scanner(System.in);
                String msg[];
                
                    System.out.print(">> ");
                    msg = s.nextLine().split(" ");
                    this.send(msg[1], Integer.parseInt(msg[0]), this.id);
            }
                
            //s.close();

        } catch (Exception e) {
            System.err.println("lalaAn error occured... Program exiting");
            System.exit(1);
        }
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
    }

    public void close() {
        try {
            this.channel.close();
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    public void send(String msg, int to, int from) {
        this.table.printTable();
        Route nextHop = this.table.getRouteTo(to);

        int gate;
        if (nextHop == null)
            gate = 1;

        gate = nextHop.gate;

        msg = "msg:" + to + ":" + from + ":" + msg;

        try {
            channel.basicPublish(EXCHANGE_NAME, "link" + gate, null, msg.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
