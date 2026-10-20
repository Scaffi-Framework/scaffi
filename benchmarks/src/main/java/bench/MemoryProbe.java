package bench;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nl.xsyntos.scaffi.framework.commands.CommandContext;
import nl.xsyntos.scaffi.framework.commands.ConverterRegistry;
import nl.xsyntos.scaffi.framework.commands.SubCommandBundle;
import nl.xsyntos.scaffi.framework.commands.annotations.SubCommand;

/** Measures shallow sizes of a bundle and only its directly owned metadata. */
public class MemoryProbe {
    private static Instrumentation instrumentation;

    public static void premain(String args, Instrumentation value) {
        instrumentation = value;
    }

    public static void main(String[] args) throws Exception {
        Method register = ConverterRegistry.class.getDeclaredMethod("registerPredefinedConverters");
        register.setAccessible(true);
        register.invoke(null);
        Method method = FrameworkBenchmark.Commands.class.getMethod("numeric", CommandContext.class, String.class, Integer.class, Boolean.class);
        SubCommandBundle bundle = new SubCommandBundle(method.getAnnotation(SubCommand.class), method);
        long bundleBytes = instrumentation.getObjectSize(bundle);
        long metadataBytes = 0;
        try {
            Field field = SubCommandBundle.class.getDeclaredField("parameters");
            field.setAccessible(true);
            Object[] metadata = (Object[]) field.get(bundle);
            metadataBytes += instrumentation.getObjectSize(metadata);
            for (Object item : metadata) metadataBytes += instrumentation.getObjectSize(item);
        } catch (NoSuchFieldException expectedOnBaseline) {
            // Baseline bundles retain no parameter metadata cache.
        }
        System.out.printf("{\"parameters_including_context\":4,\"bundle_bytes\":%d,\"metadata_bytes\":%d,\"total_owned_bytes\":%d}%n",
            bundleBytes, metadataBytes, bundleBytes + metadataBytes);
    }
}
