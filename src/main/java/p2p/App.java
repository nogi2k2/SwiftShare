package p2p;

import java.io.IOException;

import p2p.controller.FileController;
public class App {
    public static void main(String[] args) {
        try {
            FileController fileController = new FileController(8080);
            fileController.start();

            System.out.println("SwiftShare server started on port 8080");
            System.out.println("Application available at http://localhost:3000");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down Http Server");
                fileController.stop();
            }));

            System.out.println("Press Enter to shutdown the server");
            System.in.read();
            fileController.stop();

        } catch (IOException e) {
            System.err.println("Error starting Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
