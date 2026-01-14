import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class HashCalculator {

    //Se acepta 'dificultad' como parámetro
    public static int calculateHash(String data, int min, int max, int dificulty) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("md5");
        //Se Genera el String objetivo (ej: "00" o "000") dinámicamente
        String target = "0".repeat(dificulty);
        for (int i = min; i <= max; i++) {
            //Si el hilo principal interrumpe (porque otro ganó), se para.
            if (Thread.currentThread().isInterrupted()) {
                return -1;
            }
            digest.reset();
            // Formato: 000datos, 001datos...
            String input = String.format("%03d%s", i, data);
            digest.update(input.getBytes());
            String result = HexFormat.of().formatHex(digest.digest());
            // Hash: se usa 'target' en vez de "00"
            if (result.startsWith(target)) {
                // ¡Encontrado!
                return i;
            }
        }
        // No encontrado en este rango
        return -1;
    }
}
