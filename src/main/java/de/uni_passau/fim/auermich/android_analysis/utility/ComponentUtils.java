package de.uni_passau.fim.auermich.android_analysis.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.iface.ClassDef;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides utility functions to check whether a class represents an android component, e.g. an activity class.
 */
public final class ComponentUtils {

    private static final Logger LOGGER = LogManager.getLogger(ComponentUtils.class);

    /**
     * The recognized activity classes.
     */
    private static final Set<String> ACTIVITY_CLASSES = new HashSet<>() {{
        add("Landroid/app/Activity;");
        add("Landroidx/appcompat/app/AppCompatActivity;");
        add("Landroid/support/v7/app/AppCompatActivity;");
        add("Landroid/support/v7/app/ActionBarActivity;");
        add("Landroid/support/v4/app/FragmentActivity;");
        add("Landroid/preference/PreferenceActivity;");
    }};

    /**
     * The recognized fragment classes, see https://developer.android.com/reference/android/app/Fragment.
     */
    private static final Set<String> FRAGMENT_CLASSES = new HashSet<>() {{
        add("Landroid/app/Fragment;");
        add("Landroidx/fragment/app/Fragment;");
        add("Landroid/support/v4/app/Fragment;");
        add("Landroid/app/DialogFragment;");
        add("Landroid/app/ListFragment;");
        add("Landroid/preference/PreferenceFragment;");
        add("Landroid/webkit/WebViewFragment;");
    }};

    /**
     * The recognized service classes, see https://developer.android.com/reference/android/app/Service.
     */
    private static final Set<String> SERVICE_CLASSES = new HashSet<>() {{
        add("Landroid/app/Service;");
        add("Landroid/app/IntentService;");
        add("Landroid/widget/RemoteViewsService;");
        add("Landroid/app/job/JobService;");
    }};

    /**
     * The recognized broadcast receiver classes, see https://developer.android.com/reference/android/content/BroadcastReceiver.
     */
    private static final Set<String> BROADCAST_RECEIVER_CLASSES = new HashSet<>() {{
        add("Landroid/content/BroadcastReceiver;");
        add("Landroid/appwidget/AppWidgetProvider;");
    }};

    private ComponentUtils() {
        throw new UnsupportedOperationException("Utility class can't be instantiated!");
    }

    /**
     * Checks whether the given class represents an activity by checking against the super class.
     *
     * @param classes      The set of classes.
     * @param currentClass The class to be inspected.
     * @return Returns {@code true} if the current class is an activity,
     * otherwise {@code false}.
     */
    public static boolean isActivity(final List<ClassDef> classes, final ClassDef currentClass) {

        // TODO: this approach might be quite time-consuming, may find a better solution

        String superClass = currentClass.getSuperclass();
        boolean abort = false;

        while (!abort && superClass != null && !superClass.equals("Ljava/lang/Object;")) {

            abort = true;

            if (ACTIVITY_CLASSES.contains(superClass)) {
                return true;
            } else {
                // step up in the class hierarchy
                for (ClassDef classDef : classes) {
                    if (classDef.toString().equals(superClass)) {
                        superClass = classDef.getSuperclass();
                        abort = false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the given class represents a fragment by checking against the super class.
     *
     * @param classes      The set of classes.
     * @param currentClass The class to be inspected.
     * @return Returns {@code true} if the current class is a fragment,
     * otherwise {@code false}.
     */
    public static boolean isFragment(final List<ClassDef> classes, final ClassDef currentClass) {

        // TODO: this approach might be quite time-consuming, may find a better solution

        String superClass = currentClass.getSuperclass();
        boolean abort = false;

        while (!abort && superClass != null && !superClass.equals("Ljava/lang/Object;")) {

            abort = true;

            if (FRAGMENT_CLASSES.contains(superClass)) {
                return true;
            } else {
                // step up in the class hierarchy
                for (ClassDef classDef : classes) {
                    if (classDef.toString().equals(superClass)) {
                        superClass = classDef.getSuperclass();
                        abort = false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the given class represents a service by checking against the super class.
     *
     * @param classes      The set of classes.
     * @param currentClass The class to be inspected.
     * @return Returns {@code true} if the current class is a service,
     * otherwise {@code false} is returned.
     */
    public static boolean isService(final List<ClassDef> classes, final ClassDef currentClass) {

        // TODO: this approach might be quite time-consuming, may find a better solution

        String superClass = currentClass.getSuperclass();
        boolean abort = false;

        while (!abort && superClass != null && !superClass.equals("Ljava/lang/Object;")) {

            abort = true;

            if (SERVICE_CLASSES.contains(superClass)) {
                return true;
            } else {
                // step up in the class hierarchy
                for (ClassDef classDef : classes) {
                    if (classDef.toString().equals(superClass)) {
                        superClass = classDef.getSuperclass();
                        abort = false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the given class represents a broadcast receiver by checking against the super class.
     *
     * @param classes      The set of classes.
     * @param currentClass The class to be inspected.
     * @return Returns {@code true} if the current class is a broadcast receiver,
     * otherwise {@code false}.
     */
    public static boolean isBroadcastReceiver(List<ClassDef> classes, ClassDef currentClass) {

        // TODO: this approach might be quite time-consuming, may find a better solution

        String superClass = currentClass.getSuperclass();
        boolean abort = false;

        while (!abort && superClass != null && !superClass.equals("Ljava/lang/Object;")) {

            abort = true;

            if (BROADCAST_RECEIVER_CLASSES.contains(superClass)) {
                return true;
            } else {
                // step up in the class hierarchy
                for (ClassDef classDef : classes) {
                    if (classDef.toString().equals(superClass)) {
                        superClass = classDef.getSuperclass();
                        abort = false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the given class in an abstract class.
     *
     * @param classDef The class to be checked.
     * @return Returns {@code true} if the given class is declared abstract, otherwise {@code false} is returned.
     */
    public static boolean isAbstractClass(ClassDef classDef) {
        return Arrays.stream(AccessFlags.getAccessFlagsForClass(classDef.getAccessFlags()))
                .anyMatch(flag -> flag == AccessFlags.ABSTRACT);
    }
}
