package overlays;

import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import overlays.exception.*;
import overlays.frames.*;
import overlays.frames.Packet.PacketType;
import overlays.utils.MessageBuffer;
import overlays.utils.Neighbour;
import overlays.utils.Route;
import overlays.utils.ShellColour;

public class PhysicalNode extends Thread {
    // virtual

    private static final String EXCHANGE_NAME = "o";
    private boolean VERBOSE;

    public int id;
    public RoutingTable table;
    private Channel channel;
    private String queueName;

    private ExecutorService services;
    private ScheduledExecutorService schedule;
    private Semaphore mustGossip;

    private AtomicBoolean exit;
    private AtomicInteger maxHost;

    MessageBuffer upBuff, downBuff;

    public LinkedList<Neighbour> neigh;

    public PhysicalNode(int id, String[] neighbours, MessageBuffer upBuff, MessageBuffer downBuff, boolean verbose) {

        this.VERBOSE = verbose;
        if (this.VERBOSE)
            this.pprint("creating physical layer...", ShellColour.ANSI_GREEN);

        this.exit = new AtomicBoolean(false);

        this.id = id;
        this.table = new RoutingTable(id, neighbours);
        this.maxHost = new AtomicInteger(this.table.table.size() + 1);

        this.neigh = new LinkedList<>();
        for (int i = 0; i < neighbours.length; i++)
            this.neigh.add(new Neighbour(i));

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        this.services = Executors.newFixedThreadPool(4);
        this.schedule = Executors.newScheduledThreadPool(2);

        mustGossip = new Semaphore(0);

        this.upBuff = upBuff;
        this.downBuff = downBuff;

        try {
            this.channel = factory.newConnection().createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, "topic");
            this.queueName = channel.queueDeclare().getQueue();

            channel.queueBind(this.queueName, EXCHANGE_NAME, "link" + this.id);

        } catch (Exception e) {
            this.errprint();
            System.exit(1);
        }
    }

    public void run() {

        try {
            this.hello();

        } catch (RouteException | DeadNodeException e) {
            this.errprint(e.getMessage());

            // Phys layer services
        } finally {

            // gossip service
            services.execute(new Runnable() {
                public void run() {

                    try {
                        while (!exit.get()) {
                            mustGossip.acquire();
                            if(!exit.get()) {
                                if (VERBOSE)
                                    pprint("gossiping...", ShellColour.ANSI_PURPLE);
                                gossip();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // send message services
            services.execute(new Runnable() {
                public void run() {

                    while (!exit.get())
                        try {
                            Packet pck = new Packet();
                            pck.type = PacketType.MSG;
                            Message msg = downBuff.getMessage();
                            pck.from = msg.getSender();
                            pck.to = msg.getReceiver();
                            pck.data = msg;

                            send(pck);

                        } catch (Exception e) {
                            errprint(e.getMessage());
                            ;
                        }
                }
            });

            // receive packet service
            this.services.execute(new Runnable() {
                public void run() {

                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {

                        try {
                            Packet recvPck = unmarshallPacket(delivery.getBody());

                            switch (recvPck.type) {
                            case TABLE:
                                switch (table.updateTable(recvPck.from,(RoutingTable) recvPck.data)) {
                                case ADDED:
                                    mustGossip.release();
                                    maxHost.addAndGet(table.lastAdded);
                                    break;
                                case UPDATED:
                                    mustGossip.release();
                                    break;
                                case IGNORED:
                                    break;
                                case DELETED:
                                    break;
                                }

                                break;

                            case HELLO:
                                if (VERBOSE)
                                    pprint("receive 'hello' from" + recvPck.from, ShellColour.ANSI_PURPLE);
                                table.resurrectRoute(recvPck.from);
                                mustGossip.release();
                                break;

                            case MSG:
                                if (recvPck.to == id)
                                    upBuff.putMessage((Message) recvPck.data);

                                else {
                                    if (VERBOSE)
                                        pprint("routing from: " + recvPck.from + ", to: " + recvPck.to,
                                                ShellColour.ANSI_BLUE);
                                    try {
                                        send(recvPck);
                                    } catch (RouteException e) {
                                        errprint(e.getMessage());
                                    }
                                }
                                break;

                            case BYE:
                                if (recvPck.to == id) {
                                    if (VERBOSE)
                                        pprint("killing route for: " + recvPck.from, ShellColour.ANSI_YELLOW);
                                    table.killRoute(recvPck.from);
                                    mustGossip.release();

                                } else
                                    send(recvPck);
                                break;

                            case PING:
                                if (VERBOSE)
                                    pprint("received ping from: " + recvPck.from, ShellColour.ANSI_CYAN);
                                Neighbour n = neigh.get(recvPck.from);
                                n.isAlive.set(true);
                                n.TTL.set(5);
                                if (table.isDeadRoute(n.id)) {
                                    table.resurrectRoute(n.id);
                                    mustGossip.release();
                                }
                                break;

                            default:
                                throw new PacketException("Invalid Packet Type");
                            }

                        } catch (Exception e) {
                            pprint("ici1", ShellColour.ANSI_YELLOW);
                            errprint(e.getMessage());
                        }
                    };

                    try {
                        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
                        });

                    } catch (Exception e) {
                        errprint();
                        System.exit(1);
                    }
                }
            });

            this.schedule.scheduleAtFixedRate(new Runnable() {
                public void run() {

                    for (Integer nidx : table.getNeighbours()) {
                        Neighbour n = neigh.get(nidx);

                        if (n.TTL.decrementAndGet() == 0)
                            n.isAlive.set(false);

                        try {
                            Packet pck = new Packet();
                            pck.type = PacketType.PING;
                            pck.from = id;
                            pck.to = n.id;
                            send(pck);

                        } catch (Exception e) {
                            errprint(e.getMessage());
                        }

                    }

                    try {
                        Thread.sleep(2000);
                        for (Integer nidx : table.getNeighbours())
                            if (!neigh.get(nidx).isAlive.get()) {
                                if (!table.isDeadRoute(nidx)) {
                                    table.killRoute(nidx);
                                    mustGossip.release();
                                }
                            }
                    } catch (Exception e) {
                        errprint(e.getMessage());
                    }
                }
            }, 0, 5, TimeUnit.SECONDS);
        }
    }

    private void gossip() throws InterruptedException {
        for (Integer n : this.table.getNeighbours()) {
            Packet pck = new Packet();
            pck.type = PacketType.TABLE;
            pck.from = this.id;
            pck.to = n;
            pck.data = this.table;
            try {
                send(pck);
            } catch (RouteException e) {
                errprint(e.getMessage());
            }
        }
    }

    private void hello() throws RouteException, DeadNodeException {
        for (Integer n : this.table.getNeighbours()) {
            Packet pck = new Packet();
            pck.type = PacketType.HELLO;
            pck.from = this.id;
            pck.to = n;

            try {
                send(pck);
            } catch (RouteException e) {
                errprint(e.getMessage());
            }
        }
    }

    public void send(Packet pck) throws RouteException {

        if (!table.hasRouteTo(pck.to))
            throw new RouteException("Table [" + id + "] has no route to <" + pck.to + ">");

        else
            services.execute(new Runnable() {
                public void run() {

                    Route nextHop = table.getRouteTo(pck.to);

                    try {
                        byte[] bytes = marshallPacket(pck);
                        channel.basicPublish(EXCHANGE_NAME, "link" + nextHop.gate, null, bytes);
                    } catch (Exception e) {
                        errprint(e.getMessage());
                    }
                }
            });
    }

    public void close() {
        exit.set(true);
        this.schedule.shutdown();
        this.mustGossip.release(); // to kill it
        this.leaveNetwork();
        this.services.shutdown();
    }

    public void leaveNetwork() {
        for (Integer n : this.table.getNeighbours()) {
            Packet pck = new Packet();
            pck.type = PacketType.BYE;
            pck.from = id;
            pck.to = n;

            try {
                send(pck);
            } catch (RouteException e) {
                errprint(e.getMessage());
            }
        }
    }

    public boolean isDead(int node) {
        return this.neigh.get(node).isAlive.get();
    }

    public int getMaxHost() {
        return this.maxHost.get();
    }

    private byte[] marshallPacket(Packet pck) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);

        out.writeObject(pck);
        out.flush();

        return bos.toByteArray();
    }

    private Packet unmarshallPacket(byte[] bytes) throws Exception {
        ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(bytes));

        return (Packet) in.readObject();
    }

    private void pprint(String msg, String colour) {
        System.out.println("\t\t" + colour + "PHYS.LAYER: " + msg + ShellColour.ANSI_RESET);
    }

    private void errprint(String msg) {
        System.err.println(ShellColour.ANSI_RED + "\t\tPHYS.LAYER: " + msg + ShellColour.ANSI_RESET);
    }

    private void errprint() {
        System.err.println(
                ShellColour.ANSI_RED + "\t\tPHYS.LAYER: An error occured... Program exiting" + ShellColour.ANSI_RESET);
    }
}
