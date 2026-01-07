import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class HashCalculator {

    public static int calculateHash(String data, int min, int max) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("md5");

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

            // Hash que empiece por dos ceros
            if (result.startsWith("00")) {
                // ¡Encontrado!
                return i;
            }
        }
        // No encontrado en este rango
        return -1;
    }
}
