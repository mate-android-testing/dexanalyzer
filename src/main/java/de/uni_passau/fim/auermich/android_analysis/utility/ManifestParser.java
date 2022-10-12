package de.uni_passau.fim.auermich.android_analysis.utility;

import de.uni_passau.fim.auermich.android_analysis.component.Activity;
import de.uni_passau.fim.auermich.android_analysis.component.BroadcastReceiver;
import de.uni_passau.fim.auermich.android_analysis.component.Component;
import de.uni_passau.fim.auermich.android_analysis.component.Service;
import de.uni_passau.fim.auermich.android_analysis.component.bundle.ActivityAlias;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ManifestParser {

    private static final Logger LOGGER = LogManager.getLogger(ManifestParser.class);

    private final String MANIFEST;

    private String packageName;
    private String mainActivity;

    // the components declared in the manifest
    private static final String[] COMPONENTS = new String[]{"activity", "activity-alias", "service", "receiver"};

    public ManifestParser(String manifest) {
        MANIFEST = manifest;
    }

    public List<Component> extractComponents() {

        LOGGER.info("Parsing Manifest for components!");

        List<Component> components = new ArrayList<>();

        try {
            File xmlFile = new File(MANIFEST);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);

            for (String componentType : COMPONENTS) {

                NodeList nodeList = doc.getElementsByTagName(componentType);

                for (int i = 0; i < nodeList.getLength(); i++) {

                    Node node = nodeList.item(i);

                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;

                        String componentName = element.getAttribute("android:name");
                        Component component = translateToComponent(componentType, componentName);

                        if (element.hasAttribute("android:enabled")) {
                            component.setEnabled(Boolean.getBoolean(element.getAttribute("android:enabled")));
                        } else {
                            // components are enabled by default
                            component.setEnabled(true);
                        }

                        if (element.hasAttribute("android:exported")) {
                            component.setExported(Boolean.getBoolean(element.getAttribute("android:exported")));
                        } else {
                            // components are not exported by default unless they specify at least one intent filter
                            boolean exported = false;

                            NodeList children = node.getChildNodes();

                            for (int j = 0; j < children.getLength(); j++) {
                                Node child = children.item(j);

                                if (child.getNodeType() == Node.ELEMENT_NODE) {
                                    Element elem = (Element) child;

                                    if (Objects.equals(elem.getTagName(), "intent-filter")) {
                                        exported = true;
                                        break;
                                    }
                                }
                            }
                            component.setExported(exported);
                        }
                        components.add(component);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't parse AndroidManifest.xml!");
            LOGGER.warn(e.getMessage());
        }

        return components;
    }

    private Component translateToComponent(String componentType, String componentName) {

        switch (componentType) {
            case "activity":
                return new Activity(componentName);
            case "activity-alias":
                return new ActivityAlias(componentName);
            case "service":
                return new Service(componentName);
            case "receiver":
                return new BroadcastReceiver(componentName);
            default:
                throw new UnsupportedOperationException("Component type " + componentType + " not yet supported!");
        }
    }

    /**
     * Parses the AndroidManifest.xml for the package name and the name of the main activity.
     *
     * @return Returns {@code true} when we were able to derive both information,
     *         otherwise {@code false}.
     */
    public boolean extractMainActivityAndPackageName() {

        LOGGER.info("Parsing AndroidManifest for MainActivity and PackageName!");

        try {
            File xmlFile = new File(MANIFEST);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);

            NodeList nodeList = doc.getElementsByTagName("manifest");
            // there should be only a single manifest tag
            Node node = nodeList.item(0);

            // get the package name
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                if (!element.hasAttribute("package")) {
                    LOGGER.warn("Couldn't derive package name!");
                    return false;
                } else {
                    packageName = element.getAttribute("package");
                }
            }

            NodeList intentFilters = doc.getElementsByTagName("intent-filter");
            final String NAME_ATTRIBUTE = "android:name";
            final String ALIAS_NAME_ATTRIBUTE = "android:targetActivity";

            // find intent-filter that describes the main activity
            for (int i = 0; i < intentFilters.getLength(); i++) {

                Node intentFilter = intentFilters.item(i);
                NodeList tags = intentFilter.getChildNodes();

                boolean foundMainAction = false;
                boolean foundMainCategory = false;

                for (int j = 0; j < tags.getLength(); j++) {
                    Node tag = tags.item(j);
                    if (tag.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) tag;

                        if (element.getTagName().equals("action")
                                && element.getAttribute(NAME_ATTRIBUTE)
                                .equals("android.intent.action.MAIN")) {
                            foundMainAction = true;
                        } else if (element.getTagName().equals("category")
                                && element.getAttribute(NAME_ATTRIBUTE)
                                .equals("android.intent.category.LAUNCHER")) {
                            foundMainCategory = true;
                        }

                        if (foundMainAction && foundMainCategory) {
                            Node mainActivityNode = intentFilter.getParentNode();
                            if (mainActivityNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element main = (Element) mainActivityNode;
                                if (main.getTagName().equals("activity")) {
                                    mainActivity = main.getAttribute(NAME_ATTRIBUTE);
                                    return true;
                                } else if (main.getTagName().equals("activity-alias")) {
                                    mainActivity = main.getAttribute(ALIAS_NAME_ATTRIBUTE);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            LOGGER.warn("Couldn't derive name of main-activity!");
        } catch (Exception e) {
            LOGGER.warn("Couldn't parse AndroidManifest.xml!");
            LOGGER.warn(e.getMessage());
        }
        return false;
    }
}
