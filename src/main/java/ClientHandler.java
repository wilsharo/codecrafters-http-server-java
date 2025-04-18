import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientHandler implements Runnable {
  private final Socket clientSocket;

  public ClientHandler(Socket socket) {
    this.clientSocket = socket;
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
      // String method = requestParts[0]; // GET, POST etc
      String path = requestParts[1];

      DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

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
        StringBuilder request = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null) {
          request.append(line).append("\r\n");
          // Check for end of headers (empty line)
          if (line.isEmpty())
            break;
        }

        String fullRequest = request.toString();

        System.out.println("fullRequest: " + fullRequest);

        int userAgentIndex = fullRequest.indexOf("User-Agent");

        String userAgent = request.toString().substring(userAgentIndex + 12).trim();

        System.out.println("userAgent: " + userAgent);

        int userAgentLength = userAgent.length();

        out.write(
            ("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                userAgentLength + "\r\n\r\n" + userAgent)
                .getBytes());
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