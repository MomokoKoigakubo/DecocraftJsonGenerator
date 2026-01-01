package com.momo.decogen;

/**
 * Launcher class for the fat JAR.
 * JavaFX requires the main class to NOT extend Application when running from a fat JAR.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
