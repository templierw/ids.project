package overlay;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

public class Launcher {  
    public static void main(String[] args) throws Exception {

        Scanner s = new Scanner(System.in);
        
        int id = Integer.parseInt(args[1]);
        String neighbours = "";
        try {
            BufferedReader physReader = new BufferedReader(
                    new FileReader("./topologies/" + args[0] + ".txt")
                );

            int i = 0;
            while(i++ < id) 
                physReader.readLine();

            neighbours = physReader.readLine();
                
                physReader.close();
                
            } catch (Exception e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }

        final VirtualNode myNode = new VirtualNode(id, neighbours.split(":"));

        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run() {
                myNode.close();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e){}
            }
        });

        String[] cmd;
        boolean loop = true;
        while (loop) {
            System.out.print("#~~>> ");
            cmd = s.nextLine().split(" ");

            if (cmd[0].compareTo("sendl") == 0) {
                myNode.sendLeft(cmd[1]);
            }

            else if (cmd[0].compareTo("sendr") == 0) {
                myNode.sendRight(cmd[1]);
            }

            else if (cmd[0].compareTo("neigh") == 0) {
                myNode.printNeighbours();
            }

            else if (cmd[0].compareTo("--exit") == 0) {
                loop = false;
            }

            else if (cmd[0].compareTo("table") == 0)
                myNode.physLayer.table.printTable();

            else {
                System.out.println("Unknown commmand <" + cmd[0] + ">");
            }

        }
        s.close();
        myNode.close();
        myNode.join();
        Thread.sleep(1000);
        System.exit(1);
    }
}
