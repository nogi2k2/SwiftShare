package p2p.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import p2p.utils.UploadUtils;

public class FileSharer {
    private HashMap<Integer, String> availableFiles;

    public FileSharer(){
        this.availableFiles = new HashMap<>();
    }

    public int offerFile(String filePath){
        int port;
        while (true){
            port = UploadUtils.generateCode();
            if (!availableFiles.containsKey(port)){
                availableFiles.put(port, filePath);
                return port;
            }
        }
    }

    public void startFileServer(int port){
        String filePath = availableFiles.get(port);
        if (filePath == null){
            System.err.println("No file being served on port: " + port);
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("Serving file " + new File(filePath).getName() + " on port " + port);
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: "+ clientSocket.getInetAddress());

            new Thread(new FileSenderHandler(clientSocket, filePath)).start();
        } catch (IOException e) {
            System.err.println("Error starting the server on port " + port + ": " + e.getMessage());
        }
    }

    private static class FileSenderHandler implements Runnable{
        private final String filePath;
        private final Socket clientSocket;

        public FileSenderHandler(Socket clientSocket, String filePath){
            this.filePath = filePath;
            this.clientSocket = clientSocket;
        }

        @Override
        public void run(){
            try (FileInputStream fis = new FileInputStream(filePath);
                 OutputStream oos = clientSocket.getOutputStream()){

                String filename = new File(filePath).getName();
                String header = "Filename: " + filename + "\n";
                oos.write(header.getBytes());

                byte buffer[] = new byte[4096];
                int bytesRead;
                while((bytesRead = fis.read(buffer)) != -1){
                    oos.write(buffer, 0, bytesRead);
                }
                System.out.println("File " + filename + " sent to " + clientSocket.getInetAddress());

            } catch (IOException e) {
                System.err.println("Error sending file to client: " + e.getMessage());
            }finally{
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }
}
