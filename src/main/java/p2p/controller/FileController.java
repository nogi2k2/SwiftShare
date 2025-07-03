package p2p.controller;

import p2p.service.FileSharer;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.Socket;
import java.net.InetSocketAddress;

import org.apache.common.io.IOUtils;
public class FileController {

    private final FileSharer fileSharer;
    private final HttpServer server;
    private final String uploadDir;
    private final ExecutorService executorService;

    public FileController(int port) throws IOException{
        this.fileSharer = new FileSharer();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.uploadDir = System.getProperty("Java.io.tmpdir") + File.separator + "swiftshare-uploads";
        this.executorService = Executors.newFixedThreadPool(10);

        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()){
            uploadDirFile.mkdirs();
        }

        server.createContext("/upload", new UploadHandler());
        server.createContext("/download", new DownloadHandler());
        server.createContext("/", new CORSHandler());
        server.setExecutor(executorService);
    }

    public void start(){
        server.start();
        System.out.println("Server started on port "+ server.getAddress().getPort());
    }

    public void stop(){
        server.stop(0);
        executorService.shutdown();
        System.out.println("Server stopped");
    }

    private class CORSHandler implements HttpHandler{
        @Override 
        public void handle(HttpExchange exchange) throws IOException{
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Aceess-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type,Authorization");

            if (!exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")){
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String response = "Not Found";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()){
                os.write(response.getBytes());
            } catch (IOException e) {
                System.err.println();
            }
        }
    }
    
    private static class MultipartParser{

        private final byte[] data;
        private final String boundary;

        public MultipartParser(byte[] data, String boundary){
            this.data = data;
            this.boundary = boundary;
        }

        public ParseResult parse(){
            try {
                String dataAsString = new String(data);

                String filenameMarker = "filename=\"";
                int filenameStart = dataAsString.indexOf(filenameMarker);
                if (filenameStart == -1){return null;}
                filenameStart += filenameMarker.length();
                int filenameEnd = dataAsString.indexOf("\"", filenameStart);
                String filename = dataAsString.substring(filenameStart, filenameEnd);

                String contentTypeMarker = "Content-Type: ";
                int contentTypeStart = dataAsString.indexOf(contentTypeMarker, filenameEnd);
                String contentType = "application/octet-stream";

                if (contentTypeStart != -1){
                    contentTypeStart += contentTypeMarker.length();
                    int contentTypeEnd = dataAsString.indexOf("\r\n", contentTypeStart);
                    contentType = dataAsString.substring(contentTypeStart, contentTypeEnd);
                }

                String headerEndMarker = "\r\n\r\n";
                int contentStart = dataAsString.indexOf(headerEndMarker);
                if (contentStart == -1){
                    return null;
                }
                contentStart += headerEndMarker.length();
                byte[] boundaryBytes = ("\r\n--" + boundary + "--").getBytes();
                int contentEnd = findSequence(data, boundaryBytes, contentStart);

                if (contentEnd == -1){
                    boundaryBytes = ("\r\n--" + boundary).getBytes();
                    contentEnd = findSequence(data, boundaryBytes, contentStart);
                }

                if (contentEnd == -1 || contentEnd <= contentStart){return null;}

                byte[] fileContent = new byte[contentEnd - contentStart];
                System.arraycopy(data, contentStart, fileContent, 0, fileContent.length);

                return new ParseResult(filename, contentType, fileContent);
            } catch (Exception e) {
                System.err.println("Error parsing multipart data: " + e.getMessage());
                return null;
            }
        }

        private int findSequence(byte[] data, byte[] sequence, int startPos){
            outer:
            for (int i=startPos; i<data.length - sequence.length; i++){
                for (int j=0; j<sequence.length; j++){
                    if (data[i+j] != sequence[j]){continue outer;}
                }
                return i;
            }
            return -1;
        }

        public static class ParseResult{
            private final String filename;
            private final String contentType;
            private final byte[] fileContent;

            public ParseResult(String filename, String contentType, byte[] fileContent){
                this.filename = filename;
                this.contentType = contentType;
                this.fileContent = fileContent;
            }
        }
    }

    private class UploadHandler implements HttpHandler{}
    
    private class DownloadHandler implements HttpHandler{}

}
