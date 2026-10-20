package nl.xsyntos.scaffi.framework.commands.fixtures;

import java.util.List;

import nl.xsyntos.scaffi.framework.commands.CommandContext;
import nl.xsyntos.scaffi.framework.commands.CommandResponse;
import nl.xsyntos.scaffi.framework.commands.converters.IConverter;

/** Custom converter fixture for registration from a shared discovery pass. */
public class TokenConverter implements IConverter<TokenConverter.Token> {
    public record Token(String value) {}

    @Override
    public Token convert(CommandContext context, String value) {
        return new Token(value);
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public List<String> tabComplete(CommandContext context, String value, int currentParam) {
        return List.of();
    }

    @Override
    public CommandResponse onError(String value, Exception ex) {
        return CommandResponse.builder().build();
    }
}
