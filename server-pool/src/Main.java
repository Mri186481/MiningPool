import java.io.IOException;
public class Main {
    public static void main(String[] args) throws IOException {
        //Servidor en el puerto 3000
        new MineServer().start(3000);
    }
}