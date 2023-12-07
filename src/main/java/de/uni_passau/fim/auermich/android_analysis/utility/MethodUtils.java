package de.uni_passau.fim.auermich.android_analysis.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.Method;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class MethodUtils {

    private static final Logger LOGGER = LogManager.getLogger(MethodUtils.class);

    private MethodUtils() {
        throw new UnsupportedOperationException("Utility class can't be instantiated!");
    }

    /**
     * Derives a unique method signature for the given method.
     *
     * @param method The method to derive its method signature.
     * @return Returns the method signature of the given {@param method}.
     */
    public static String deriveMethodSignature(Method method) {
        return method.toString();
    }

    /**
     * Searches for a target method in the given {@code dexFile}.
     *
     * @param dexFiles        The dexFiles to search in.
     * @param methodSignature The signature of the target method.
     * @return Returns an optional containing either the target method or not.
     */
    public static Optional<Method> searchForTargetMethod(List<DexFile> dexFiles, String methodSignature) {

        // TODO: search for target method based on className + method signature
        String className = methodSignature.split("->")[0];

        for (DexFile dexFile : dexFiles) {

            Set<? extends ClassDef> classes = dexFile.getClasses();

            // search for target method
            for (ClassDef classDef : classes) {
                if (classDef.toString().equals(className)) {
                    for (Method method : classDef.getMethods()) {
                        if (deriveMethodSignature(method).equals(methodSignature)) {
                            return Optional.of(method);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }
}
