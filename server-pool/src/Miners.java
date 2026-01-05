import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
//Miners Genera las transacciones y gestiona el estado del minado
public class Miners {

    // Lista de hilos (mineros) conectados para poder gestionarlos
    private final List<MineThread> connectedMiners = new ArrayList<>();
    // Soporte para notificaciones (Observer Pattern)
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    // Generador de aleatorios para las transacciones
    private final RandomGenerator random = RandomGenerator.getDefault();
    // RECORD para la estructura de datos (Java 21)
    public record Transaccion(String cuentaOrigen, String cuentaDestino, int cantidad) {
        @Override
        public String toString() {
            // Formato pedido: mv|cantidad|origen|destino
            return "mv|%d|%s|%s".formatted(cantidad, cuentaOrigen, cuentaDestino);
        }
    }

    // --- Gestión de Mineros ---

    public synchronized void addMiner(MineThread miner) {
        connectedMiners.add(miner);
    }

    public synchronized void removeMiner(MineThread miner) {
        connectedMiners.remove(miner);
    }

    public int getMinerCount() {
        return connectedMiners.size();
    }

    // --- Lógica del Pool y Notificaciones ---

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    //Este método simula la creación del bloque cada 5 minutos.
    //     * Genera las transacciones y avisa a TODOS los hilos (MineThread)
    //     * para que envíen el "new_request" a sus clientes
    public void startNewMiningRound() {
        // Genero las transacciones aleatorias
        String payload = generarPaqueteDatos(5); // Simulamos 5 transacciones

        System.out.println("[MINERS] Generando nuevo bloque: " + payload);

        // Notificamos a todos los listeners (los MineThread)
        // El nombre de la propiedad es "NEW_REQUEST", el valor antiguo null, el nuevo es el PAYLOAD
        pcs.firePropertyChange("NEW_REQUEST", null, payload);
    }

    //Cuando un minero encuentra la solución
    public void notifySolutionFound(int minerId, String solution) {
        System.out.println("[MINERS] ¡Solución encontrada por Minero " + minerId + "! Sol: " + solution);

        // Avisamos a todos para que paren ("end")
        pcs.firePropertyChange("END_MINING", null, "Winner: " + minerId);
    }

    // --- Generación de Datos

    private String generarPaqueteDatos(int numeroTransacciones) {
        List<Transaccion> transacciones = new ArrayList<>();
        for (int i = 0; i < numeroTransacciones; i++) {
            String origen = "user" + random.nextInt(1, 100);
            String destino = "user" + random.nextInt(1, 100);
            while (origen.equals(destino)) destino = "user" + random.nextInt(1, 100);
            int cantidad = random.nextInt(1, 1001);
            transacciones.add(new Transaccion(origen, destino, cantidad));
        }
        // Unimos con ";" y añadimos el ";" final
        return transacciones.stream()
                .map(Transaccion::toString)
                .collect(Collectors.joining(";")) + ";";
    }
}
