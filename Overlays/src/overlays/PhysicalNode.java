package overlays;

import java.io.IOException;
import java.util.LinkedList;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class PhysicalNode extends Thread{
    // virtual

    private static final String EXCHANGE_NAME = "o";

    public int id;
    public RoutingTable table;
    public int nbNodes;
    public LinkedList<Integer> neighbours;


    private Channel channel;
    private String queueName;
    public boolean isDone;

    public PhysicalNode(int id, String[] neighbours) {
        
        this.id = id;
        this.table = new RoutingTable(id, neighbours);
        this.neighbours = this.table.getNeighbours();

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

        this.broadcast(); // flood with your initial info
        
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
                        this.broadcast();
            }
        };

        try {
            channel.basicConsume(this.queueName, true, deliverCallback, consumerTag -> { });

        } catch (Exception e) {
            System.err.println("lalaAn error occured... Program exiting");
            System.exit(1);
        }
    }

    private void broadcast() {
        for (Integer n : this.neighbours) {
            for (Route r : this.table.table) {
                if (n != r.to) {
                    String msg = "table:" + r.to + ":" + r.nbHop + ":" + this.id;
                    
                    try {
                        channel.basicPublish(EXCHANGE_NAME, "link" + n, null, msg.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
