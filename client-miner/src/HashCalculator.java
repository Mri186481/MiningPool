import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class HashCalculator {

    public static int calculateHash(String data, int min, int max, int dificulty) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("md5");
        String target = "0".repeat(dificulty);
        for (int i = min; i <= max; i++) {
            //Si el hilo principal interrumpe (porque otro ganó), se para.
            if (Thread.currentThread().isInterrupted()) {
                return -1;
            }
            digest.reset();
            String input = String.format("%03d%s", i, data);
            digest.update(input.getBytes());
            String result = HexFormat.of().formatHex(digest.digest());
            if (result.startsWith(target)) {
                return i;
            }
        }
        return -1;
    }
}
