package de.uni_passau.fim.auermich.android_analysis;

import de.uni_passau.fim.auermich.android_analysis.component.Component;
import de.uni_passau.fim.auermich.android_analysis.component.Fragment;
import de.uni_passau.fim.auermich.android_analysis.component.bundle.ActivityAlias;
import de.uni_passau.fim.auermich.android_analysis.scanner.DexScanner;
import de.uni_passau.fim.auermich.android_analysis.utility.ComponentUtils;
import de.uni_passau.fim.auermich.android_analysis.utility.ManifestParser;
import de.uni_passau.fim.auermich.android_analysis.utility.Utility;
import lanchon.multidexlib2.BasicDexFileNamer;
import lanchon.multidexlib2.MultiDexIO;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jf.dexlib2.iface.DexFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
     * The package name of the application.
     */
    private static String packageName;

    /**
     * Whether all classes should be resolved or only those being part of the application package.
     * By default, only classes belonging to the application package are resolved.
     */
    private static boolean resolveAllClasses = false;

    /**
     * Defines the entry point for the static analysis of an APK.
     *
     * @param args The command line arguments. The first argument must refer to the path of the APK.
     *         The second argument (optional) --rac || --resolve-all-classes denotes whether all classes should
     *         be resolved or not.
     * @throws IOException Should never happen.
     */
    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            LOGGER.info("Usage: java -jar dexanalyzer.jar <path-to-apk> --resolve-all-classes (OPTIONAL). " +
                    "The APK need to be named after the package name of the app!");
        } else {

            // we assume that the name of the APK corresponds to the package name of the app
            File apkFile = new File(args[0]);
            packageName = apkFile.getName().replace(".apk", "");
            LOGGER.debug("Package Name: " + packageName);

            if (args.length == 2) {
                String argument = args[1];
                if (argument.equals("--rac") || argument.equals("--resolve-all-classes")) {
                    LOGGER.debug("Resolving all classes!");
                    resolveAllClasses = true;
                }
            }

            // decode the APK file
            File decodedAPKPath = Utility.decodeAPK(apkFile);

            DexFile mergedDex = MultiDexIO.readDexFile(true, apkFile,
                    new BasicDexFileNamer(), null, null);

            // scan dex files for the relevant static data
            DexScanner dexScanner = new DexScanner(List.of(mergedDex), packageName, resolveAllClasses);

            // create the output directory for the static data if not present yet in the respective app folder
            File staticDataDir = new File(apkFile.getParentFile(), packageName + File.separator + "static_data");
            staticDataDir.mkdirs();

            generateComponentInfo(dexScanner, staticDataDir, decodedAPKPath);
            generateStaticStrings(dexScanner, staticDataDir);
            generateStaticIntentInfo(dexScanner, staticDataDir);

            // remove the decoded APK files
            try {
                FileUtils.deleteDirectory(decodedAPKPath);
            } catch (IOException e) {
                LOGGER.warn("Couldn't delete directory " + decodedAPKPath + " properly!");
            }
        }
    }

    /**
     * Generates the components.xml file listening detailed information about the individual components.
     *
     * @param dexScanner Scans the dex files for the static data.
     * @param staticDataDir The directory where the components.xml file should be stored.
     * @throws FileNotFoundException Should never happen.
     */
    private static void generateComponentInfo(DexScanner dexScanner, File staticDataDir,
                                              File decodedAPKPath) throws FileNotFoundException {

        // we are only interested in activities, services and broadcast receivers
        List<Component> allComponents = dexScanner.lookUpComponents().stream()
                .filter(component -> !(component instanceof Fragment))
                .collect(Collectors.toList());

        ManifestParser manifest = new ManifestParser(decodedAPKPath + File.separator + "AndroidManifest.xml");
        List<Component> manifestComponents = manifest.extractComponents();

        List<Component> components = mergeComponents(allComponents, manifestComponents);
        dexScanner.lookUpDynamicBroadcastReceivers(components);

        File outputFile = new File(staticDataDir, "components.xml");
        PrintStream printStream = new PrintStream(outputFile);

        // write xml header
        printStream.print("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>"
                + System.lineSeparator());

        components.forEach(component -> {
            printStream.print(component.toXmlSimple() + System.lineSeparator());
            LOGGER.debug(component.toXmlSimple());
        });

        printStream.close();
    }

    private static List<Component> mergeComponents(List<Component> allComponents, List<Component> manifestComponents) {

        final Set<Component> components = new HashSet<>();

        for (Component component : manifestComponents) {

            if (component instanceof ActivityAlias) {
                // activity-aliases are only pseudo-components
                components.add(component);
                continue;
            }

            // the component should be discoverable in the code
            Component codeComponent = allComponents.get(allComponents.indexOf(component));

            // copy over attributes
            codeComponent.setEnabled(component.isEnabled());
            codeComponent.setExported(component.isExported());

            components.add(codeComponent);
        }

        // the remaining components should be primarily dynamic broadcast receivers and abstract component classes
        for (Component component : allComponents) {
            if (!components.contains(component)) {
                components.add(component);
            }
        }

        return components.stream()
                // we don't count abstract classes as real components
                .filter(component -> (component instanceof ActivityAlias) || !ComponentUtils.isAbstractClass(component.getClazz()))
                .collect(Collectors.toList());
    }

    /**
     * Generates the staticIntentInfo.xml file necessary for the ExecuteMATERandomExplorationIntent strategy.
     *
     * @param dexScanner Scans the dex files for the static data.
     * @param staticDataDir The directory where the staticIntentInfo.xml file should be stored.
     * @throws FileNotFoundException Should never happen.
     */
    private static void generateStaticIntentInfo(DexScanner dexScanner, File staticDataDir) throws FileNotFoundException {

        List<Component> components = dexScanner.lookUpComponents();

        LOGGER.debug("Components: ");
        for (Component component : components) {
            LOGGER.debug(component);
        }

        // extract the static intent data
        dexScanner.extractIntentInfo(components);

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

    /**
     * Generates the staticStrings.xml file which contains all static strings of an APK.
     *
     * @param dexScanner Scans the dex files for the static string data.
     * @param staticDataDir The directory where the staticStrings.xml file should be stored.
     * @throws FileNotFoundException Should never happen.
     */
    private static void generateStaticStrings(DexScanner dexScanner, File staticDataDir) throws FileNotFoundException {

        List<Component> components = dexScanner.lookUpComponents();

        dexScanner.extractStringConstants(components);
        File outputFile = new File(staticDataDir, "staticStrings.xml");
        PrintStream printStream = new PrintStream(outputFile);

        // write xml header
        printStream.print("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>"
                + System.lineSeparator());

        components.forEach(component -> {
            printStream.print(component.staticStringsToXml());
            LOGGER.debug(component.staticStringsToXml());
        });
        printStream.close();
    }

}
