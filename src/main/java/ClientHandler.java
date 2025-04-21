import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
  private final Socket clientSocket;
  private final String directoryString;

  public ClientHandler(Socket socket, String directoryString) {
    this.clientSocket = socket;
    this.directoryString = directoryString;
  }

  @Override
  public void run() {
    try {

      // Use the InputStream of the Socket to read the client's request. The request
      // is typically an HTTP request, and the path is part of the first line.
      BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String requestLine = in.readLine();

      // Extract the path from the request line. The path is usually the second
      // element when splitting by spaces.
      String[] requestParts = requestLine.split(" ");
      String method = requestParts[0]; // GET, POST etc
      String path = requestParts[1];

      DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

      Map<String, String> headers = new HashMap<>();

      // Read the request headers and add them to a map
      String line;
      while ((line = in.readLine()) != null && !line.isEmpty()) {

        System.out.println("line: " + line);

        int userAgentIndex = line.indexOf("User-Agent");

        if (userAgentIndex != -1) {
          headers.put("User-Agent".toLowerCase(), line.substring(userAgentIndex + 12).trim());
        }

        int contentLengthIndex = line.indexOf("Content-Length");

        if (contentLengthIndex != -1) {
          headers.put("Content-Length".toLowerCase(), line.substring(contentLengthIndex + 16).trim());
        }

        int contentTypeIndex = line.indexOf("Content-Type");

        if (contentTypeIndex != -1) {
          headers.put("Content-Type".toLowerCase(), line.substring(contentTypeIndex + 14).trim());
        }
      }

      if ("/".equals(path)) {
        // Handle request for root path
        out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
      } else if (path.startsWith("/echo/")) {

        String echoeText = path.substring(6);

        out.write(
            ("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                echoeText.length() + "\r\n\r\n" + echoeText)
                .getBytes());
      } else if (path.startsWith("/user-agent")) {

        String userAgent = headers.get("user-agent");

        int userAgentLength = userAgent.length();

        out.write(
            ("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                userAgentLength + "\r\n\r\n" + userAgent)
                .getBytes());
      } else if (path.startsWith("/files/")) {

        if (method.equalsIgnoreCase("post")) {

          int contentLength = Integer.parseInt(headers.get("content-length").trim());

          System.out.println("content length: " + contentLength);

          // Read the request body, if Content-Length is greater than 0
          String requestBody = "";
          if (contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            int bytesRead = in.read(bodyChars); // Use bufferedReader.read
            if (bytesRead > 0) {
              requestBody = new String(bodyChars, 0, bytesRead);
            }
            // Write the request body to a file
            try {
              String fileName = path.substring(7);
              File tempFile = new File(directoryString + "/" + fileName);
              Files.write(Paths.get(tempFile.getAbsolutePath()), requestBody.getBytes());
            } catch (IOException e) {
              System.err.println("Error writing to file: " + e.getMessage());
            }
          } else {
            System.out.println("No body received");
          }

          out.write("HTTP/1.1 201 Created\r\n\r\n".getBytes());

        } else {
          String fileName = path.substring(7);

          String filePathString = directoryString + fileName;

          File directory = new File(directoryString);
          File file = new File(directory, fileName);

          if (file.exists()) {
            // Use Paths.get() for robust path construction
            Path filePath = Paths.get(filePathString);

            String fileContent = new String(Files.readAllBytes(filePath));

            // Get the file size in bytes
            long fileSizeInBytes = Files.size(filePath);

            out.write(
                ("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " +
                    fileSizeInBytes + "\r\n\r\n" + fileContent)
                    .getBytes());
          } else {
            out.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
          }
        }

      } else {
        // Handle request for /other paths
        out.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
      }

    } catch (IOException e) {
      System.err.println("Error handling client connection: " + e.getMessage());
    } finally {
      try {
        clientSocket.close();
      } catch (IOException e) {
        System.err.println("Error closing client socket: " + e.getMessage());
      }
    }
  }
}