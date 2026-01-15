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
//asi cada cliente puede buscar en varios sitios a la vez dentro de su rango, para ello:
//Detecto cuántos núcleos tiene la CPU.
//Divido el rango que nos da el servidor (ej: 0-100.000) en trocitos iguales para cada núcleo.
//Lanzo varios hilos a la vez para que busquen en paralelo.

    private static PrintWriter out;
    // private static Thread minerThread;
    private static List<Thread> minerThreads = new ArrayList<>();
    private static volatile boolean iWon = false;

    public static void main(String[] args) {
        System.out.println("--- CLIENTE MINERO MULTIHILO INICIADO ---");
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println(">> Detectados " + cores + " núcleos para minar.");
        try {
            //CONEXIÓN
            Socket client = new Socket();
            client.connect(new InetSocketAddress("localhost", 3000));

            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);

            //PROTOCOLO: Identificacion, mando connect
            out.println("connect");

            //BUCLE DE ESCUCHA
            //El cliente no se cierra, se queda esperando órdenes del servidor
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                processMessage(serverMessage);
            }

            //Si se sale del while es que el servidor se cerró
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
            out.println("ack");
            handleNewWork(msg);
        }
        else if (msg.startsWith("end")) {
            if (iWon) {
                System.out.println(">> ¡VICTORIA! El servidor confirma que he ganado la ronda.");
                iWon = false;
            } else {
                System.out.println(">> OTRO MINERO GANÓ. Parando mi trabajo...");
            }
            stopMining();
        }
    }

    private static void handleNewWork(String msg) {
        String[] parts = msg.split(" ");
        //parts[1] es la dificultad
        int dificulty = Integer.parseInt(parts[1]);
        //partes[2] es el rango "0-1000"
        String[] ranges = parts[2].split("-");
        int minGlobal = Integer.parseInt(ranges[0]);
        int maxGlobal = Integer.parseInt(ranges[1]);
        //partes[3] son los datos "mv|..."
        String data = parts[3];
        System.out.println(">> ¡A MINAR! Dificultad: " + dificulty + " ceros. Rango: " + minGlobal + " a " + maxGlobal);

        stopMining();

        int numThreads = Runtime.getRuntime().availableProcessors();
        int totalItems = maxGlobal - minGlobal;
        int chunkSize = totalItems / numThreads;
        System.out.println(">> Lanzando " + numThreads + " hilos (cada uno procesará ~" + chunkSize + " hashes).");

        for (int i = 0; i < numThreads; i++) {
            int startRange = minGlobal + (i * chunkSize);
            int endRange;
            //Compruebo si este es el ÚLTIMO hilo de la lista
            if (i == numThreads - 1) {
                //Si soy el último, me quedo con hasta el final real (max)
                //Esto recoge los "restos" de la división inexacta
                endRange = maxGlobal;
            } else {
                //Si NO soy el último, cojo solo mi trozo normal
                //(start + tamaño) - 1 porque el rango es inclusivo
                endRange = startRange + chunkSize - 1;
            }

            Thread t = new Thread(() -> {
                try {
                    int solution = HashCalculator.calculateHash(data, startRange, endRange, dificulty);

                    //Si encuentra solución y nadie me ha manadado parar mientras yo estaba calculando  y nadie ha ganado todavía...
                    if (solution != -1 && !Thread.currentThread().isInterrupted() && !iWon) {
                        synchronized (Main.class) {
                            if (!iWon) {
                                iWon = true;
                                String threadName = Thread.currentThread().getName();
                                System.out.println(">> ¡ENCONTRADO por " + threadName + "! Sol: " + solution);
                                out.println("sol " + solution);
                                stopMining();
                            }
                        }
                    }

                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            });
            minerThreads.add(t);
            t.start();
        }
    }

    private static void stopMining() {
        for (Thread t : minerThreads) {
            if (t != null && t.isAlive()) {
                t.interrupt();
            }
        }
        minerThreads.clear();
    }
}