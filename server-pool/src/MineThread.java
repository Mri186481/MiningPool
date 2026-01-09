import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MineThread extends Thread implements PropertyChangeListener {
/*Al igual que La clase EchoThread es fundamentalmente un hilo de servidor responsable de manejar la conexión individual
de un cliente minero, procesar sus comandos (connect, ack, sol) y notificarle los nuevos mensajes del chat.
Combina la funcionalidad de redes (Sockets) con el patrón de observador (PropertyChangeListener).
*/

    private Socket client;
    private Miners miners;
    // ID para calcular el rango (ej: minero 1 rango 0-100)
    private int minerId;

    private BufferedReader in;
    private PrintWriter out;
    private boolean running = true;

    // Constante para el tamaño del rango de búsqueda, ahora mejor dinamica
    //private static final int RANGE_SIZE = 1000;

    public MineThread(Socket client, Miners miners, int minerId) {
        this.client = client;
        this.miners = miners;
        this.minerId = minerId;

        // Asi nos suscribimos a los eventos de Miners (cuando se genere un bloque)
        miners.addPropertyChangeListener(this);
    }
    /*
    Constructor MineThread:
        1. Recibe el Socket (la conexión específica del cliente) ,una referencia al objeto Miners global y el id del minero.
        2. Llama a miners.addPropertyChangeListener(this): Esto registra el MineThread como un escuchador del objeto Miners.
           A partir de este momento, cada vez que el Miners cambie el método propertyChange de este hilo será invocado.
     */

    @Override
    public void run() {
        try {
            //El metodo run es el procesamiento de la conexion.
            //Configura los flujos de entrada (in, para leer lo que envía el cliente) y salida
            // (out, para enviarle datos al cliente). El true en PrintWriter habilita el auto-flush,
            // asegurando que los mensajes se envíen inmediatamente.
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
            // Limpieza al desconectar, SIEMPRE se ejecuta, es una red de seguridad, no importa
            //que termine bien o mal haY QUE HACER LO MISMO EN LOS DOS CASOS.
            //Borra al ususario de la lista lógica
            miners.removeMiner(this);
            //Se quita de notificaciones, así se evita en caso de error a ususarios que no existan
            miners.removePropertyChangeListener(this);
            //cierra la conexion fisica, asi se evita que el servidor se quede sin puertos disponibles
            try { client.close(); } catch (IOException e) {}
        }
    }
    //Esto viene de cada cliente/minero individual
    private void processMessage(String msg) {
        //Este método analiza el mensaje que llega del cliente y realiza una acción basada en el prefijo:
        // PROTOCOLO
        // 1. Cliente se conecta
        if (msg.startsWith("connect")) {
            // Server responde: ack <total_clients> total clients
            int total = miners.getMinerCount();
            out.println("ack " + total + " total clientes");

            // OJO: Para probar, fuerzo el inicio de una ronda de minado al conectarse el 1, 2º cliente
            // Esto es para ver que funciona
            //if (total >= 1) {
            if (total == 1) {
            // Solo si es el PRIMER cliente (total == 1), arrancamos la primera ronda.
            // Las siguientes rondas las hará el temporizador de Miners cada 5 mins.
                System.out.println("[SERVER] Iniciando ronda de minado...");
                miners.startNewMiningRound();
            }
            // Si entra el cliente nº 2, 3, etc., se unen a la ronda que ya esté en marcha
            // o esperan a la siguiente del temporizador.
        }

        // 2. Cliente confirma recepción de trabajo, QUE EL SERVIDOR HA PEDIDO BUSCAR UNA SOLUCION
        else if (msg.startsWith("ack")) {
            System.out.println("[MINER " + minerId + "] ack Minero comienza a trabajar.");
        }

        // 3. Cliente envía solución
        else if (msg.startsWith("sol")) {
            // Msg ejemplo: "sol 3654"
            String solution = msg.split(" ")[1];
            //Se muestra la solucion y avisamos a todos para que paren ("end")
            miners.notifySolutionFound(minerId, solution);
        }
    }

    //Este método se dispara cuando Miners hace firePropertyChange.
    //     * Aquí es donde se envia EL PAQUETE AL CLIENTE., el sistema envia a TODOS
    //Miners (el gestor) actúa como una radio emisora.
    //MineThread es una radio receptora
    //El evento: evt es la señal que llega.
    //eventName: Es el "título" de la noticia. Sirve para diferenciar si el server está mandando trabajo (NEW_REQUEST)
    // o mandando parar (END_MINING).
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String eventName = evt.getPropertyName();

        if ("NEW_REQUEST".equals(eventName)) {
            //Recupera el paqute de datos, en este caso las transacciones
            String payload = (String) evt.getNewValue();

            //NUEVO FORMATO: new_request <dificultad> <rango> <datos> new_request 0000 0-100 mv|10|a1|b2;...
            // Con esto se recupera la dificultad actual del objeto Miners ---
            int diff = miners.getDifficulty();

            // rango para cada cliente específico.
            // Ejemplo: Minero 1 -> 0-1000, Minero 2 -> 1001-2000
            // En funcion de la dificultad se calcula el tamaño del rango
            // Si es fácil (2 ceros), rango pequeño (1000).
            // Si es difícil (4 ceros), rango enorme (100.000) para asegurar que haya solución posible.
            int currentRangeSize;
            switch (diff) {
                case 2: currentRangeSize = 1000; break;     // Probabilidad 1/256
                case 3: currentRangeSize = 10000; break;    // Probabilidad 1/4096
                case 4: currentRangeSize = 100000; break;   // Probabilidad 1/65536
                default: currentRangeSize = 1000; break;
            }
            int startRange = (minerId - 1) * currentRangeSize;
            int endRange = startRange + currentRangeSize;

            // mensaje final, empaqueta el mensaje y lo envia al cliente
            // --- NUEVO FORMATO: new_request <dificultad> <rango> <datos>
            String messageToSend = "new_request " + diff + " " + startRange + "-" + endRange + " " + payload;
            //ANtes no habia dificlcutad ===> String messageToSend = "new_request " + startRange + "-" + endRange + " " + payload;

            out.println(messageToSend);
            System.out.println("[MINER " + minerId + "] Enviado: " + messageToSend);
        }

        else if ("END_MINING".equals(eventName)) {
            // Recupero el string que enviamos desde Miners
            String infoVictoria = (String) evt.getNewValue();
            // Alguien encontró la solución, mandamos parar.
            out.println("end " + infoVictoria);
        }
    }
}
