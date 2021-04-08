package overlays;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.Scanner;

public class Launcher {
    public static void main(String[] args) throws Exception {

        LinkedList<VirtualNode> nodes = new LinkedList<>();
        Scanner s = new Scanner(System.in);

        String[] inputV, inputP;
        String currentVLine,
               currentPLine;

        VirtualNode myNode = null;
        
        int id = Integer.parseInt(args[2]);
        try {
            BufferedReader virtReader = new BufferedReader(new FileReader("./config/" + args[0] + ".txt"));
            BufferedReader physReader = new BufferedReader(new FileReader("./config/" + args[1] + ".txt"));

            int i = 0;
            while(i++ < id) {
                virtReader.readLine();
                physReader.readLine();
            }
            currentVLine = virtReader.readLine();
            currentPLine = physReader.readLine();
            inputV = currentVLine.split(":");
            inputP = currentPLine.split(":");

          
            myNode = new VirtualNode(id, inputV, inputP);
        
            virtReader.close();
            physReader.close();


        } catch (Exception e) {
            e.printStackTrace();
        }

        String[] cmd;
        boolean loop = true;
        while (loop) {
            System.out.print("#~~>> ");
            cmd = s.nextLine().split(" ");
            System.out.println(cmd[0]);

            if (cmd[0].compareTo("sendl") == 0) {
                myNode.sendLeft(cmd[1]);
            }

            else if (cmd[0].compareTo("sendr") == 0) {
                myNode.sendRight(cmd[1]);
            }

            else if (cmd[0].compareTo("--exit") == 0) {
                loop = false;
            }

            else {
                System.out.println("Unknown commmand <" + cmd[0] + ">");
            }

        }

        // for (PhysicalNode n : nodes){
        //     n.start();
        //     n.join();
        // }

        // Thread.sleep(2000);

        // /*for (PhysicalNode n : nodes){
        //     n.ready = true;
        // }*/

        // for (PhysicalNode n : nodes){
        //     n.table.printTable();
        // }
            


        //System.exit(0);

    }
}
