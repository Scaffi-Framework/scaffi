package nl.xsyntos.scaffi.framework.commands;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.reflect.ClassPath;

import nl.xsyntos.scaffi.framework.commands.converters.*;
import nl.xsyntos.scaffi.framework.configuration.Vector3;

public class ConverterRegistry {
    private static final Map<Class<?>, IConverter<?>> converters = new HashMap<>();

    public static void registerConverters(JavaPlugin plugin) throws IOException {
        registerPredefinedConverters();
        Class<? extends JavaPlugin> clazz = plugin.getClass();
        ClassPath cp = ClassPath.from(clazz.getClassLoader());

        String packageName = clazz.getPackage().getName();
        for (ClassPath.ClassInfo info : cp.getTopLevelClassesRecursive(packageName)) {
            Class<?> cls = info.load();
            if (IConverter.class.isAssignableFrom(cls)) {
                try {
                    IConverter<?> converter = (IConverter<?>) cls.getDeclaredConstructor().newInstance();
                    //Get the return type of the converter
                    Class<?> type = converter.getClass().getMethod("convert", CommandContext.class, String.class).getReturnType();
                    converters.put(type, converter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Nullable
    public static IConverter<?> getConverter(Class<?> clazz) {
        return converters.get(clazz);
    }

    private static void registerPredefinedConverters() {
        converters.put(Integer.class, new IntConverter());
        converters.put(Boolean.class, new BooleanConverter());
        converters.put(Player.class, new PlayerConverter());
        converters.put(String.class, new StringConverter());
        converters.put(Vector3.class, new Vector3Converter());

    }
}
