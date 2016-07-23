package ru.ogres.tools;

import com.squareup.javapoet.*;
import org.springframework.data.repository.CrudRepository;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
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

            TypeMirror keyType = null;
            String keyName = null;

            for (VariableElement variableElement : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
                System.out.println("Field: " + variableElement.getSimpleName());
                if ( variableElement.getAnnotation(Id.class) != null){
                    keyName = variableElement.getSimpleName().toString();
                    keyType = variableElement.asType();
                    System.out.println(" Name: " + keyName + " Type:" + keyType.toString());
                }
            }

            if (keyName == null)
                messager.printMessage(Diagnostic.Kind.ERROR, "@Id field not found");
            build(typeElement, keyType, keyName);
        }
    }


    private void build(TypeElement typeElement, TypeMirror keyType, String keyName) {

        try{
            String name = typeElement.getSimpleName() + "Repository";

            TypeSpec typeSpec = TypeSpec.interfaceBuilder(name)
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(ParameterizedTypeName.get(ClassName.get(CrudRepository.class),
                            TypeName.get(typeElement.asType()),
                            TypeName.get(keyType)))
                    .build();

            JavaFile javaFile = JavaFile.builder("ru.ogres.tools.repo", typeSpec)
                    .build();
            javaFile.writeTo(filer);
        }catch (IOException e){
            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }



    }

}


