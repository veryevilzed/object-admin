package ru.ogres.tools;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.springframework.data.repository.CrudRepository;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
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
    private ElementTypePair objectAdminEntityType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.objectAdminEntityType = getType(OAE_TYPE);
    }

    private Types typeUtils() {
        return processingEnv.getTypeUtils();
    }

    private ElementTypePair getType(String className) {
        TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(className);
        DeclaredType declaredType = typeUtils().getDeclaredType(typeElement);
        return new ElementTypePair(typeElement, declaredType);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        checkObjectAdminEntityAnnotatedElement(roundEnv);
        return false;
    }

    private void checkObjectAdminEntityAnnotatedElement(RoundEnvironment roundEnv){
        Set<? extends Element> entityAnnotated =
                roundEnv.getElementsAnnotatedWith(objectAdminEntityType.element);
        // technically, we don't need to filter here, but it gives us a free cast
        for (TypeElement typeElement : ElementFilter.typesIn(entityAnnotated)) {
            System.out.println("Element: " + typeElement.getSimpleName());
            for (VariableElement variableElement : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
                System.out.println("Field: " + variableElement.getSimpleName());
            }
        }
    }


    private void build(Map<Element, TypeElement> finded) {
        finded.forEach((type, element) -> {
            try {
                String name = String.format("%sRepository", type.getSimpleName());
                Type idType = null;

                for(Field field : element.getClass().getFields()){
                    System.out.println("1.Field:" + field.getName());
                }

                System.out.println("");
                for(Field field : type.getClass().getFields()){
                    System.out.println("Field:" + field.getName());
                    if (field.getAnnotationsByType(Id.class).length > 0){
                        System.out.println("Found");
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
                        .addSuperinterface(ParameterizedTypeName.get(CrudRepository.class, type.getClass(), idType))
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


class ElementTypePair {
    public ElementTypePair(TypeElement element, DeclaredType type) {
        this.element = element;
        this.type = type;
    }

    final TypeElement element;
    final DeclaredType type;
}