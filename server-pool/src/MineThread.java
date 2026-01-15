import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
/*Al igual que La clase EchoThread es fundamentalmente un hilo de servidor responsable de manejar la conexión individual
de un cliente minero, procesar sus comandos (connect, ack, sol) y notificarle los nuevos mensajes del chat.
Combina la funcionalidad de redes (Sockets) con el patrón de observador (PropertyChangeListener).
*/
public class MineThread extends Thread implements PropertyChangeListener {
    private Socket client;
    private Miners miners;
    private int minerId;

    private BufferedReader in;
    private PrintWriter out;
    private boolean running = true;

    public MineThread(Socket client, Miners miners, int minerId) {
        this.client = client;
        this.miners = miners;
        this.minerId = minerId;

        miners.addPropertyChangeListener(this);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);

            //las dos condiciones deben de ser ciertas, que no haya un logout y que no se corte la conexion
            String msg;
            while (running && (msg = in.readLine()) != null) {
                processMessage(msg);
            }
        } catch (IOException e) {
            System.err.println("Error en MineThread " + minerId + ": " + e.getMessage());
        } finally {
            // Limpieza al desconectar, SIEMPRE se ejecuta, no importa que termine bien o mal haY QUE HACER LO MISMO EN LOS DOS CASOS.
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
            out.println("ack " + total + " total clientes");

            if (total == 1) {
            // Solo si es el PRIMER cliente (total == 1), arrancamos la primera ronda.
            // Las siguientes rondas las hará el temporizador de Miners cada 5 mins.
                System.out.println("[SERVER] Iniciando ronda de minado...");
                miners.startNewMiningRound();
            }
            // Si entra el cliente nº 2, 3, etc., se unen a la ronda que ya esté en marcha
            // o esperan a la siguiente del temporizador.
        }

        // 2. Cliente confirma recepción de trabajo
        else if (msg.startsWith("ack")) {
            System.out.println("[MINER " + minerId + "] ack Minero comienza a trabajar.");
        }

        // 3. Cliente envía solución
        else if (msg.startsWith("sol")) {
            String solution = msg.split(" ")[1];
            //Se muestra la solucion y avisamos a todos para que paren ("end")
            miners.notifySolutionFound(minerId, solution);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String eventName = evt.getPropertyName();

        if ("NEW_REQUEST".equals(eventName)) {
            String payload = (String) evt.getNewValue();
            //NUEVO FORMATO: new_request <dificultad> <rango> <datos>; ejemplo: new_request 4 0-100000 0-100 mv|10|a1|b2;...
            int diff = miners.getDifficulty();

            // Rango para cada cliente específico.
            // Ejemplo: Minero 1 -> 0-1000, Minero 2 -> 1001-2000
            // En funcion de la dificultad se calcula el tamaño del rango
            // Si es fácil (2 ceros, probabilidad 1/256), rango pequeño (1000).
            // Si es difícil (4 ceros, probabilidad 1/65536), rango enorme (100.000) para asegurar que haya solución posible.
            int currentRangeSize;
            switch (diff) {
                case 2: currentRangeSize = 1000; break;
                case 3: currentRangeSize = 10000; break;
                case 4: currentRangeSize = 100000; break;
                default: currentRangeSize = 1000; break;
            }
            int startRange = (minerId - 1) * currentRangeSize;
            int endRange = startRange + currentRangeSize;

            // mensaje final, empaqueta el mensaje y lo envia al cliente
            String messageToSend = "new_request " + diff + " " + startRange + "-" + endRange + " " + payload;
            out.println(messageToSend);
            System.out.println("[MINER " + minerId + "] Enviado: " + messageToSend);
        }

        else if ("END_MINING".equals(eventName)) {
            String infoVictoria = (String) evt.getNewValue();
            out.println("end " + infoVictoria);
        }
    }
}
