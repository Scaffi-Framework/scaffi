package xsyntos.scaffi.framework;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.bukkit.plugin.java.JavaPlugin;

import xsyntos.scaffi.framework.commands.CommandRegistry;
import xsyntos.scaffi.framework.commands.ConverterRegistry;
import xsyntos.scaffi.framework.configuration.ScaffiMainConfig;

/**
 * The Main class to initialize a Scaffi Plugin
 */
public class ScaffiPlugin {

    public static ScaffiMainConfig config = new ScaffiMainConfig();

    /**
     * Initialize the Scaffi framework for the plugin.
     * @param basePlugin the Minecraft plugin
     */
    public static void enable(JavaPlugin basePlugin) {
            try {
                ScaffiPlugin.config.setPlugin(basePlugin);
                ConverterRegistry.registerConverters(basePlugin);
                CommandRegistry.registerCommands(basePlugin);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
    
}
