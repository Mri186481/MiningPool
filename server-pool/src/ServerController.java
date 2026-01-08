import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class ServerController {


    @FXML
    private TextArea logArea;

    // Método getter para poder pasarle este TextArea a la clase ConsoleOutput
    public TextArea getLogArea() {
        return logArea;
    }
}
