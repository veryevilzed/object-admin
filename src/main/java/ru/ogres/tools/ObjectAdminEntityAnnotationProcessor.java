package ru.ogres.tools;

import com.fasterxml.classmate.types.TypePlaceHolder;
import com.squareup.javapoet.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.persistence.Id;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
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
                if (variableElement.getAnnotation(Id.class) != null) {
                    keyName = variableElement.getSimpleName().toString();
                    keyType = variableElement.asType();
                    System.out.println(" Name:" + keyName + " Type:" + keyType.toString());
                }
            }

            if (keyName == null)
                messager.printMessage(Diagnostic.Kind.ERROR, "@Id field not found");
            try {
                TypeSpec repository = buildRepository(typeElement, keyType, keyName);
                buildWebHandler(typeElement, keyType, keyName, repository);
            }catch (Exception e){
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }
    }

    private TypeSpec buildRepository(TypeElement typeElement, TypeMirror keyType, String keyName) throws IOException {

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
            return typeSpec;
    }

    private void buildWebHandler(TypeElement typeElement, TypeMirror keyType, String keyName, TypeSpec repository) throws IOException {
        if (repository == null)
            return;

        String name = typeElement.getSimpleName() + "WebHandler";


        TypeSpec.Builder b = repository.toBuilder();

        TypeSpec typeSpec = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Controller.class)
                .addAnnotation(
                        AnnotationSpec.builder(RequestMapping.class)
                                .addMember("path", "$S", "/" + typeElement.getSimpleName().toString())
                                .build()
                )

                .addField(
                        FieldSpec.builder(TypeVariableName.get(repository.name), "repo", Modifier.PUBLIC)
                                .addAnnotation(Autowired.class)
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("list")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(ResponseBody.class)
                        .addAnnotation(
                                AnnotationSpec.builder(RequestMapping.class)
                                        .addMember("path", "$S", "")
                                        .build()
                        )
                        .returns( ParameterizedTypeName.get(ClassName.get("java.util", "List"), ClassName.get(typeElement) ))
                        .addCode("List<$T> res = new $T<>();\n" +
                                "repo.findAll().forEach(res::add);\n" +
                                "return res;\n", typeElement, ArrayList.class)
                        .build()
                )
                .build();

        JavaFile javaFile = JavaFile.builder("ru.ogres.tools.repo", typeSpec)
                .build();
        javaFile.writeTo(filer);


    }

}


