package overlays;

public class Launcher {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello, World!" + args[0]);

        // get matrix

        // create RoutingNode
        RoutingNode router = new RoutingNode();
        VirtualNode vNode = new VirtualNode();

        router.start();
        vNode.start();

        // create N Virtual node

        // ....

    }
}
