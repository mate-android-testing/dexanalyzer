package de.uni_passau.fim.auermich.android_analysis.utility;

import brut.androlib.ApkDecoder;
import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.directory.DirectoryException;
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
     *
     * @return Returns the path of the decoded APK.
     */
    public static File decodeAPK(final File apkPath) {

        // set 3rd party library (apktool) logging to 'SEVERE'
        java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
        rootLogger.setLevel(Level.SEVERE);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.SEVERE);
        }

        final Config config = Config.getDefaultConfig();
        config.forceDelete = true; // overwrites existing dir: -f

        try {
            // do not decode dex classes to smali: -s
            config.setDecodeSources(Config.DECODE_SOURCES_NONE);

            /*
             * TODO: Right now we need to decode the resources completely although we only need to alter the manifest.
             *  While decoding only the manifest works and even re-packaging succeeds, the APK cannot be properly signed
             *  anymore: https://github.com/iBotPeaches/Apktool/issues/3389
             */

            // do not decode resources: -r
            // config.setDecodeResources(Config.DECODE_RESOURCES_NONE);

            // decode the manifest: --force-manifest
            // config.setForceDecodeManifest(Config.FORCE_DECODE_MANIFEST_FULL);

            // path where we want to decode the APK (the same directory as the APK)
            File parentDir = apkPath.getParentFile();
            File outputDir = new File(parentDir, "decodedAPK");

            LOGGER.debug("Decoding Output Dir: " + outputDir);

            final ApkDecoder decoder = new ApkDecoder(config, apkPath);
            decoder.decode(outputDir);
            return outputDir;
        } catch (AndrolibException | IOException | DirectoryException e) {
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
