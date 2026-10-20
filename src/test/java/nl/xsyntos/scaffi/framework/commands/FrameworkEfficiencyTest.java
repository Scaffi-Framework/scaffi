package nl.xsyntos.scaffi.framework.commands;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import nl.xsyntos.scaffi.framework.commands.annotations.Command;
import nl.xsyntos.scaffi.framework.commands.annotations.CommandNullable;
import nl.xsyntos.scaffi.framework.commands.annotations.HelpCommand;
import nl.xsyntos.scaffi.framework.commands.annotations.ITabOverwriter;
import nl.xsyntos.scaffi.framework.commands.annotations.SubCommand;
import nl.xsyntos.scaffi.framework.commands.annotations.TabOverwrite;
import nl.xsyntos.scaffi.framework.commands.converters.PlayerConverter;
import nl.xsyntos.scaffi.framework.commands.processors.CommandProcessor;
import nl.xsyntos.scaffi.framework.commands.processors.TabProcessor;
import nl.xsyntos.scaffi.framework.configuration.Vector3;

public class FrameworkEfficiencyTest {
    @Command(command = "test")
    @HelpCommand(entriesPerPage = 1)
    public static class Commands {
        @SubCommand(command = "echo", aliases = {"say", "repeat"})
        public CommandResponse echo(CommandContext context, String value, @CommandNullable Integer count) {
            return CommandResponse.builder().message(value + ":" + count + ":" + context.getAlias()).build();
        }

        @SubCommand(command = "move")
        public CommandResponse move(CommandContext context, Vector3 position, String label) {
            return CommandResponse.builder().message(position.getX() + ":" + position.getY() + ":" + position.getZ() + ":" + label).build();
        }

        @SubCommand
        public CommandResponse root(CommandContext context, @TabOverwrite(Suggestions.class) String value) {
            return CommandResponse.builder().message(value).build();
        }
    }

    public static class Suggestions implements ITabOverwriter {
        public List<String> tabComplete(CommandContext context, String value, int currentParam) {
            return List.of("Istanbul", "Berlin");
        }
    }

