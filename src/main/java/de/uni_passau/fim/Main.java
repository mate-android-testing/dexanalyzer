package de.uni_passau.fim;

import de.uni_passau.fim.component.Component;
import de.uni_passau.fim.scanner.DexScanner;
import de.uni_passau.fim.scanner.ManifestScanner;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.MultiDexContainer;

import java.io.*;
import java.util.ArrayList;
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

    private static final Opcodes API_OPCODE = Opcodes.forApi(28);

    public static void main(String[] args) throws IOException {

        // File apkFile = new File("/home/auermich/smali/ws.xsoh.etar_17.apk");
        // File apkFile = new File("/home/auermich/tools/mate-commander/BMI-debug.apk");
        // File apkFile = new File("C:\\Users\\Michael\\git\\mate-commander\\ws.xsoh.etar_17.apk");

        if (args.length < 1) {
            throw new IllegalStateException("No APK file specified!");
        }

        // we assume that the name of the APK corresponds to the package name of the app
        File apkFile = new File(args[0]);
        String packageName = apkFile.getName().replace(".apk", "");
        System.out.println("Package Name: " + packageName);

        MultiDexContainer<? extends DexBackedDexFile> apk
                = DexFileFactory.loadDexContainer(apkFile, API_OPCODE);

        List<DexFile> dexFiles = new ArrayList<>();

        apk.getDexEntryNames().forEach(dexFile -> {
            try {
                dexFiles.add(apk.getEntry(dexFile).getDexFile());
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalStateException("Couldn't load dex file!");
            }
        });

        // scan dex files for components
        DexScanner dexScanner = new DexScanner(dexFiles, apkFile.getPath());
        List<Component> components = dexScanner.lookUpComponents();

        for (Component component : components) {
            System.out.println(component);
        }

        // look up for dynamically registered broadcast receivers
        dexScanner.lookUpDynamicBroadcastReceivers(components);

        // write out collected information as xml in same dir as APK
        File outputDir = new File(apkFile.getParentFile(),
                packageName + File.separator + "static_data");
        outputDir.mkdirs();

        File outputFile = new File(outputDir, "staticIntentInfo.xml");
        PrintStream printStream = new PrintStream(outputFile);

        /*

        // write xml header
        printStream.print("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>"
                + System.lineSeparator());

        components.forEach(component -> {
            printStream.print(component.toXml());
            System.out.print(component.toXml());
        });

        printStream.close();

        */
    }
}
