package ru.ogres.tools;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.springframework.data.repository.CrudRepository;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by zed on 22.07.16.
 */
@SupportedAnnotationTypes({ObjectAdminEntityAnnotationProcessor.OAE_TYPE})
public class ObjectAdminEntityAnnotationProcessor extends AbstractProcessor {

    final static String OAE_TYPE = "ru.ogres.tools.ObjectAdminEntity";

    Filer filer;
    Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<Type, TypeElement> finded = new HashMap<>();
        if (!roundEnv.processingOver()) {
            for (TypeElement annotation : annotations) {
                for (Element e : roundEnv.getElementsAnnotatedWith(annotation)) {
                    if (e.getClass().getAnnotation(Entity.class) != null){
                        finded.putIfAbsent(e.getClass(), annotation);
                    }
                }
            }
        }
        build(finded);
        return false;
    }

    private void build(Map<Type, TypeElement> finded) {
        finded.forEach((type, element) -> {
            try {
                String name = String.format("%sRepository", type.getClass().getName());
                Type idType = null;

                for(Field field : type.getClass().getDeclaredFields()){
                    Annotation idAnnotation = field.getAnnotation(Id.class);
                    if (idAnnotation != null){
                        idType = field.getType();
                        break;
                    }
                }

                if (idType == null)
                    for(Method method : type.getClass().getMethods()){
                        Annotation idAnnotation = method.getAnnotation(Id.class);
                        if (idAnnotation != null && method.getName().startsWith("get")){
                            idType = method.getReturnType();
                            break;
                        }
                    }

                if (idType == null){
                    messager.printMessage(Diagnostic.Kind.ERROR, name +" not found @Id annotation");
                    return;
                }


                TypeSpec typeSpec = TypeSpec.interfaceBuilder(name)
                        .addModifiers(Modifier.PUBLIC)
                        .addSuperinterface(ParameterizedTypeName.get(CrudRepository.class, type, idType))
                        .build();

                JavaFile javaFile = JavaFile.builder("ru.ogres.tools.repo", typeSpec)
                        .build();
                javaFile.writeTo(filer);
                messager.printMessage(Diagnostic.Kind.NOTE, String.format("Entity Repo %s builded.", name));
            }catch (IOException e){
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        });


    }

}
