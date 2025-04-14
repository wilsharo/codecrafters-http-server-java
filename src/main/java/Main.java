import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {

    try {
      ServerSocket serverSocket = new ServerSocket(4221);
    
      // Since the tester restarts the program quite often, setting SO_REUSEADDR
      // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);
    
      //store accepted connection in Socket object so that we can ineract with the client
      Socket clienSocket = serverSocket.accept();
      //The server sends an HTTP response to the client by writing directly to the socket's output stream
      //The response consists of the HTTP version, status code, and reason phrase, followed by two CRLF sequences, indicating the end of the status line and headers.
      clienSocket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
      System.out.println("accepted new connection");
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}
