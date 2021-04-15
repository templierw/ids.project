package overlays;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

public class Launcher {

    public enum Cmd {
        SENDL,
        SENR
    }
    public static void main(String[] args) throws Exception {

        /*Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Closing client safely");
            try {
                server.remove(this);
            }
            catch (Exception ignored) {}
        }));*/

        Scanner s = new Scanner(System.in);

        VirtualNode myNode = null;
        
        int id = Integer.parseInt(args[1]);
        try {
            BufferedReader physReader = new BufferedReader(
                    new FileReader("./config/" + args[0] + ".txt")
                );

            int i = 0;
            while(i++ < id) 
                physReader.readLine();

            myNode = new VirtualNode(
                            id, physReader.readLine().split(":")
                            );
        
            physReader.close();

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

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
