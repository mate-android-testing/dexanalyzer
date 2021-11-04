package de.uni_passau.fim.auermich.android_analysis;

import de.uni_passau.fim.auermich.android_analysis.component.Component;
import de.uni_passau.fim.auermich.android_analysis.scanner.DexScanner;
import lanchon.multidexlib2.BasicDexFileNamer;
import lanchon.multidexlib2.MultiDexIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jf.dexlib2.iface.DexFile;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Parses the classes.dex files of an APK and extracts for each Android application component except
 * ContentProvider string constants, key-value pairs, etc. The collected information is saved in
 * a customized XML document.
 *
 * @author Auer Michael
 * @author Felix Raster
 */
public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    /**
     * Defines the entry point for the static analysis of an APK.
     *
     * @param args The command line arguments. The first argument must refer to the path of the APK.
     * @throws IOException Should never happen.
     */
    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            throw new IllegalStateException("No APK file specified!");
        }

        // we assume that the name of the APK corresponds to the package name of the app
        File apkFile = new File(args[0]);
        String packageName = apkFile.getName().replace(".apk", "");
        LOGGER.debug("Package Name: " + packageName);

        DexFile mergedDex = MultiDexIO.readDexFile(true, apkFile,
                new BasicDexFileNamer(), null, null);

        // scan dex files for components
        DexScanner dexScanner = new DexScanner(List.of(mergedDex), apkFile.getPath());
        List<Component> components = dexScanner.lookUpComponents();

        LOGGER.debug("Components: ");
        for (Component component : components) {
            LOGGER.debug(component);
        }

        // look up for dynamically registered broadcast receivers
        dexScanner.lookUpDynamicBroadcastReceivers(components);

        // write out collected information as xml in same dir as APK
        File outputDir = new File(apkFile.getParentFile(),
                packageName + File.separator + "static_data");
        outputDir.mkdirs();

        File outputFile = new File(outputDir, "staticIntentInfo.xml");
        PrintStream printStream = new PrintStream(outputFile);

        // write xml header
        printStream.print("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>"
                + System.lineSeparator());

        components.forEach(component -> {
            printStream.print(component.toXml());
            LOGGER.debug(component.toXml());
        });

        printStream.close();
    }
}
