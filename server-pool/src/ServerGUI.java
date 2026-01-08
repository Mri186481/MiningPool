import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.PrintStream;

//Proceso de ejecucion
//Pulso "Play" en ServerLauncher.
//ServerLauncher llama a ServerGUI.main.
//ServerGUI.main llama a launch().
//launch() prepara su motor grafico y llama automaticamnete a start()
//start() dibuja la ventana negra y los textos.

public class ServerGUI extends Application {

    @Override
    public void start(Stage stage) {
        try {
            //CARGAR EL DISEÑO
            // Le decimos que busque el archivo fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("server_layout.fxml"));
            // load() lee el XML y crea los objetos (botones, textos...) en memoria
            Parent root = loader.load();

            //OBTENER EL CONTROLADOR
            // Al cargar el FXML, Java crea automáticamente una instancia de ServerController.
            // La recuperamos aquí para pedirle el TextArea.
            ServerController controller = loader.getController();

            //ACTIVAR LA REDIRECCIÓN
            // Creamos el "puente" pasándole el logArea, que nos lo da el controlador
            ConsoleOutput consoleOutput = new ConsoleOutput(controller.getLogArea());
            // Le decimos a Java: "De ahora en adelante, System.out eres tú"
            PrintStream ps = new PrintStream(consoleOutput, true);
            System.setOut(ps);
            // También redirigimos los errores (System.err)
            System.setErr(ps);

            //ARRANCAR EL SERVIDOR
            // IMPORTANTE: El servidor tiene un bucle infinito (while true).
            // Si lo lanzamos aquí directamente, la ventana nunca se dibujará porque Java se quedará bloqueado.
            // Por eso, lo lanzamos en un Hilo nuevo.
            Thread serverThread = new Thread(() -> {
                try {
                    MineServer server = new MineServer();
                    server.start(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            // Esto asegura que si cierras la ventana, el hilo muera
            serverThread.setDaemon(true);
            serverThread.start();

            //MOSTRAR LA VENTANA
            Scene scene = new Scene(root);
            stage.setTitle("Mining Pool Server GUI (FXML)");
            stage.setScene(scene);

            stage.setOnCloseRequest(e -> {
                Platform.exit();
                System.exit(0);
            });

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("¡Error fatal cargando el FXML!: " + e.getMessage());
        }
    }
    //Metodo main que se llama desde Serverlauncher
    public static void main(String[] args) {
        launch(args);
    }
}