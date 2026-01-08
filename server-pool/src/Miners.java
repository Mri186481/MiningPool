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

    // Lista de hilos (mineros) conectados para poder gestionarlos
    //Mejora: collections.syncro...envuelve el Arraylist para que la lista sea segura para sea segura para accesos concurrentes
    private final List<MineThread> connectedMiners = Collections.synchronizedList(new ArrayList<>());
    // Soporte para notificaciones (Observer Pattern)
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    // Generador de aleatorios para las transacciones
    private final RandomGenerator random = RandomGenerator.getDefault();
    // RECORD para la estructura de datos
    //Me es mas lógico pensar asi "De quién viene (Origen) -> A quién va (Destino) -> Cuánto (Cantidad)"
    //Pero el formato del servidor exige cantidad primero con toString traducuzco al formato servidor

    //Variable para guardar la dificultad de la ronda actual (por defecto 2) ---
    private int currentDifficulty = 2;

    //Getter para que MineThread sepa qué dificultad enviar al cliente ---
    public int getDifficulty() {
        return currentDifficulty;
    }
    public record Transaccion(String cuentaOrigen, String cuentaDestino, int cantidad) {
        @Override
        public String toString() {
            // Formato pedido: mv|cantidad|origen|destino
            return "mv|%d|%s|%s".formatted(cantidad, cuentaOrigen, cuentaDestino);
        }
    }

    //Validacion del HASh, se guarda el paquete de datos actual para poder validar luego
    private String currentData;

    // --- Gestión de Mineros ---

    //Con synchronized gestiono la concurrencia de los hilos mineros, los bloquea, hace la funcion y sale y lo desbloquea

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
    /*
        Permiten a cualquier otra clase que implemente la interfaz PropertyChangeListener registrarse (o darse de baja)
        para recibir las notificaciones que dispara el objeto Miners a través de pcs.
    */

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
        //Establecer dificultad aleatoria para esta ronda (entre 2 y 4 ceros) ---
        // nextInt(2, 5) devuelve 2, 3 o 4 (nunca llega al 5)
        this.currentDifficulty = ThreadLocalRandom.current().nextInt(2, 5);
        System.out.println("[SERVER] Dificultad establecida para esta ronda: " + this.currentDifficulty + " ceros.");
        // Genero las transacciones aleatorias
        this.currentData = generarPaqueteDatos(5); // Simulamos 5 transacciones

        System.out.println("[SERVER] Generando nuevo bloque: " + this.currentData);

        // Notificamos a todos los listeners (los MineThread)
        // El nombre de la propiedad es "NEW_REQUEST", el valor antiguo null, el nuevo es el PAYLOAD
        pcs.firePropertyChange("NEW_REQUEST", null, this.currentData);
    }

    //Cuando un minero encuentra la solución, tambien tiene que avisar a todos para que paren
    //y VALIDAR la solucion para ver si es correcta
    public synchronized void notifySolutionFound(int minerId, String solutionString) {
        try {
            //Se intenta convertir, si envían "texto" en vez de número, saltará al catch.
            int solution = Integer.parseInt(solutionString);
            //Si la ronda acabó, se ignora y se sale, asi se evita problemas con la condicion de carrera
            if (this.currentData == null) {
                System.out.println("[SERVER] Solución recibida tarde (" + solutionString + "). La ronda ya ha terminado. Ignorando...");
                return;
            }
            System.out.println("String a hashear (Servidor): " + String.format("%03d%s", solution, this.currentData));
            // -------------
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
        // Por seguridad
        if (data == null) return false;


        MessageDigest digest = MessageDigest.getInstance("md5");

        //Se replica exactamente cómo lo hace el cliente: numero + datos, pero sin el for
        //Exactamnete igual, rellenando con ceros a la izquierda 5--->005
        String msg = String.format("%03d%s", solution, data);

        digest.update(msg.getBytes());

        String result = HexFormat.of().formatHex(digest.digest());

        // Validación dinámica basada en la dificultad actual ---
        // Se genera el prefijo (ej: "00". "000" o "0000") según currentDifficulty
        String target = "0".repeat(this.currentDifficulty);

        // Se comprueba si empieza por 00
        if (result.startsWith(target)) {
            return true;  // La validación es correcta
        } else {
            return false; // La validación ha fallado
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
            //Creo el objeto y lo guardo en la lista
            transacciones.add(new Transaccion(origen, destino, cantidad));
        }
        // Unimos con ";" y añadimos el ";" final
        // Esto convierte la lista de objetos en un String largo: "mv|103|user12|user23;mv|248|user33|user4;"
        //Creo un constructor de cadenas
        //StringBuilder del paquet java.langstreambuilder  es como una caja abierta donde vas metiendo cosas eficientemente y solo se cierras al final.
        //Mejor que utilizar String, ya que son inmutables y no se puede cambiar y java tendria que copiar texto viejo
        //añadir texto nuevo, crear objeto nuevo en memoria y borrar el viejo
        StringBuilder sb = new StringBuilder();
        //Cojo el array y recorro la lista de transacciones una por una
        for (Transaccion t : transacciones) {
            //Añado la transacción, append añade (Appends the specified string to this character sequence)
            sb.append(t.toString());
            //Añadimos el punto y coma después de cada una
            sb.append(";");
        }
        //Converto lo que he acumulado a un String final
        return sb.toString();
    }
}
