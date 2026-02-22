package net.paulem.launchermc.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Errors {
    public static String getStackTrace(String errorName, Throwable cause)
    {
        StringBuilder msg = new StringBuilder(err(errorName + cause.toString()));
        for (StackTraceElement trace : cause.getStackTrace())
        {
            final String toPrint = "\nat " + trace.toString();
            msg.append(toPrint);
        }

        return msg.toString();
    }

    public static String message(boolean err, String toWrite)
    {
        final String date = String.format("[%s] ", new SimpleDateFormat("hh:mm:ss").format(new Date()));
        return date + (err ? "[ERROR]: " : "[INFO]: ") + toWrite;
    }

    public static String err(String message)
    {
        return message(true, message);
    }
}
