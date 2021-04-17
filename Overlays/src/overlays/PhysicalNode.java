package overlays;

import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import overlays.exception.*;
import overlays.frames.*;
import overlays.frames.Packet.PacketType;

public class PhysicalNode extends Thread {
    // virtual

    private static final String EXCHANGE_NAME = "o";

    public int id;
    public RoutingTable table;
    private Channel channel;
    private String queueName;

    private ExecutorService services;
    private ScheduledExecutorService schedule;
    private Semaphore mustGossip, canLeave;

    private AtomicBoolean exit = new AtomicBoolean(false);

    MessageBuffer upBuff, downBuff;

    public LinkedList<Neighbour> neigh;

    public PhysicalNode(int id, String[] neighbours, MessageBuffer upBuff, MessageBuffer downBuff) {

        this.id = id;
        this.table = new RoutingTable(id, neighbours);

        this.neigh = new LinkedList<>();
        for (int i = 0; i < neighbours.length; i++)
            this.neigh.add(new Neighbour(i));

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        this.services = Executors.newFixedThreadPool(4);
        this.schedule = Executors.newScheduledThreadPool(1);

        mustGossip = new Semaphore(0);
        canLeave = new Semaphore(0);

        this.upBuff = upBuff;
        this.downBuff = downBuff;

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

        try {
            this.hello();

        } catch (RouteException | DeadNodeException e1) {
            System.err.println(e1.getMessage());

        } finally {
            services.execute(new Runnable() {
                public void run() {

                    try {
                        boolean loop = true;
                        while (loop) {
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

            services.execute(new Runnable() {
                public void run() {

                    while (true)
                        try {
                            Packet pck = new Packet();
                            pck.type = PacketType.MSG;
                            Message msg = downBuff.getMessage();
                            pck.from = msg.getSender();
                            pck.to = msg.getReceiver();
                            pck.msg = msg;

                            send(pck);

                        } catch (Exception e) {
                            System.err.println(e.getMessage());
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
                                    Route r = new Route(recvPck.to, recvPck.nbHop, recvPck.from, recvPck.alive);
                                    if (table.updateTable(r)) // flood only if new
                                        mustGossip.release();

                                }
                                break;

                            case HELLO:
                                table.resurrectRoute(recvPck.from);
                                mustGossip.release();
                                break;

                            case MSG:
                                if (recvPck.to == id)
                                    upBuff.putMessage(recvPck.msg);

                                else {
                                    System.out.println("\t\nrouting from " + recvPck.from + " to " + recvPck.to);
                                    try {
                                        send(recvPck);
                                    } catch (RouteException e) {
                                        System.err.println(e.getMessage());
                                    }
                                }
                                break;

                            case BYE:
                                if (recvPck.to == id) {
                                    System.out.println("removing table for " + recvPck.from);
                                    table.killRoute(recvPck.from);

                                } else
                                    send(recvPck);
                                break;

                            case PING:
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
                            System.err.println(e.getMessage());
                        }
                    };

                    try {
                        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
                        });

                    } catch (Exception e) {
                        System.err.println("An error occured... Program exiting");
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
                            System.err.println(e.getMessage());
                        }

                    }

                    try {
                        Thread.sleep(2000);
                        for (Integer nidx : table.getNeighbours())
                            if (!neigh.get(nidx).isAlive.get()) {
                                if (!table.isDeadRoute(nidx)){
                                    table.killRoute(nidx);
                                    mustGossip.release();
                                }
                            }
                    } catch (RouteException e) {
                        System.err.println(e.getMessage());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 5, TimeUnit.SECONDS);
        }
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
                    pck.alive = r.alive;

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

    private void hello() throws RouteException, DeadNodeException {
        for (Integer n : this.table.getNeighbours()) {
            if (!table.isDeadRoute(n)) {

                Packet pck = new Packet();
                pck.type = PacketType.HELLO;
                pck.from = this.id;
                pck.to = n;

                try {
                    byte[] bytes = marshallPacket(pck);
                    channel.basicPublish(EXCHANGE_NAME, "link" + n, null, bytes);
                    mustGossip.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void close() {
        try {
            this.leaveNetwork();
            this.canLeave.acquire();
            exit.set(true);
            // this.channel.close();
            // this.services.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
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
                        e.printStackTrace();
                    }
                }
            });
    }

    public void leaveNetwork() {
        for (Route r : table.table) {
            Packet pck = new Packet();
            pck.type = PacketType.BYE;
            pck.from = id;
            pck.to = r.to;

            try {
                send(pck);
            } catch (RouteException e) {
                e.printStackTrace();
            }
        }
        canLeave.release();
    }

    public boolean isDead(int node) {
        return this.neigh.get(node).isAlive.get();
    }
    
    public int getNbAlive() {
        int nb = 0;
        for(Route r : this.table.table)
            if (r.alive) nb++;
        
        return nb;
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
}
