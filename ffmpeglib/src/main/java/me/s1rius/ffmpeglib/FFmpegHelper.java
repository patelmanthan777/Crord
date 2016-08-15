package me.s1rius.ffmpeglib;

public class FFmpegHelper {

    static {
        System.loadLibrary("ffmpeghelper");
    }

    public static void runCommand(String[] comands) {
        run(comands);
    }


    private static native int run(String[] commands);
}
