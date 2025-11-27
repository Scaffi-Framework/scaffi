package xsyntos.scaffi.framework.commands.processors;

import javax.annotation.Nullable;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.google.auto.service.AutoService;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import xsyntos.scaffi.framework.commands.CommandRegistry;
import xsyntos.scaffi.framework.commands.SubCommandBundle;
import xsyntos.scaffi.framework.commands.annotations.HelpCommand;

import java.io.Writer;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Set;

@AutoService(Processor.class)
@javax.annotation.processing.SupportedAnnotationTypes("xsyntos.scaffi.framework.commands.annotations.HelpCommand")
@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_21)
public class HelpCommandProcessor extends AbstractProcessor  {
    
    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        elementUtils = env.getElementUtils();
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(HelpCommand.class)) {
            if (element.getKind() != ElementKind.CLASS) continue;

            TypeElement typeElement = (TypeElement) element;
            Class<?> clazz = element.getKind().getClass();
            HashMap<String, SubCommandBundle> bundles = CommandRegistry.collectSubCommands(clazz, true);


            String originalClassName = typeElement.getSimpleName().toString();
            String packageName = elementUtils.getPackageOf(typeElement).toString();
            String generatedClassName = originalClassName + "_HelpGenerated";
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "HelpCommandProcessor running on " + typeElement.getSimpleName());

            try {
                JavaFileObject file = processingEnv.getFiler()
                    .createSourceFile(packageName + "." + generatedClassName);
                try (Writer writer = file.openWriter()) {
                    writer.write("package " + packageName + ";\n\n");
                    writer.write("public class " + generatedClassName + " {\n\n");
                    writer.write("    public void help() {\n");
                    writer.write("        System.out.println(\"Help command executed for " + originalClassName + "\");\n");
                    writer.write("    }\n");
                    writer.write("}\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }



    private BaseComponent transformParam(Parameter param) {
        boolean isOptional = param.isAnnotationPresent(Nullable.class);
        
        ComponentBuilder builder = new ComponentBuilder();
        if(isOptional)
            builder.append("[");

        builder.append("<")
            .append(param.getType().getName())
            .append(">");
        
        if(isOptional)
            builder.append("]");

        return builder.build();
    }
    
}