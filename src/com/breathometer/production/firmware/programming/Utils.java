package com.breathometer.production.firmware.programming;

import jdk.nashorn.api.scripting.URLReader;


import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

/**
 * Created by Joe on 4/20/2016.
 */
public class Utils {
    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static void insert(String filename, long offset, byte[] content) throws Exception {
        RandomAccessFile r = new RandomAccessFile(new File(filename), "rw");
        RandomAccessFile rtemp = new RandomAccessFile(new File(filename + "~"), "rw");
        long fileSize = r.length();
        FileChannel sourceChannel = r.getChannel();
        FileChannel targetChannel = rtemp.getChannel();
        sourceChannel.transferTo(offset, (fileSize - offset), targetChannel);
        sourceChannel.truncate(offset);
        r.seek(offset);
        r.write(content);
        long newOffset = r.getFilePointer();
        targetChannel.position(0L);
        sourceChannel.transferFrom(targetChannel, newOffset, (fileSize - offset));
        sourceChannel.close();
        targetChannel.close();
    }

    public static String toHex(String arg) {
        return String.format("%040x", new BigInteger(1, arg.getBytes(/*YOUR_CHARSET?*/)));
    }

    public static Object computeChecksum(String hex) throws Exception{
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        URL url = Utils.class.getResource("checksum.js");
        engine.eval(new URLReader(url));
        Invocable inv = (Invocable) engine;
        return inv.invokeFunction("calculate_checksum8",hex);
    }

    /**
     * Defines a custom format for the stack trace as String.
     */
    public static String getCustomStackTrace(Throwable aThrowable) {
        //add the class name and any message passed to constructor
        StringBuilder result = new StringBuilder( "Exception: " );
        result.append(aThrowable.toString());
        String NEW_LINE = System.getProperty("line.separator");
        result.append(NEW_LINE);

        //add each element of the stack trace
        for (StackTraceElement element : aThrowable.getStackTrace()){
            result.append(element);
            result.append(NEW_LINE);
        }
        return result.toString();
    }

    public static String readFile(String path, Charset encoding) throws IOException
    {
        Path path2 = Paths.get(path);
        byte[] encoded = Files.readAllBytes(path2);
        return new String(encoded, encoding);
    }

    public static File getJarLocation()
    {
        Properties props = System.getProperties();
        String classpath = props.getProperty("java.class.path");
        String userdir = props.getProperty("user.dir");
        String pathSeparator = props.getProperty("path.separator");

        if (classpath.contains(pathSeparator))
        { // contains multiple jars
            classpath = classpath.split(pathSeparator)[0];
        }

        File fileJar;
        if (classpath.contains(File.separator))
        { // executed within eclipse
            try
            {
                fileJar = new File(classpath).getCanonicalFile();
            }
            catch (IOException e)
            {
                fileJar = null;
            }
        }
        else
        { // executed from executable jar
            try
            {
                fileJar = new File(userdir + File.separator + classpath).getCanonicalFile().getParentFile();
            }
            catch (IOException e)
            {
                fileJar = null;
            }
        }

        if (fileJar.toString().endsWith(".jar"))
            return fileJar.getParentFile();

        return fileJar;
    }
}
