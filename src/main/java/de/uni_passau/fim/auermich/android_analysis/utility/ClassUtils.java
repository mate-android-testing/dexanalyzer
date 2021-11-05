package de.uni_passau.fim.auermich.android_analysis.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Provides utility functions for class name related transformations.
 */
public final class ClassUtils {

    private static final Logger LOGGER = LogManager.getLogger(ClassUtils.class);

    private ClassUtils() {
        throw new UnsupportedOperationException("Utility class can't be instantiated!");
    }

    /**
     * Transforms a class name containing '/' into a class name with '.'
     * instead, and removes the leading 'L' as well as the ';' at the end.
     *
     * @param className The class name which should be transformed.
     * @return The transformed class name.
     */
    public static String dottedClassName(String className) {

        if (className.startsWith("[")) {
            // array type
            int index = className.indexOf('L');
            if (index == -1) {
                // primitive array type, e.g [I or [[I
                return className;
            } else {
                // complex array type, e.g. [Ljava/lang/Integer;
                int beginClassName = className.indexOf('L');
                className = className.substring(0, beginClassName)
                        + className.substring(beginClassName + 1, className.indexOf(';'));
                className = className.replace('/', '.');
                return className;
            }
        } else if (!className.startsWith("L")) {
            // primitive type
            return className;
        } else {
            className = className.substring(className.indexOf('L') + 1, className.indexOf(';'));
            className = className.replace('/', '.');
            return className;
        }
    }

    /**
     * Searches for a target class in the given {@code dexFiles}.
     *
     * @param dexFiles  The dexFiles to search in.
     * @param className The name of the target class.
     * @return Returns an optional containing either the target class or not.
     */
    public static Optional<ClassDef> searchForTargetClass(List<DexFile> dexFiles, String className) {

        for (DexFile dexFile : dexFiles) {

            Set<? extends ClassDef> classes = dexFile.getClasses();

            // search for target method
            for (ClassDef classDef : classes) {
                if (classDef.toString().equals(className)) {
                    return Optional.of(classDef);
                }
            }
        }
        return Optional.empty();
    }
}
