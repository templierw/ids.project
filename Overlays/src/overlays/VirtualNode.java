package overlays;

import java.util.Scanner;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

public class VirtualNode extends Thread{
    // virtual

    private static final String EXCHANGE_NAME = "direct_logs";

    private Channel channel;
    private String queueName;
    public boolean isDone;
    private Scanner s;

    public VirtualNode() {
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try {
            this.channel = factory.newConnection().createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            this.queueName = channel.queueDeclare().getQueue();
            channel.queueBind(this.queueName, EXCHANGE_NAME, "router");
            channel.queueBind(this.queueName, EXCHANGE_NAME, "test1");
            this.s = new Scanner(System.in);

        } catch (Exception e) {
            System.err.println("An error occured... Program exiting");
            System.exit(1);
        }
    }

    public void run() {

        while (true) {

            String cmd = s.nextLine();
            String msg = "test1:" + cmd;
            try {
                channel.basicPublish(EXCHANGE_NAME, "router", null, msg.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
