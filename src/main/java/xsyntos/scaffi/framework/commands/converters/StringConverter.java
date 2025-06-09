package xsyntos.scaffi.framework.commands.converters;

import java.util.List;
import java.util.Collections;

import xsyntos.scaffi.framework.commands.CommandContext;
import xsyntos.scaffi.framework.commands.CommandResponse;
import xsyntos.scaffi.framework.commands.IConverter;

/**
 * The default converter for Strings
 */
public class StringConverter implements IConverter<String> {
    @Override
    public String convert(CommandContext context, String value) {
        return value;
    }
    
    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public List<String> tabComplete(CommandContext context, String value, int currentParam) {
        return Collections.emptyList();
    }

    @Override
    public CommandResponse onError(String value, Exception ex) {
        return CommandResponse.builder()
            .message(value + " is not a valid string!")
            .build();
    }
}
