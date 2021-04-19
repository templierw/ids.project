package overlay;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

import overlay.utils.ShellColour;

public class Launcher {

    public static void printHelp(boolean full) {
        if(full)
            System.out.println(
                ShellColour.ANSI_CYAN + "\n\t\tWelcome to Overlay!\n\n" +
                ShellColour.ANSI_GREEN + "\t to execute the program: " +
                ShellColour.ANSI_WHITE + "node.jar <topology> <id> [-v]\n" +
                ShellColour.ANSI_RESET 
            );

        System.out.println(
            ShellColour.ANSI_PURPLE + "sendl <message> OR sendr <message> " +
            ShellColour.ANSI_WHITE + "to send a message to one of your neighbours\n" +
            ShellColour.ANSI_PURPLE + "--table " + 
            ShellColour.ANSI_WHITE + "to see your routing table\n" +
            ShellColour.ANSI_PURPLE + "--help " + 
            ShellColour.ANSI_WHITE + "to see this message again\n" +
            ShellColour.ANSI_PURPLE + "--neigh " + 
            ShellColour.ANSI_WHITE + "to see your neighbours' status (verbose mode only)\n" +
            ShellColour.ANSI_PURPLE + "--exit " + 
            ShellColour.ANSI_WHITE + "to... exit" +
            ShellColour.ANSI_RESET
        );
    }
    public static void main(String[] args) throws Exception {

        boolean verbose = false;
        // parse arguments

        try {
            if (args.length < 2 || args.length > 3)
                throw new Exception("Illegal number of arguments");

            if ((args.length == 3))
                if ((args[2].compareTo("-v") != 0))
                    throw new Exception("Illegal arguments");
                else verbose = true;

        } catch (Exception e) {
            System.err.println(e.getMessage());
            printHelp(true);
            System.exit(-1);
        }

        int id = 0;
        try {
            id = Integer.parseInt(args[1]);
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid ID format, must be integer");
            printHelp(true);
            System.exit(-1);
        }

        String neighbours = "";
        try {
            BufferedReader physReader = new BufferedReader(new FileReader("./topologies/" + args[0] + ".txt"));

            int i = 0;
            while (i++ < id & i < 20)
                physReader.readLine();

            neighbours = physReader.readLine();

            if (neighbours == null)
                throw new Exception("Invalid file or ID");

            physReader.close();

        } catch (Exception e) {
            System.err.println(e.getMessage());
            printHelp(true);
            System.exit(-1);
        }


        try {
            if (neighbours.split(":").length < id)
                throw new Exception("Invalid identifier number");
        } catch (NumberFormatException e) {
            System.err.println(e.getMessage());
            printHelp(true);
            System.exit(-1);
        }

        final VirtualNode myNode = new VirtualNode(id, neighbours.split(":"), verbose);

        /*Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                myNode.close();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        });*/

        Scanner s = new Scanner(System.in);
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

            else if (cmd[0].compareTo("--neigh") == 0) {
                if (!verbose)
                    System.out.println(
                        ShellColour.ANSI_RED + "\tAvailable in verbose mode only..." +
                        ShellColour.ANSI_RESET
                    );
                myNode.printNeighbours();
            }

            else if (cmd[0].compareTo("--exit") == 0) {
                loop = false;
            }

            else if (cmd[0].compareTo("--table") == 0)
                myNode.physLayer.table.printTable();

            else if (cmd[0].compareTo("--help") == 0)
                printHelp(true);    

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
