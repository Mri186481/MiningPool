import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
//MineServer mantiene el bucle infinito de aceptar conexiones y añade cada cleinte a la lista de gestion
public class MineServer {

    public void start(int port) throws IOException {
        System.out.println("[SERVER] Mining Pool Server iniciado en puerto " + port);
        ServerSocket serverSocket = new ServerSocket(port);

        // Instancio el gestor del Pool (lo que en clase es la sala de chat)
        Miners miners = new Miners();

        while (true) {
            System.out.println("[SERVER] Esperando mineros...");
            Socket client = serverSocket.accept();
            System.out.println("[SERVER] Nuevo minero conectado: " + client.getInetAddress());
            // Asigno un ID único al minero para calcular sus rangos (0-100, 101-200...)
            int minerId = miners.getMinerCount() + 1;
            // Creo el hilo para ese cliente, le pasamos el gestor (miners)
            MineThread thread = new MineThread(client, miners, minerId);
            // Lo añado al gestor
            miners.addMiner(thread);
            thread.start();
        }
    }
}
