import javafx.application.Platform;
import javafx.scene.control.TextArea;
import java.io.IOException;
import java.io.OutputStream;

// CLASE AUXILIAR: Redirige System.out a un TextArea de JavaFX
public class ConsoleOutput extends OutputStream {
    //La clase ConsoleOutput hereda de OutputStream. Un OutputStream es una "tubería" por donde viajan datos (bytes).
    //Java obliga a decir cómo manejar esos datos cuando llegan.
    private final TextArea textArea;
    public ConsoleOutput(TextArea textArea) {
        this.textArea = textArea;
    }

    //NO SE UTILIZA, pero es obligatorio, la clase OutputStream exige que exista, recibe un solo numero entero
    @Override
    public void write(int b) throws IOException {
        // Redirige byte a byte
        append(String.valueOf((char) b));
    }
    //Redirige bloques de texto
    //Recibe un array con todos los datos byte[] b por ejemplo: ['H', 'O', 'L', 'A']
    //off(offset/desplazamiento) donde empieza a leer, por ejemplo 0, desde el principio
    //len(cuantos caracteres lee)
    //Con esta crea un nuevo string cogiendo el trozo de golpe no letra a letra, hace que la interfaz vaya fluida y no se trabe
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        append(new String(b, off, len));
    }

    private void append(String text) {
        //Como los mensajes vienen de hilos del servidor (MineThread),
        //no pueden tocar la UI directamente. Platform.runLater le dice a JavaFX:
        //"Cuando tengas un hueco en el hilo de la interfaz, pinta esto".
        Platform.runLater(() -> textArea.appendText(text));
    }
}
