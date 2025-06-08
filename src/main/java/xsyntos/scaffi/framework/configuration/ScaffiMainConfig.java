package xsyntos.scaffi.framework.configuration;

import org.bukkit.plugin.java.JavaPlugin;

import lombok.Data;

@Data
public class ScaffiMainConfig {
    private JavaPlugin plugin;
    private Messages messages = new Messages();
}
