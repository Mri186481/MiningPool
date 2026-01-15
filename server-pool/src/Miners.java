import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.concurrent.ThreadLocalRandom;

//Miners Genera las transacciones y gestiona el estado del minado
public class Miners {


    private final List<MineThread> connectedMiners = Collections.synchronizedList(new ArrayList<>());
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final RandomGenerator random = RandomGenerator.getDefault();
    private int currentDifficulty = 2;

    public int getDifficulty() {
        return currentDifficulty;
    }
    //RECORD para la estructura de datos
    public record Transaccion(String cuentaOrigen, String cuentaDestino, int cantidad) {
        @Override
        public String toString() {
            // Formato pedido: mv|cantidad|origen|destino
            return "mv|%d|%s|%s".formatted(cantidad, cuentaOrigen, cuentaDestino);
        }
    }

    private String currentData;

    //Constructor para iniciar el temporizador automático
    public Miners() {
        Thread temporizador = new Thread(() -> {
            while (true) {
                try {
                    // Esperamos 5 minutos (300,000 milisegundos), para probar poner 30000(30sg)
                    Thread.sleep(300000);
                    // Solo mandamos trabajo si hay alguien conectado para hacerlo
                    // Si la lista no está vacía, iniciamos ronda.
                    if (!connectedMiners.isEmpty()) {
                        System.out.println("[AUTO-TIMER] Pasaron 5 min. Iniciando nueva ronda automática...");
                        startNewMiningRound();
                    } else {
                        System.out.println("[AUTO-TIMER] Pasaron 5 min, pero no hay mineros conectados. Esperando...");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        temporizador.setDaemon(true);
        temporizador.start();
    }

    //Gestión de Mineros

    public synchronized void addMiner(MineThread miner) {
        connectedMiners.add(miner);
    }

    public synchronized void removeMiner(MineThread miner) {
        connectedMiners.remove(miner);
    }

    public int getMinerCount() {
        return connectedMiners.size();
    }

    //Lógica del Pool y Notificaciones

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    //Creacion del Bloque

    public void startNewMiningRound() {
        this.currentDifficulty = ThreadLocalRandom.current().nextInt(2, 5);
        System.out.println("[SERVER] Dificultad establecida para esta ronda: " + this.currentDifficulty + " ceros.");
        this.currentData = generarPaqueteDatos(5);
        System.out.println("[SERVER] Generando nuevo bloque: " + this.currentData);
        pcs.firePropertyChange("NEW_REQUEST", null, this.currentData);
    }


    public synchronized void notifySolutionFound(int minerId, String solutionString) {
        try {
            int solution = Integer.parseInt(solutionString);

            if (this.currentData == null) {
                System.out.println("[SERVER] Solución recibida tarde (" + solutionString + "). La ronda ya ha terminado. Ignorando...");
                return;
            }

            System.out.println("String a hashear (Servidor): " + String.format("%03d%s", solution, this.currentData));
            if (validate(this.currentData, solution)) {
                System.out.println("[MINERS] ¡Solución encontrada por Minero " + minerId + "! Sol: " + solutionString);
                String datosVictoria = "El minero " + minerId + " ha encontrado la solucion: " + solutionString;
                // Avisamos a todos para que paren ("end")
                pcs.firePropertyChange("END_MINING", null, datosVictoria);
                this.currentData = null;
            } else {
                System.out.println("[MINERS] Solución INCORRECTA rechazada.");
            }
            } catch (NumberFormatException e) {
                System.err.println("[ERROR] El minero " + minerId + " envió un formato de número inválido: " + solutionString);
            } catch (NoSuchAlgorithmException e) {
                System.err.println("[ERROR] Algoritmo de hash no disponible en el servidor.");
            }
    }

    private boolean validate(String data, int solution) throws NoSuchAlgorithmException {
        if (data == null) return false;
        MessageDigest digest = MessageDigest.getInstance("md5");
        //Se replica exactamente cómo lo hace el cliente
        String msg = String.format("%03d%s", solution, data);
        digest.update(msg.getBytes());
        String result = HexFormat.of().formatHex(digest.digest());
        //Validación dinámica basada en la dificultad actual
        //Se genera el prefijo (ej: "00". "000" o "0000") según currentDifficulty
        String target = "0".repeat(this.currentDifficulty);
        if (result.startsWith(target)) {
            return true;
        } else {
            return false;
        }
    }

    // --- Generación de Datos

    private String generarPaqueteDatos(int numeroTransacciones) {
        List<Transaccion> transacciones = new ArrayList<>();
        for (int i = 0; i < numeroTransacciones; i++) {
            String origen = "user" + random.nextInt(1, 100);
            String destino = "user" + random.nextInt(1, 100);
            while (origen.equals(destino)) {
                // Si son iguales, genera un nuevo destino
                destino = "user" + random.nextInt(1, 100);
            }
            int cantidad = random.nextInt(1, 1001);

            transacciones.add(new Transaccion(origen, destino, cantidad));
        }

        StringBuilder sb = new StringBuilder();
        for (Transaccion t : transacciones) {
            sb.append(t.toString());
            sb.append(";");
        }
        //Converto lo que he acumulado a un String final "mv|103|user12|user23;mv|248|user33|user4;..."
        return sb.toString();
    }
}