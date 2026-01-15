import javafx.application.Platform;
import javafx.scene.control.TextArea;
import java.io.IOException;
import java.io.OutputStream;

// CLASE AUXILIAR: Redirige System.out a un TextArea de JavaFX
public class ConsoleOutput extends OutputStream {

    private final TextArea textArea;
    public ConsoleOutput(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(int b) throws IOException {
        // Redirige byte a byte, no se utiliza, es obligatorio ponerla
        append(String.valueOf((char) b));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        append(new String(b, off, len));
    }

    private void append(String text) {
        Platform.runLater(() -> textArea.appendText(text));
    }
}
