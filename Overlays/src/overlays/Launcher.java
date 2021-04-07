package overlays;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;

public class Launcher {
    public static void main(String[] args) throws Exception {

        LinkedList<PhysicalNode> nodes = new LinkedList<>();

        String[] input;
        String currentLine;
        
        int nbNodes = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./config/" + args[0] + ".txt"));
            while ((currentLine = reader.readLine()) != null) {
                input = currentLine.split(":");
                nodes.add(new PhysicalNode(nbNodes++, input));
            }
            
            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        for (PhysicalNode n : nodes){
            n.start();
            n.join();
        }

        Thread.sleep(2000);

        for (PhysicalNode n : nodes){
            n.table.printTable();
        }
            


        //System.exit(0);

    }
}
