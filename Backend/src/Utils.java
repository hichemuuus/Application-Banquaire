package com.example.demo1;  // ← ajouter cette ligne
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    private static final long serialVersionUID = 1L;
    public static String sha256(String mdps)  {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashbytes = md.digest(mdps.getBytes("UTF-8"));

            StringBuilder sb = new StringBuilder();
            for(byte b : hashbytes){
                sb.append(String.format("%02x" ,b));
            }
            return sb.toString();
        }catch(Exception e){
            throw new RuntimeException();
        }

    }
}
