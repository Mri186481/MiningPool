import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Main {
// Añdo mecanismo de concurrencia en el cliente para la busqueda de soluciones
//Para ello voy a paralelizar el trabajo en los nucleos que tenga la cpu del cliente
//asi cada cliente puede buscar en varios sitios a la vez dentro de su rango, para ello tengo:
//1.Detectar cuántos núcleos tiene la CPU.
//2.Dividir el rango que nos da el servidor (ej: 0-100.000) en trocitos iguales para cada núcleo.
//3.Lanzar varios hilos a la vez para que busquen en paralelo.

    //Estas variables sean accesibles desde los métodos auxiliares
    private static PrintWriter out;
    //En lugar de un solo hilo (minerThread), uso una lista de hilos ---
    // private static Thread minerThread;
    private static List<Thread> minerThreads = new ArrayList<>();
    //volatile asegura que los cambios se vean inmediatamente entre hilos
    private static volatile boolean iWon = false;

    public static void main(String[] args) {
        System.out.println("--- CLIENTE MINERO MULTIHILO INICIADO ---");
        //Envio informacion de cuántos núcleos vamos a usar
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println(">> Detectados " + cores + " núcleos para minar.");
        try {
            //CONEXIÓN
            Socket client = new Socket();
            client.connect(new InetSocketAddress("localhost", 3000));

            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);

            // PROTOCOLO: Identificacion, mando connect
            out.println("connect");

            //BUCLE DE ESCUCHA
            // El cliente no se cierra, se queda esperando órdenes del servidor
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                processMessage(serverMessage);
            }

            // Si se sale del while es que el servidor se cerró
            client.close();

        } catch (IOException e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }

    private static void processMessage(String msg) {
        System.out.println("[RECIBIDO] " + msg);

        if (msg.startsWith("ack")) {
            System.out.println(">> Conexión aceptada por el pool.");
        }
        else if (msg.startsWith("new_request")) {
            iWon = false;
            //Se confirma que el servidor ha peddo buscar una solucion
            out.println("ack");
            // El servidor manda trabajo: "new_request 0-100 mv|10|a|b;"
            handleNewWork(msg);
        }
        else if (msg.startsWith("end")) {
            // El servidor dice que paremos (otro ganó)
            if (iWon) {
                System.out.println(">> ¡VICTORIA! El servidor confirma que he ganado la ronda.");
                iWon = false;
            } else {
                System.out.println(">> OTRO MINERO GANÓ. Parando mi trabajo...");
            }
            stopMining();
        }
    }

    // Prepara los datos y lanza el hilo minero
    private static void handleNewWork(String msg) {
        // --- NUEVO FORMATO DE PARSEO ---
        // msg viene ahora así: "new_request 3 0-100 mv|datos..."
        // Partimos el mensaje por espacios
        String[] parts = msg.split(" ");

        // parts[1] es ahora la dificultad
        int dificulty = Integer.parseInt(parts[1]);

        // partes[2] es el rango "0-100"
        String[] ranges = parts[2].split("-");
        int minGlobal = Integer.parseInt(ranges[0]);
        int maxGlobal = Integer.parseInt(ranges[1]);

        // partes[3] son los datos "mv|..."
        String data = parts[3];

        System.out.println(">> ¡A MINAR! Dificultad: " + dificulty + " ceros. Rango: " + minGlobal + " a " + maxGlobal);

        // Por si acaso había uno viejo corriendo
        stopMining();

        // Calcular división del trabajo
        int numThreads = Runtime.getRuntime().availableProcessors();
        int totalItems = maxGlobal - minGlobal;
        // Tamaño del trozo para cada hilo
        int chunkSize = totalItems / numThreads;
        // Aviso informativo de la paralelizacion efectuada
        System.out.println(">> Lanzando " + numThreads + " hilos (cada uno procesará ~" + chunkSize + " hashes).");
        // 3. Crear y lanzar los hilos
        for (int i = 0; i < numThreads; i++) {
            // Calcular sub-rango para el hilo 'i'
            int startRange = minGlobal + (i * chunkSize);

            // El último hilo se lleva "lo que sobre" hasta el final para no perder decimales
            int endRange;

            // Compruebo si este es el ÚLTIMO hilo de la lista
            if (i == numThreads - 1) {
                // Si soy el último, me quedo con hasta el final real (max)
                // Esto recoge los "restos" de la división inexacta
                endRange = maxGlobal;
            } else {
                // Si NO soy el último, cojo solo mi trozo normal
                // (start + tamaño) - 1 porque el rango es inclusivo
                endRange = startRange + chunkSize - 1;
            }

            // Crear el hilo
            Thread t = new Thread(() -> {
                try {
                    // Llamada a HashCalculator
                    int solution = HashCalculator.calculateHash(data, startRange, endRange, dificulty);

                    // Si encuentra solución y nadie me ha manadado parar mientras yo estaba calculando  y nadie ha ganado todavía...
                    if (solution != -1 && !Thread.currentThread().isInterrupted() && !iWon) {
                        // Doble check sincronizado para evitar que dos hilos del mismo cliente envíen a la vez
                        synchronized (Main.class) {
                            if (!iWon) {
                                iWon = true;
                                //Obtengo el nombre del hilo que ha ganado (ej: "Hilo-3") ---
                                String threadName = Thread.currentThread().getName();
                                //Lo muestro en el mensaje ---
                                System.out.println(">> ¡ENCONTRADO por " + threadName + "! Sol: " + solution);
                                out.println("sol " + solution);
                                // Parar a mis propios hermanos hilos
                                stopMining();
                            }
                        }
                    }

                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            });

            // Añadir a la lista y arrancar
            minerThreads.add(t);
            t.start();
        }
    }

    private static void stopMining() {
        // Interrumpir TODOS los hilos de la lista
        for (Thread t : minerThreads) {
            if (t != null && t.isAlive()) {
                t.interrupt();
            }
        }
        minerThreads.clear();
    }
}