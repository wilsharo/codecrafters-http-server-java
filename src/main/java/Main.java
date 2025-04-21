import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  private static final int PORT = 4221; // Choose a suitable port
  private static final int MAX_THREADS = 10; // Adjust based on your needs
  private static String directoryString;

  public static void main(String[] args) {
    ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS);

    // Parse command line arguments
    if (args.length > 1 && args[0].equals("--directory")) {
      directoryString = args[1];
    }

    try (ServerSocket serverSocket = new ServerSocket(PORT)) {

      // Since the tester restarts the program quite often, setting SO_REUSEADDR
      // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);
      System.out.println("Server started on port " + PORT);

      while (true) {
        try {
          // Block until a client connects
          Socket clientSocket = serverSocket.accept();
          System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

          // Create a Runnable task to handle the client connection
          ClientHandler clientHandler = new ClientHandler(clientSocket, directoryString);

          // Submit the task to the thread pool for execution
          executorService.submit(clientHandler);

        } catch (IOException e) {
          System.err.println("Error accepting client connection: " + e.getMessage());
        }
      }
    } catch (IOException e) {
      System.err.println("Could not start server on port " + PORT + ": " + e.getMessage());
    } finally {
      // Shutdown the executor service when the server socket is closed or an error
      // occurs
      if (executorService != null) {
        executorService.shutdown();
      }
    }
  }
}