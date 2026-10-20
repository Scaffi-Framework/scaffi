package bench;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.openjdk.jmh.annotations.*;

import nl.xsyntos.scaffi.framework.commands.*;
import nl.xsyntos.scaffi.framework.commands.annotations.Command;
import nl.xsyntos.scaffi.framework.commands.annotations.CommandNullable;
import nl.xsyntos.scaffi.framework.commands.annotations.SubCommand;
import nl.xsyntos.scaffi.framework.commands.processors.CommandProcessor;
import nl.xsyntos.scaffi.framework.commands.processors.TabProcessor;
import nl.xsyntos.scaffi.framework.configuration.Vector3;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms512m", "-Xmx512m", "-XX:+UseG1GC"})
@Threads(1)
public class FrameworkBenchmark {
    @Command(command = "bench", allowExtraArgs = false)
    public static class Commands {
        static final CommandResponse RESPONSE = new CommandResponse(null, null, null);
        int checksum;

        @SubCommand(command = "ping", allowExtraArgs = false)
        public CommandResponse ping(CommandContext context) {
            checksum = context.getArgs().length;
            return RESPONSE;
        }

        @SubCommand(command = "numeric", allowExtraArgs = false)
        public CommandResponse numeric(CommandContext context, String label, Integer count, Boolean enabled) {
            checksum = label.length() + count + (enabled ? 1 : 0);
            return RESPONSE;
        }

        @SubCommand(command = "vector", allowExtraArgs = false)
        public CommandResponse vector(CommandContext context, Vector3 position, String label) {
            checksum = (int) (position.getX() + position.getY() + position.getZ()) + label.length();
            return RESPONSE;
        }

        @SubCommand(command = "optional", allowExtraArgs = false)
        public CommandResponse optional(CommandContext context, @CommandNullable Integer count) {
            checksum = count == null ? 7 : count;
            return RESPONSE;
        }

        @SubCommand(command = "player")
        public CommandResponse player(CommandContext context, Player player) {
            return RESPONSE;
        }
    }

    private Commands instance;
    private CommandSender sender;
    private CommandProcessor processor;
    private TabProcessor commandTabs;
    private TabProcessor playerTabs;
    private Field serverField;
    private Object previousServer;
    private final String[] pingArgs = {"ping"};
    private final String[] numericArgs = {"numeric", "example", "12345", "true"};
    private final String[] vectorArgs = {"vector", "12.5", "64", "-30.25", "home"};
    private final String[] optionalArgs = {"optional"};
    private final String[] commandTabArgs = {"command09"};
    private final String[] playerTabArgs = {"player", "player19"};

    @Setup(Level.Trial)
    public void setup() throws Exception {
        Method register = ConverterRegistry.class.getDeclaredMethod("registerPredefinedConverters");
        register.setAccessible(true);
        register.invoke(null);
        instance = new Commands();
        sender = (CommandSender) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{CommandSender.class},
            (proxy, method, args) -> method.getReturnType() == boolean.class ? true : null);
        HashMap<String, SubCommandBundle> commands = new HashMap<>();
        for (Method method : Commands.class.getMethods()) {
            SubCommand annotation = method.getAnnotation(SubCommand.class);
            if (annotation != null) commands.put(annotation.command(), new SubCommandBundle(annotation, method));
        }
        Command annotation = Commands.class.getAnnotation(Command.class);
        processor = new CommandProcessor(commands, annotation, instance);
        HashMap<String, SubCommandBundle> names = new HashMap<>();
        for (int i = 0; i < 100; i++) names.put(String.format(Locale.ROOT, "Command%03d", i), commands.get("ping"));
        commandTabs = new TabProcessor(names, annotation);
        playerTabs = new TabProcessor(commands, annotation);
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            String name = String.format(Locale.ROOT, "Player%03d", i);
            players.add((Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> method.getName().equals("getName") ? name : null));
        }
        Server server = (Server) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Server.class},
            (proxy, method, args) -> method.getName().equals("getOnlinePlayers") ? players : null);
        serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        previousServer = serverField.get(null);
        serverField.set(null, server);
        verify();
    }

    private void verify() {
        if (commandNoArguments() != 1 || commandNumeric() != 12353 || commandVector() != 50 || commandOptionalMissing() != 7)
            throw new AssertionError("Command semantics differ from expected fixture values");
        if (tabCommands100().size() != 10 || tabPlayers200().size() != 10)
            throw new AssertionError("Completion must return exactly ten matching names");
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        serverField.set(null, previousServer);
    }

    @Benchmark
    public int commandNoArguments() {
        processor.onCommand(sender, null, "bench", pingArgs);
        return instance.checksum;
    }

    @Benchmark
    public int commandNumeric() {
        processor.onCommand(sender, null, "bench", numericArgs);
        return instance.checksum;
    }

    @Benchmark
    public int commandVector() {
        processor.onCommand(sender, null, "bench", vectorArgs);
        return instance.checksum;
    }

    @Benchmark
    public int commandOptionalMissing() {
        processor.onCommand(sender, null, "bench", optionalArgs);
        return instance.checksum;
    }

    @Benchmark
    public List<String> tabCommands100() {
        return commandTabs.onTabComplete(sender, null, "bench", commandTabArgs);
    }

    @Benchmark
    public List<String> tabPlayers200() {
        return playerTabs.onTabComplete(sender, null, "bench", playerTabArgs);
    }

    public static void main(String[] args) throws Exception {
        FrameworkBenchmark bench = new FrameworkBenchmark();
        bench.setup();
        try {
            bench.verify();
            System.out.println("PASS: all six benchmark workloads produce expected outputs");
        } finally {
            bench.tearDown();
        }
    }
}
