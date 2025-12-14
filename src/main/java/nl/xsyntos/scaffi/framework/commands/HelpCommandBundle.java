package nl.xsyntos.scaffi.framework.commands;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import nl.xsyntos.scaffi.framework.commands.annotations.HelpCommand;
import nl.xsyntos.scaffi.framework.commands.annotations.SubCommand;
import nl.xsyntos.scaffi.framework.commands.converters.IConverter;

public class HelpCommandBundle extends SubCommandBundle {
    private List<BaseComponent> helpComponents;

    public HelpCommandBundle(String command, HelpCommand helpCommand, SubCommand subCommand, Map<String, SubCommandBundle> subCommands) {
        super(subCommand, null);
        this.helpComponents = generateHelpComponents(command, subCommands, helpCommand);
        this.setMethod(extractMethod());
    }
    
    public CommandResponse executeHelp(CommandContext context, @Nullable Integer page) {
        if(page == null) {
            page = 1;
        }

        if(page < 1 || page > helpComponents.size()) {
            return CommandResponse.builder()
                .message("Invalid help page number. Please enter a number between 1 and " + helpComponents.size())
                .build();
        }


        return CommandResponse.builder()
            .component(helpComponents.get(page - 1))
            .build();
    }

    @Override
    public CommandResponse invoke(Object instance, Object... params) throws Exception {
        CommandContext context = (CommandContext) params[0];
        int page = 1;
        if(params.length > 1 && params[1] != null) {
            page = (Integer) params[1];
        }

        return executeHelp(context, page);
    }

    private Method extractMethod() {
        try {
            return this.getClass().getMethod("executeHelp", CommandContext.class, Integer.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private List<BaseComponent> generateHelpComponents(String command, Map<String, SubCommandBundle> subCommands, HelpCommand helpCommand) {
        List<BaseComponent> helpComponents = new ArrayList<>();
        //generate help components for for all subcommands, split into pages of ENTRIES_PER_PAGE
        Collection<SubCommandBundle> bundles = subCommands.values();
        int totalPages = (int) Math.ceil((double) bundles.size() / helpCommand.entriesPerPage());
        for(int page = 0; page < totalPages; page++) {
            int startIndex = page * helpCommand.entriesPerPage();
            List<SubCommandBundle> pageBundles = bundles.stream().skip(startIndex).limit(helpCommand.entriesPerPage()).toList();
            BaseComponent helpComponent = generateHelpComponent(pageBundles, command, page + 1, page == 0, page == totalPages - 1, helpCommand);
            helpComponents.add(helpComponent);
        }
        return helpComponents;
    }

    private BaseComponent generateHelpComponent(Collection<SubCommandBundle> bundle, String command, int currentPage, boolean isFirstPage, boolean isLastPage, HelpCommand helpCommand) {
        ComponentBuilder builder = new ComponentBuilder();
        builder.append(generateHeader(helpCommand));
        for(SubCommandBundle subCommandBundle : bundle) {
            boolean lastOfPage = subCommandBundle.equals(((List<SubCommandBundle>)bundle).get(((List<SubCommandBundle>)bundle).size() -1));
            builder.append(generateCommandEntry(command, subCommandBundle, lastOfPage, helpCommand));
        }
        builder.append(generateFooter(command, currentPage, isFirstPage, isLastPage));

        return builder.build();
    }

    private BaseComponent generateHeader(HelpCommand helpCommand) {
        ComponentBuilder builder = new ComponentBuilder();
        builder.append("----- ")
            .color(ChatColor.getByChar(helpCommand.primaryColor()))
            .append(helpCommand.headerText())
            .color(ChatColor.getByChar(helpCommand.secondaryColor()))
            .append(" -----\n")
            .color(ChatColor.getByChar(helpCommand.primaryColor()));
        return builder.build();
    }

    private BaseComponent generateFooter(String command, int currentPage, boolean isFirstPage, boolean isLastPage) {
        ComponentBuilder builder = new ComponentBuilder().append("\n");
        if(!isFirstPage) {
            TextComponent prevPage = new TextComponent(" <Previous Page> ");
            prevPage.setColor(net.md_5.bungee.api.ChatColor.GREEN);
            prevPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + command + " help " + (currentPage - 1)));
            builder.append(prevPage);
        }
        if(!isLastPage) {
            TextComponent nextPage = new TextComponent(" <Next Page> ");
            nextPage.setColor(net.md_5.bungee.api.ChatColor.GREEN);
            nextPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + command + " help " + (currentPage + 1)));
            builder.append(nextPage);
        }
        return builder.build();
    }

    private BaseComponent generateCommandEntry(String command, SubCommandBundle bundle, boolean lastOfPage, HelpCommand helpCommand) {
        ComponentBuilder builder = new ComponentBuilder();
        builder.append("/" + command + " ").color(ChatColor.getByChar(helpCommand.primaryColor())).append(bundle.getSubCommand().command()).color(ChatColor.getByChar(helpCommand.primaryColor()));
        Parameter[] parameters = bundle.getParameters();
        for(int i = 1; i < parameters.length; i++) {
            Parameter param = parameters[i];
            builder.append(" ").append(transformParam(param));
        }
        if(!bundle.getSubCommand().description().isEmpty()) {
            builder.append(": ").append(bundle.getSubCommand().description()).color(ChatColor.getByChar(helpCommand.secondaryColor()));
        }

        if(!lastOfPage) {
            builder.append("\n");
        }

        return builder.build();
    }

    private BaseComponent transformParam(Parameter param) {
        boolean isOptional = param.isAnnotationPresent(Nullable.class);
        IConverter<?> c = ConverterRegistry.getConverter(param.getType());
        String typeName = c != null ? c.getTypeName() : param.getType().getSimpleName();

        ComponentBuilder builder = new ComponentBuilder();
        if(isOptional)
            builder.append("[");

        builder.append("<")
            .append(typeName)
            .append(">");
        
        if(isOptional)
            builder.append("]");

        return builder.build();
    }
}
