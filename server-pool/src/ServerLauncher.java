public class ServerLauncher {
    public static void main(String[] args) {
        // Simplemente llama al main de la interfaz
        ServerGUI.main(args);
    }
    //--Nueva estructura al implementar JavaFX a lo que ya había hecho--
    //He tenido que añadir a mano la libreria de javaFX 21
    //ServerLauncher: Es una lanzadera, es el archivo a ejecutar. He eliminado Main.
    //ServerGUI: Es la ventana y el cerebro visual, arranca MineServer como Main antes.
    //ConsoleOutput: Es el "cable" o clase puente, coge lo que java iba a escribir en
    //consola y lo escribe en un cuadro de texto de JavaFx
    //Miners, MineServer, MineThread: No cambia apenas nada.
    //
    //
    // Proceso de ejecucion
    //Pulso "Play" en ServerLauncher.
    //ServerLauncher llama a ServerGUI.main.
    //ServerGUI.main llama a launch().
    //launch() prepara su motor gráfico y llama automaticamnete a start()
    //start() dibuja la ventana negra y los textos.




}
