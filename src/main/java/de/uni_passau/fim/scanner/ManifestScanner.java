package de.uni_passau.fim.scanner;

import de.uni_passau.fim.Main;
import de.uni_passau.fim.component.Activity;
import de.uni_passau.fim.component.BroadcastReceiver;
import de.uni_passau.fim.component.Component;
import de.uni_passau.fim.component.Service;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public class ManifestScanner {

    // TODO: refactor + currently unused

    private static ArrayList<Component> components;
    private static Component currentComponent;
    private static String packageName;
    private ManifestScanner() {

    }

    public static ArrayList<Component> scan(File file) throws IOException {
        components = new ArrayList<>();
        // Run a java app in a separate system process
        // TODO: make pc independent
        String jarPath = null;
        try {
            jarPath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).toPath().toAbsolutePath().getParent().toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        Process proc = Runtime.getRuntime().exec("java -jar " + jarPath + "/apktool_2.4.0.jar d " + file.getAbsolutePath() + " -o " + jarPath + "/tmpApkFiles -f");

        while (proc.isAlive()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        File manifest = new File(jarPath + "/tmpApkFiles/AndroidManifest.xml");
        if(!manifest.exists())
            throw new UnsupportedOperationException(jarPath + "/apktool_2.4.0.jar didn't create the manifest file for " + file.getAbsolutePath());

        try {
            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            xmlReader.setContentHandler(new ContentHandler() {
                @Override
                public void setDocumentLocator(Locator locator) {

                }

                @Override
                public void startDocument() throws SAXException {

                }

                @Override
                public void endDocument() throws SAXException {

                }

                @Override
                public void startPrefixMapping(String prefix, String uri) throws SAXException {

                }

                @Override
                public void endPrefixMapping(String prefix) throws SAXException {

                }

                @Override
                public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
                    if(localName.equals("activity")) {
                        currentComponent = new Activity(assembleFullName(atts.getValue("http://schemas.android.com/apk/res/android", "name")));
                        if(componentAvailable(atts)) {
                            components.add(currentComponent);
                            currentComponent = null;
                        }
                    } else if(localName.equals("service")) {
                        currentComponent = new Service(assembleFullName(atts.getValue("http://schemas.android.com/apk/res/android","name")));
                        if(componentAvailable(atts)) {
                            components.add(currentComponent);
                            currentComponent = null;
                        }
                    } else if(localName.equals("receiver")) {
                        currentComponent = new BroadcastReceiver(assembleFullName(atts.getValue("http://schemas.android.com/apk/res/android","name")));
                        if(componentAvailable(atts)) {
                            components.add(currentComponent);
                            currentComponent = null;
                        }
                    } else if(localName.equals("activity-alias")) {
                        for(Component component : components) {
                            if(component.getName().equals(assembleFullName(atts.getValue("http://schemas.android.com/apk/res/android","targetActivity")))) {
                                currentComponent = null;
                                return;
                            }
                        }
                        currentComponent = new Activity(assembleFullName(atts.getValue("http://schemas.android.com/apk/res/android","targetActivity")));
                        if(componentAvailable(atts)) {
                            components.add(currentComponent);
                            currentComponent = null;
                        }

                    } else if(localName.equals("intent-filter")) {
                        if(currentComponent != null) {
                            components.add(currentComponent);
                            currentComponent = null;
                        }
                    } else if(localName.equals("manifest")) {
                        packageName = atts.getValue("package");
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {

                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {

                }

                @Override
                public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {

                }

                @Override
                public void processingInstruction(String target, String data) throws SAXException {

                }

                @Override
                public void skippedEntity(String name) throws SAXException {

                }
            });
            xmlReader.parse(new InputSource(new FileInputStream(manifest)));
        } catch (SAXException e) {
            e.printStackTrace();
        }

        // TODO: refactor
        Files.move(manifest.toPath(), Paths.get(Paths.get("").toAbsolutePath().toString() + "/"
                + file.getName().substring(0, file.getName().length() - 4) + ".static_data/manifest.xml"), StandardCopyOption.REPLACE_EXISTING);


        return components;
    }

    private static boolean componentAvailable(Attributes atts) {
        return atts.getValue("http://schemas.android.com/apk/res/android","exported") != null && atts.getValue("http://schemas.android.com/apk/res/android","exported").equals("true") && (atts.getValue("http://schemas.android.com/apk/res/android","enabled") == null || atts.getValue("http://schemas.android.com/apk/res/android","enabled").equals("true"));
    }

    private static String assembleFullName(String componentName) {
        if(packageName == null)
            throw new IllegalStateException("Seems the jar tried to assemble a component-name without having found the manifest-tag before.");
        if(componentName.charAt(0) == '.')
            return packageName + componentName;
        else
            return componentName;
    }
}
