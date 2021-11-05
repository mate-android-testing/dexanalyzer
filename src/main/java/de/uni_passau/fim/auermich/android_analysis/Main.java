package de.uni_passau.fim.auermich.android_analysis;

import de.uni_passau.fim.auermich.android_analysis.component.Component;
import de.uni_passau.fim.auermich.android_analysis.scanner.DexScanner;
import lanchon.multidexlib2.BasicDexFileNamer;
import lanchon.multidexlib2.MultiDexIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jf.dexlib2.iface.DexFile;

import java.io.File;
import java.io.FileNotFoundException;
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

    private static String packageName;

    /**
     * Defines the entry point for the static analysis of an APK.
     *
     * @param args The command line arguments. The first argument must refer to the path of the APK.
     * @throws IOException Should never happen.
     */
    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            LOGGER.info("Usage: java -jar dexanalyzer.jar <path-to-apk>. The APK need to" +
                    "be named after the package name of the app!");
        } else {

            // we assume that the name of the APK corresponds to the package name of the app
            File apkFile = new File(args[0]);
            packageName = apkFile.getName().replace(".apk", "");
            LOGGER.debug("Package Name: " + packageName);

            DexFile mergedDex = MultiDexIO.readDexFile(true, apkFile,
                    new BasicDexFileNamer(), null, null);

            // scan dex files for the relevant static data
            DexScanner dexScanner = new DexScanner(List.of(mergedDex), apkFile.getPath());

            // create the output directory for the static data if not present yet in the respective app folder
            File staticDataDir = new File(apkFile.getParentFile(),
                    packageName + File.separator + "static_data");
            staticDataDir.mkdirs();

            generateStaticIntentInfo(dexScanner, staticDataDir);
        }
    }

    /**
     * Generates the staticIntentInfo.xml file necessary for the ExecuteMATERandomExplorationIntent strategy.
     *
     * @param dexScanner Scans the dex files for the static data.
     * @param staticDataDir The directory where the staticIntentInfo.xml file should be stored.
     * @throws FileNotFoundException Should never happen.
     */
    private static void generateStaticIntentInfo(DexScanner dexScanner, File staticDataDir) throws FileNotFoundException {

        // look up components
        List<Component> components = dexScanner.lookUpComponents(packageName);

        LOGGER.debug("Components: ");
        for (Component component : components) {
            LOGGER.debug(component);
        }

        // extract the intent data
        dexScanner.extractIntentInfo(components);

        // look up for dynamically registered broadcast receivers
        dexScanner.lookUpDynamicBroadcastReceivers(components);

        File outputFile = new File(staticDataDir, "staticIntentInfo.xml");
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
