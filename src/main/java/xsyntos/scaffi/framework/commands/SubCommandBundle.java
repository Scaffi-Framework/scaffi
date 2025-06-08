package xsyntos.scaffi.framework.commands;

import java.lang.reflect.Method;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class SubCommandBundle {
    private SubCommand subCommand;
    private Method method;
}
