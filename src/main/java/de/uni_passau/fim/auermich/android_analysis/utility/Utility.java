package de.uni_passau.fim.auermich.android_analysis.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
