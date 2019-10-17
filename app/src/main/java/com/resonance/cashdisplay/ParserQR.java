package com.resonance.cashdisplay;

import android.util.Base64;
//import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Святослав on 20.10.2016.
 */
public class ParserQR {
    public static final String TAG = "ParserQR";
    
    private String FiscalNumber ="" ;//фискальный номер
    private String ManufNumber = "";//заводской номер
    private String NumTranzaction = "";//№ транзакции
    private Double SummToPay = 0.00;//сумма

    public String getFiscalNumber(){return  this.FiscalNumber;}
    public String getManufNumber(){return  this.ManufNumber;}
    public String getNumTranzaction(){return  this.NumTranzaction;}
    public Double getSummToPay(){return  this.SummToPay;}

    public ParserQR(){
    }
    public boolean ParseQRData(String StrQr){
        boolean parse = false;
        if (StrQr.trim().length()>=40) {
            try {

                byte[] arrBase64dec = Base64.decode(StrQr.getBytes("UTF-8"), Base64.DEFAULT);
                byte[] arrDecrypt = AES256Cipher.decryption(arrBase64dec);


                String decoded = new String(arrDecrypt);
                FiscalNumber = decoded.substring(0, 10);//фискальный номер
                ManufNumber = decoded.substring(10, 20);
                ;//заводской номер
                NumTranzaction = decoded.substring(20, 30);
                ;//№ транзакции
                String tmp = decoded.substring(30, 40);
                SummToPay = Double.valueOf(tmp) / 100;//сумма*/
                parse = true;
            } catch (Exception e) {
                Log.d(TAG, "ERROR ParseQRData : " +e.getMessage());
                parse = false;
            }
        }
        return parse;
    }


}

class AES256Cipher {

    public static final String TAG = "AES256Cipher";

    /* static key for AES256 crypt */
    private static final byte[] mkey = new byte[]{
            (byte)0xAC, (byte)0x61, (byte)0xD8, (byte)0xBB, (byte)0xFF, (byte)0xAB, (byte)0x9A, (byte)0x84,
            (byte)0xE5, (byte)0xFE, (byte)0x4A, (byte)0xEC, (byte)0xBD, (byte)0xDA, (byte)0x45, (byte)0x7D,
            (byte)0xCF, (byte)0xDE, (byte)0xC1, (byte)0x9C, (byte)0x40, (byte)0xD7, (byte)0xB5, (byte)0x3E,
            (byte)0x3A, (byte)0x57, (byte)0x53, (byte)0x8C, (byte)0xB1, (byte)0x4E, (byte)0x41, (byte)0xBD };

    private static final byte[]  init_vector = new byte[]{
            (byte)0x42, (byte)0x2D, (byte)0xFE, (byte)0x67, (byte)0x71, (byte)0x67, (byte)0x75, (byte)0x2D,
            (byte)0xDA, (byte)0xBC, (byte)0x9C, (byte)0x05, (byte)0x57, (byte)0x73, (byte)0xEE, (byte)0x73 };

    public static  byte[] encryption(byte[] textBytes){

        byte[] buf = null;
        try{
            buf = encrypt(init_vector, mkey, textBytes);
        }catch(Exception e){
            e.printStackTrace();
            Log.e(TAG, "ОШИБКА ПРИ КРИПТОВАНИИ, \n" + e.getMessage());
        };

        return  buf;
    }

    public static byte[] decryption(byte[] textBytes){

        byte[] buf = null;
        try {
            buf = decrypt(init_vector, mkey, textBytes);
        } catch(UnsupportedEncodingException e){
            Log.e(TAG, "ОШИБКА ПРИ ДЕКРИПТОВАНИИ, UnsupportedEncodingException \n" + e.getMessage());
        } catch(NoSuchAlgorithmException e){
            Log.e(TAG, "ОШИБКА ПРИ ДЕКРИПТОВАНИИ, NoSuchAlgorithmException \n" + e.getMessage());
            e.printStackTrace();
        } catch(NoSuchPaddingException e){
            Log.e(TAG, "ОШИБКА ПРИ ДЕКРИПТОВАНИИ,NoSuchPaddingException \n" + e.getMessage());
            e.printStackTrace();
        } catch(InvalidKeyException e){
            Log.e(TAG, "ОШИБКА ПРИ ДЕКРИПТОВАНИИ,InvalidKeyException \n" + e.getMessage());
            e.printStackTrace();
        } catch(InvalidAlgorithmParameterException e){
            Log.e(TAG, "ОШИБКА ПРИ ДЕКРИПТОВАНИИ,InvalidAlgorithmParameterException \n" + e.getMessage());
            e.printStackTrace();
        }catch(IllegalBlockSizeException e){
            Log.e(TAG, "ОШИБКА ПРИ ДЕКРИПТОВАНИИ,IllegalBlockSizeException \n" + e.getMessage());
            e.printStackTrace();
        } catch(BadPaddingException e){
            Log.e(TAG, "ОШИБКА ПРИ ДЕКРИПТОВАНИИ, BadPaddingException  " + e);

        }/* catch(GeneralSecurityException e){
            Log.e(TAG, "ОШИБКА ПРИ ДЕКРИПТОВАНИИ, GeneralSecurityException  " + e.getMessage());
            e.printStackTrace();
        }*/

        ;



        return  buf;
    }

    private static byte[] encrypt(byte[] ivBytes, byte[] keyBytes, byte[] textBytes)
            throws java.io.UnsupportedEncodingException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {

        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = null;
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);
        return cipher.doFinal(textBytes);
    }



    private static byte[] decrypt(byte[] ivBytes, byte[] keyBytes, byte[] textBytes)
            throws java.io.UnsupportedEncodingException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException {

        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
        final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");//"AES/CBC/PKCS5Padding"
        cipher.init(Cipher.DECRYPT_MODE, newKey, ivSpec);
        return cipher.doFinal(textBytes);
    }
}
