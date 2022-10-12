package de.uni_passau.fim.auermich.android_analysis.utility;

import brut.androlib.ApkDecoder;
import brut.common.BrutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.regex.Pattern;

public final class Utility {

    private static final Logger LOGGER = LogManager.getLogger(Utility.class);

    /**
     * The name of the exclusion pattern file.
     */
    private static final String EXCLUSION_PATTERN_FILE = "exclude.txt";

    private Utility() {
        throw new UnsupportedOperationException("Utility class can't be instantiated!");
    }

    /**
     * Decodes a given APK using apktool.
     */
    public static File decodeAPK(File apkPath) {

        // set 3rd party library (apktool) logging to 'SEVERE'
        java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
        rootLogger.setLevel(Level.SEVERE);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.SEVERE);
        }

        ApkDecoder decoder = new ApkDecoder(apkPath);

        // path where we want to decode the APK (the same directory as the APK)
        File parentDir = apkPath.getParentFile();
        File outputDir = new File(parentDir, "decodedAPK");

        LOGGER.info("Decoding Output Dir: " + outputDir);
        decoder.setOutDir(outputDir);

        // overwrites existing dir: -f
        decoder.setForceDelete(true);

        try {

            // whether to decode classes.dex into smali files: -s
            decoder.setDecodeSources(ApkDecoder.DECODE_SOURCES_NONE);

            // whether to decode the AndroidManifest.xml
            // decoder.setForceDecodeManifest(ApkDecoder.FORCE_DECODE_MANIFEST_FULL);

            // whether to decode resources: -r
            // TODO: there seems to be some problem with the AndroidManifest if we don't fully decode resources
            // decoder.setDecodeResources(ApkDecoder.DECODE_RESOURCES_NONE);

            decoder.decode();
            decoder.close();

            // the dir where the decoded content can be found
            return outputDir;
        } catch (BrutException | IOException e) {
            LOGGER.warn("Failed to decode APK file!");
            LOGGER.warn(e.getMessage());
            throw new IllegalStateException(e);
        }
    }


    /**
     * Generates patterns of classes which should be excluded from the instrumentation.
     *
     * @return The pattern representing classes that should not be instrumented.
     */
    public static Pattern readExcludePatterns() {

        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(EXCLUSION_PATTERN_FILE);

        if (inputStream == null) {
            LOGGER.warn("Couldn't find exclusion pattern file!");
            return null;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder builder = new StringBuilder();

        try {
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first)
                    first = false;
                else
                    builder.append("|");
                builder.append(line);
            }
            reader.close();
        } catch (IOException e) {
            LOGGER.warn("Couldn't read from exclusion file!");
            e.printStackTrace();
            return null;
        }
        return Pattern.compile(builder.toString());
    }
}
