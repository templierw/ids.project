package overlays;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;


public class RoutingNode extends Thread {
    
    private static final String EXCHANGE_NAME = "direct_logs";

    private Channel channel;
    private String queueName;
    public boolean isDone;

    public RoutingNode() {
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try {
            this.channel = factory.newConnection().createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            this.queueName = channel.queueDeclare().getQueue();
            channel.queueBind(this.queueName, EXCHANGE_NAME, "router");

        } catch (Exception e) {
            System.err.println("An error occured... Program exiting");
            System.exit(1);
        }
    }

    public void run() {

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String r = new String(delivery.getBody(), "UTF-8");
            String[] msg = r.split(":");
            System.out.println(r);

            String from = msg[0], 
                   to   = msg[1],
                   route;
            
            channel.basicPublish(EXCHANGE_NAME, from, null, "lalala".getBytes());

        };

        try {
            channel.basicConsume(this.queueName, true, deliverCallback, consumerTag -> { });

        } catch (Exception e) {
            System.err.println("An error occured... Program exiting");
            System.exit(1);
        }

        return;
    }
    
}
