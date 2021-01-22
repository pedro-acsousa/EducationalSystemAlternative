package epic.lms;


import javax.crypto.SecretKeyFactory;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;

//Class for creating a hash for a given string
public class Hash {
    String algorithm = "PBKDF2WithHmacSHA1";

    private byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        //byte[] salt = new byte[16];
        //random.nextBytes(salt);
        byte[] salt = new byte[] {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}; //making the salt a constant in this particular case
        return salt;
    }

    private static String toHex(byte[] array) throws NoSuchAlgorithmException {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if (paddingLength > 0) {
            return String.format("%0" + paddingLength + "d", 0) + hex;
        } else {
            return hex;
        }
    }

    public static byte[] hexStringToByteArray(String hex) { //takes a hex string and converts it to byte array
        byte[] val = new byte[hex.length() / 2];
        for (int i = 0; i < val.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(hex.substring(index, index + 2), 16);
            val[i] = (byte) j;
        }
        return val;
    }

    // Generates the hash and salt aswell as converts them from byte[] into hex before returning both values
    // It is then stored in the database as hex. I chose hex over base64 because base64 includes characters that can
    // be problematic in web development
    public String[] HashString(String input) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = generateSalt();
        System.out.println(Arrays.toString(salt));
        SecretKeyFactory s = SecretKeyFactory.getInstance(algorithm);
        KeySpec spec = new PBEKeySpec(input.toCharArray(), salt, 10000, 128);
        byte[] hash = s.generateSecret(spec).getEncoded();
        String[] result = {toHex(salt), toHex(hash)};
        return result;
    }

    // Same as above hashing function however uses a given salt rather than generating its own
    public String hashWithSalt(String input, String saltString) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = hexStringToByteArray(saltString);
        SecretKeyFactory s = SecretKeyFactory.getInstance(algorithm);
        KeySpec spec = new PBEKeySpec(input.toCharArray(), salt, 10000, 128);
        byte[] hash = s.generateSecret(spec).getEncoded();
        return toHex(hash);
    }

}
