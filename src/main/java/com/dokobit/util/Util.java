package com.dokobit.util;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 */
public class Util {
    public static String byteArrayToHexString(byte[] byteArray) {
        StringBuilder result = new StringBuilder();
        for (int byteIndex = 0; byteIndex < byteArray.length; byteIndex++) {
            result.append(Integer.toString((byteArray[byteIndex] & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    public static String toSHA1(byte[] convertme) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return byteArrayToHexString(md.digest(convertme));
    }

    public static byte[] loadFile(String name) throws IOException {
        InputStream in = new FileInputStream(name);
        return IOUtils.toByteArray(in);
    }
}
