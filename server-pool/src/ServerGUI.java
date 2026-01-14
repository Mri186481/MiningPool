import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.PrintStream;

public class ServerGUI extends Application {

    @Override
    public void start(Stage stage) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("server_layout.fxml"));
            Parent root = loader.load();

            ServerController controller = loader.getController();
            //ACTIVA LA REDIRECCIÓN
            //Se crea el "puente" pasándole el logArea, que nos lo da el controlador
            ConsoleOutput consoleOutput = new ConsoleOutput(controller.getLogArea());
            // Le decimos a Java: "De ahora en adelante, System.out eres tú y tb se redirigen los errores"
            PrintStream ps = new PrintStream(consoleOutput, true);
            System.setOut(ps);
            System.setErr(ps);

            //ARRANCA EL SERVIDOR
            // IMPORTANTE: El servidor tiene un bucle infinito (while true).
            // Si se lanza aquí directamente, la ventana nunca se dibujará porque Java se quedará bloqueado.
            // Por eso, se lanza en un Hilo nuevo.
            Thread serverThread = new Thread(() -> {
                try {
                    MineServer server = new MineServer();
                    server.start(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            // Esto asegura que si cierras la ventana, el hilo muere
            serverThread.setDaemon(true);
            serverThread.start();

            //MUESTRA LA VENTANA
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