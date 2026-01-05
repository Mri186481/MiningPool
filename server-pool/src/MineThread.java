import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MineThread extends Thread implements PropertyChangeListener {

    private Socket client;
    private Miners miners;
    // ID para calcular el rango (ej: minero 1 rango 0-100)
    private int minerId;

    private BufferedReader in;
    private PrintWriter out;
    private boolean running = true;

    // Constante para el tamaño del rango de búsqueda
    private static final int RANGE_SIZE = 1000;

    public MineThread(Socket client, Miners miners, int minerId) {
        this.client = client;
        this.miners = miners;
        this.minerId = minerId;

        // Asi nos suscribimos a los eventos de Miners (cuando se genere un bloque)
        miners.addPropertyChangeListener(this);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);

            String msg;
            while (running && (msg = in.readLine()) != null) {
                processMessage(msg);
            }
        } catch (IOException e) {
            System.err.println("Error en MineThread " + minerId + ": " + e.getMessage());
        } finally {
            // Limpieza al desconectar
            miners.removeMiner(this);
            miners.removePropertyChangeListener(this);
            try { client.close(); } catch (IOException e) {}
        }
    }

    private void processMessage(String msg) {
        // PROTOCOLO
        // 1. Cliente se conecta
        if (msg.startsWith("connect")) {
            // Server responde: ack <total_clients> total clients
            int total = miners.getMinerCount();
            out.println("ack " + total + " total clients");

            // OJO: Para probar, fuerzo el inicio de una ronda de minado al conectarse el 2º cliente
            // Esto es para ver que funciona
            if (total >= 1) {
                System.out.println("[THREAD] Iniciando ronda de minado...");
                miners.startNewMiningRound();
            }
        }

        // 2. Cliente confirma recepción de trabajo
        else if (msg.startsWith("ack")) {
            System.out.println("[THREAD " + minerId + "] Cliente listo para trabajar.");
        }

        // 3. Cliente envía solución
        else if (msg.startsWith("sol")) {
            // Msg ejemplo: "sol 3654"
            String solution = msg.split(" ")[1];
            miners.notifySolutionFound(minerId, solution);
        }
    }

    //Este método se dispara cuando Miners hace firePropertyChange.
    //     * Aquí es donde se envia EL PAQUETE AL CLIENTE.
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String eventName = evt.getPropertyName();

        if ("NEW_REQUEST".equals(eventName)) {
            // El servidor ha generado un paquete nuevo.
            String payload = (String) evt.getNewValue();

            // rango para cada cliente específico.
            // Ejemplo: Minero 1 -> 0-1000, Minero 2 -> 1001-2000
            int startRange = (minerId - 1) * RANGE_SIZE;
            int endRange = startRange + RANGE_SIZE;

            // mensaje final
            // new_request 0-100 mv|10|a1|b2;...
            String messageToSend = "new_request " + startRange + "-" + endRange + " " + payload;

            out.println(messageToSend);
            System.out.println("[THREAD " + minerId + "] Enviado: " + messageToSend);
        }

        else if ("END_MINING".equals(eventName)) {
            // Alguien encontró la solución, mandamos parar.
            out.println("end");
        }
    }
}
