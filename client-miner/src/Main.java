import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class Main {

    //Estas variables sean accesibles desde los métodos auxiliares
    private static PrintWriter out;
    private static Thread minerThread;
    private static boolean iWon = false;

    public static void main(String[] args) {
        System.out.println("--- CLIENTE MINERO INICIADO ---");

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
        // msg viene así: "new_request 0-100 mv|datos..."
        // Partimos el mensaje por espacios
        String[] parts = msg.split(" ");

        // partes[1] es el rango "0-100"
        String[] ranges = parts[1].split("-");
        int min = Integer.parseInt(ranges[0]);
        int max = Integer.parseInt(ranges[1]);

        // partes[2] son los datos "mv|..."
        String data = parts[2];

        System.out.println(">> ¡A MINAR! Rango asignado: " + min + " a " + max);

        // Por si acaso había uno viejo corriendo
        stopMining();
        // Lanzo un hilo aparte (Para no bloquear la escucha del servidor)
        minerThread = new Thread(() -> {
            try {
                // Llamada a la clase HashCalculator
                int solution = HashCalculator.calculateHash(data, min, max);
                boolean hasSolution = (solution != -1);
                boolean isInterrupted = Thread.currentThread().isInterrupted();
                //Si tengo la solucion y no me han parado...
                if (hasSolution && !isInterrupted) {
                    iWon = true;
                    System.out.println(">> ¡ENCONTRADO! Enviando solución: " + solution);
                    out.println("sol " + solution);
                } else {
                    System.out.println(">> Rango terminado sin éxito (o interrumpido).");
                }

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        });

        minerThread.start();
    }

    private static void stopMining() {
        if (minerThread != null && minerThread.isAlive()) {
            minerThread.interrupt(); // Esto hace saltar el 'if' dentro de HashCalculator
        }
    }
}