    private final Commands instance = new Commands();
    private final List<String> messages = new ArrayList<>();
    private final CommandSender sender = (CommandSender) Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[]{CommandSender.class}, (proxy, method, args) -> {
            if (method.getName().equals("sendMessage")) {
                messages.add((String) args[0]);
                return null;
            }
            if (method.getReturnType() == boolean.class) return true;
            return null;
        });

    @Before
    public void registerConverters() {
        ConverterRegistry.registerConverters(List.of());
    }

    private SubCommandBundle bundle(String name, Class<?>... types) throws Exception {
        Method method = Commands.class.getMethod(name, types);
        return new SubCommandBundle(method.getAnnotation(SubCommand.class), method);
    }

    @Test
    public void commandsPreserveContextOptionalArgumentsAndAliases() throws Exception {
        SubCommandBundle echo = bundle("echo", CommandContext.class, String.class, Integer.class);
        HashMap<String, SubCommandBundle> commands = new HashMap<>();
        commands.put("echo", echo);
        commands.put("say", echo);
        CommandProcessor processor = new CommandProcessor(commands, Commands.class.getAnnotation(Command.class), instance);
        assertTrue(processor.onCommand(sender, null, "test", new String[]{"echo", "hello", "3"}));
        assertTrue(processor.onCommand(sender, null, "alias", new String[]{"say", "hello"}));
        assertEquals(List.of("hello:3:test", "hello:null:alias"), messages);
    }

    @Test
    public void multiTokenConversionPreservesSubsequentArgumentPosition() throws Exception {
        HashMap<String, SubCommandBundle> commands = new HashMap<>();
        commands.put("move", bundle("move", CommandContext.class, Vector3.class, String.class));
        new CommandProcessor(commands, Commands.class.getAnnotation(Command.class), instance)
            .onCommand(sender, null, "test", new String[]{"move", "1", "2", "3", "home"});
        assertEquals(List.of("1.0:2.0:3.0:home"), messages);
    }

    @Test
    public void rootExecutionAndContextOnlyCommandsPreserveArgumentLayout() throws Exception {
        HashMap<String, SubCommandBundle> commands = new HashMap<>();
        commands.put("", bundle("root", CommandContext.class, String.class));
        Method ping = Ping.class.getMethod("ping", CommandContext.class);
        commands.put("ping", new SubCommandBundle(ping.getAnnotation(SubCommand.class), ping));
        CommandProcessor processor = new CommandProcessor(commands, Commands.class.getAnnotation(Command.class), new Ping());
        processor.onCommand(sender, null, "test", new String[]{"ping"});
        new CommandProcessor(commands, Commands.class.getAnnotation(Command.class), instance)
            .onCommand(sender, null, "test", new String[]{"hello"});
        assertEquals(List.of("pong:test", "hello"), messages);
    }

    public static class Ping {
        @SubCommand(command = "ping", allowExtraArgs = false)
        public CommandResponse ping(CommandContext context) {
            return CommandResponse.builder().message("pong:" + context.getAlias()).build();
        }
    }

    @Test
    public void preDiscoveredClassesRegisterCustomConverters() throws Exception {
        var classes = com.google.common.reflect.ClassPath.from(getClass().getClassLoader())
            .getTopLevelClassesRecursive("nl.xsyntos.scaffi.framework.commands.fixtures");
        ConverterRegistry.registerConverters(classes);
        var converter = ConverterRegistry.getConverter(
            nl.xsyntos.scaffi.framework.commands.fixtures.TokenConverter.Token.class);
        assertNotNull(converter);
        assertEquals(new nl.xsyntos.scaffi.framework.commands.fixtures.TokenConverter.Token("value"),
            converter.convert(null, "value"));
    }

    public static class RestrictedCommands {
        @SubCommand(command = "restricted", executorWhitelist = {Player.class})
        public CommandResponse restricted(CommandContext context) {
            return CommandResponse.builder().build();
        }

        @SubCommand(command = "fail", errorResponse = CustomError.class)
        public CommandResponse fail(CommandContext context) {
            throw new IllegalStateException("Expected test exception");
        }
    }

    public static class CustomError implements nl.xsyntos.scaffi.framework.commands.annotations.ICommandResponder {
        private int calls;

        @Override
        public CommandResponse response(CommandContext context) {
            return CommandResponse.builder().message("custom:" + context.getAlias() + ":" + ++calls).build();
        }
    }

    @Test
    public void executorRestrictionsArePreservedOnCachedBundles() throws Exception {
        Method method = RestrictedCommands.class.getMethod("restricted", CommandContext.class);
        SubCommandBundle command = new SubCommandBundle(method.getAnnotation(SubCommand.class), method);
        assertFalse(command.executorAllowed(sender));
        assertTrue(command.executorAllowed(player("Alice")));
        command.setMethod(method);
        assertFalse(command.executorAllowed(sender));
    }

    @Test
    public void invocationFailuresUseFreshCustomErrorResponders() throws Exception {
        Method method = RestrictedCommands.class.getMethod("fail", CommandContext.class);
        HashMap<String, SubCommandBundle> commands = new HashMap<>();
        commands.put("fail", new SubCommandBundle(method.getAnnotation(SubCommand.class), method));
        CommandProcessor processor = new CommandProcessor(commands, Commands.class.getAnnotation(Command.class), new RestrictedCommands());
        processor.onCommand(sender, null, "alias", new String[]{"fail"});
        processor.onCommand(sender, null, "alias", new String[]{"fail"});
        assertEquals(List.of("custom:alias:1", "custom:alias:1"), messages);
    }

    @Test
    public void parameterCacheRefreshesWhenMethodOrConvertersChange() throws Exception {
        SubCommandBundle command = bundle("echo", CommandContext.class, String.class, Integer.class);
        var original = command.getConverter(1);
        ConverterRegistry.registerConverters(List.of());
        assertNotSame(original, command.getConverter(1));
        assertSame(ConverterRegistry.getConverter(String.class), command.getConverter(1));
        command.setMethod(Commands.class.getMethod("move", CommandContext.class, Vector3.class, String.class));
        assertEquals(Vector3.class, command.getParameterInfo(1).parameter().getType());
        assertSame(ConverterRegistry.getConverter(Vector3.class), command.getConverter(1));
        var copy = command.getParameters();
        copy[1] = null;
        assertNotNull(command.getParameters()[1]);
    }

    @Test
    public void constructionDoesNotCallSubclassSetters() throws Exception {
        Method method = Commands.class.getMethod("echo", CommandContext.class, String.class, Integer.class);
        SubCommandBundle command = new SubCommandBundle(method.getAnnotation(SubCommand.class), method) {
            @Override
            public void setMethod(Method value) {
                throw new AssertionError("Constructor must not invoke an overridable setter");
            }
        };
        assertEquals(3, command.getParameterCount());
    }

    @Test
    public void completionSupportsImmutableResultsAndIgnoresDefaultLocale() throws Exception {
        HashMap<String, SubCommandBundle> commands = new HashMap<>();
        commands.put("", bundle("root", CommandContext.class, String.class));
        commands.put("Info", bundle("echo", CommandContext.class, String.class, Integer.class));
        TabProcessor processor = new TabProcessor(commands, Commands.class.getAnnotation(Command.class));
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(List.of("Istanbul", "Info"), processor.onTabComplete(sender, null, "test", new String[]{"i"}));
            assertTrue(processor.onTabComplete(sender, null, "test", new String[0]).isEmpty());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void completionTracksEachTokenOfMultiTokenParameters() throws Exception {
        List<Integer> positions = new ArrayList<>();
        var converters = ConverterRegistry.class.getDeclaredField("converters");
        converters.setAccessible(true);
        @SuppressWarnings("unchecked")
        var registry = (java.util.Map<Class<?>, nl.xsyntos.scaffi.framework.commands.converters.IConverter<?>>) converters.get(null);
        var original = registry.put(Vector3.class, new nl.xsyntos.scaffi.framework.commands.converters.Vector3Converter() {
            @Override
            public List<String> tabComplete(CommandContext context, String value, int currentParam) {
                positions.add(currentParam);
                return List.of("10");
            }
        });
        try {
            HashMap<String, SubCommandBundle> commands = new HashMap<>();
            commands.put("move", bundle("move", CommandContext.class, Vector3.class, String.class));
            TabProcessor processor = new TabProcessor(commands, Commands.class.getAnnotation(Command.class));
            processor.onTabComplete(sender, null, "test", new String[]{"move", ""});
            processor.onTabComplete(sender, null, "test", new String[]{"move", "10", ""});
            processor.onTabComplete(sender, null, "test", new String[]{"move", "10", "10", ""});
            assertEquals(List.of(0, 1, 2), positions);
        } finally {
            registry.put(Vector3.class, original);
        }
    }

    @Test
    public void helpDeduplicatesAliasesEvenWhenBundlesAreDistinct() throws Exception {
        Method method = Commands.class.getMethod("echo", CommandContext.class, String.class, Integer.class);
        SubCommand annotation = method.getAnnotation(SubCommand.class);
        HashMap<String, SubCommandBundle> commands = new HashMap<>();
        for (String name : List.of("echo", "say", "repeat"))
            commands.put(name, new SubCommandBundle(annotation, method));
        HelpCommandBundle help = new HelpCommandBundle("test", Commands.class.getAnnotation(HelpCommand.class), annotation, commands);
        assertNotNull(help.executeHelp(null, 1).getComponent());
        assertNull(help.executeHelp(null, 2).getComponent());
    }

    @Test
    public void aliasesShareOneBundleDuringRegistration() {
        // HelpCommand is not inherited, so this fixture needs no running Bukkit server.
        HashMap<String, SubCommandBundle> commands = CommandRegistry.collectSubCommands("test", PlainCommands.class, true);
        assertSame(commands.get("echo"), commands.get("say"));
        assertSame(commands.get("echo"), commands.get("repeat"));
    }

    public static class PlainCommands extends Commands {}

    @Test
    public void playerCompletionFiltersBeforeReturningNames() throws Exception {
        Player alice = player("Alice");
        Player bob = player("Bob");
        Server server = (Server) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Server.class},
            (proxy, method, args) -> method.getName().equals("getOnlinePlayers") ? List.of(alice, bob) : null);
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        try {
            field.set(null, server);
            assertEquals(List.of("Alice"), new PlayerConverter().tabComplete(null, "aL", 0));
            assertEquals(List.of("Alice", "Bob"), new PlayerConverter().tabComplete(null, "", 0));
            assertTrue(new PlayerConverter().tabComplete(null, "nobody", 0).isEmpty());
        } finally {
            field.set(null, previous);
        }
    }

    private Player player(String name) {
        return (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Player.class},
            (proxy, method, args) -> method.getName().equals("getName") ? name : null);
    }
}
