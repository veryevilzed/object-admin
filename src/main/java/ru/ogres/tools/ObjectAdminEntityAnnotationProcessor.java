package ru.ogres.tools;

import com.squareup.javapoet.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.HashSet;
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

        for (TypeElement typeElement : ElementFilter.typesIn(entityAnnotated)) {
            System.out.println("Element: " + typeElement.getSimpleName());

            TypeMirror keyType = null;
            String keyName = null;
            Set<Field> fields = new HashSet<>();
            for (VariableElement variableElement : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
                Field f = new Field();
                ObjectAdminField objectAdminField = variableElement.getAnnotation(ObjectAdminField.class);
                f.field = variableElement.getSimpleName().toString();
                if (objectAdminField != null) {
                    f.name = objectAdminField.name().equals("") ? variableElement.getSimpleName().toString() : objectAdminField.name();
                    f.type = objectAdminField.type().equals("") ? variableElement.asType().toString() : objectAdminField.type();
                    f.description = objectAdminField.description();
                    f.index = objectAdminField.index();
                }else {
                    f.name = variableElement.getSimpleName().toString();
                    f.type = variableElement.asType().toString();
                }
                fields.add(f);

                System.out.println("Field: " + variableElement.getSimpleName());
                if (variableElement.getAnnotation(ObjectAdminId.class) != null) {
                    keyName = variableElement.getSimpleName().toString();
                    keyType = variableElement.asType();
                    System.out.println(" Name:" + keyName + " Type:" + keyType.toString());
                }
            }

            if (keyName == null)
                messager.printMessage(Diagnostic.Kind.ERROR, "@Id field not found");
            try {
                TypeSpec repository = buildRepository(typeElement, keyType, keyName);
                buildWebHandler(typeElement, keyType, keyName, repository, fields);
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
                    .addSuperinterface(ParameterizedTypeName.get(ClassName.get(PagingAndSortingRepository.class),
                            TypeName.get(typeElement.asType()),
                            TypeName.get(keyType)))
                    .build();

            JavaFile javaFile = JavaFile.builder("ru.ogres.tools.repo", typeSpec)
                    .build();
            javaFile.writeTo(filer);
            return typeSpec;
    }



    private void buildWebHandler(TypeElement typeElement, TypeMirror keyType, String keyName, TypeSpec repository, Set<Field> fields) throws IOException {
        if (repository == null)
            return;

        String name = typeElement.getSimpleName() + "WebHandler";


        TypeSpec.Builder b = repository.toBuilder();

        TypeSpec typeSpec = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Controller.class)
                .addAnnotation(
                        AnnotationSpec.builder(RequestMapping.class)
                                .addMember("path", "$S", "${object-admin.url:/admin/rest}/" + typeElement.getSimpleName().toString())
                                .build()
                )

                .addField(
                        FieldSpec.builder(TypeVariableName.get(repository.name), "repo", Modifier.PUBLIC)
                                .addAnnotation(Autowired.class)
                                .build()
                )
                .addField(
                        FieldSpec.builder(Integer.class, "defaultPageSize", Modifier.PUBLIC)
                                .addAnnotation(
                                        AnnotationSpec.builder(Value.class)
                                                .addMember("value", "$S", "${object-admin.page-size:50}")
                                                .build()
                                )
                                .build()
                )
                .addField(
                        FieldSpec.builder(String.class, "staticPath", Modifier.PUBLIC)
                                .addAnnotation(
                                        AnnotationSpec.builder(Value.class)
                                                .addMember("value", "$S", "${object-admin.static-path:/__oa__static}")
                                                .build()
                                )
                                .build()
                )
                .addField(
                        FieldSpec.builder(String.class, "templatePrefix", Modifier.PUBLIC)
                                .addAnnotation(
                                        AnnotationSpec.builder(Value.class)
                                                .addMember("value", "$S", "${object-admin.template-prefix:__oa__}")
                                                .build()
                                )
                                .build()
                )
//                .addMethod(
//                        MethodSpec.methodBuilder("list")
//                        .addModifiers(Modifier.PUBLIC)
//                        .addAnnotation(ResponseBody.class)
//                        .addAnnotation(
//                                AnnotationSpec.builder(RequestMapping.class)
//                                        .addMember("path", "$S", "")
//                                        .addMember("method", "$T.GET", RequestMethod.class)
//                                        .build()
//                        ).addParameter(
//                                ParameterSpec.builder(Integer.class, "page")
//                                        .addAnnotation(
//                                                AnnotationSpec.builder(RequestParam.class)
//                                                        .addMember("name", "$S", "page")
//                                                        .addMember("required", "$L", "false")
//                                                        .addMember("defaultValue", "$S", "0")
//                                                        .build()
//                                        )
//                                        .build()
//                        )
//                        .returns( ParameterizedTypeName.get(ClassName.get("org.springframework.data.domain", "Page"), ClassName.get(typeElement) ))
//                        .addCode("return repo.findAll(new $T(page,defaultPageSize));", PageRequest.class)
//                        .build()
//                )
                .addMethod(
                        MethodSpec.methodBuilder("list")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(
                                AnnotationSpec.builder(RequestMapping.class)
                                        .addMember("path", "$S", "")
                                        .addMember("method", "$T.GET", RequestMethod.class)
                                        .build()
                        ).addParameter(
                                ParameterSpec.builder(Integer.class, "page")
                                        .addAnnotation(
                                                AnnotationSpec.builder(RequestParam.class)
                                                        .addMember("name", "$S", "page")
                                                        .addMember("required", "$L", "false")
                                                        .addMember("defaultValue", "$S", "0")
                                                        .build()
                                        )
                                        .build()
                        )
                        .addParameter(ModelMap.class, "map", Modifier.FINAL)
                        .returns(String.class)
                        //.addCode(Field.build(fields))
                        .addCode(Table.build(typeElement, fields))
                        //.addCode("map.put($S, $L);\n", "fields", "fields")
                        //.addCode("map.put($S, repo.findAll());\n", "data")
                        .addCode("map.put($S, $L);\n"+
                                 "return templatePrefix+$S;\n", "oa_static", "staticPath", "index")
                        .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("singleGet")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(ResponseBody.class)
                                .addAnnotation(
                                        AnnotationSpec.builder(RequestMapping.class)
                                                .addMember("path", "$S", "/{id}")
                                                .addMember("method", "$T.GET", RequestMethod.class)
                                                .build()
                                )
                                .addParameter(
                                        ParameterSpec.builder(ClassName.get(keyType), "id")
                                                .addAnnotation(
                                                    AnnotationSpec.builder(PathVariable.class)
                                                        .build()
                                                )
                                        .build()
                                )
                                .addCode("return repo.findOne(id);\n")
                                .returns( ClassName.get(typeElement) )
                        .build()
                ).addMethod(
                        MethodSpec.methodBuilder("singlePost")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(ResponseBody.class)
                                .addAnnotation(
                                        AnnotationSpec.builder(RequestMapping.class)
                                                .addMember("path", "$S", "")
                                                .addMember("method", "{$T.POST, $T.PUT}", RequestMethod.class, RequestMethod.class)
                                                .build()
                                )
                                .addParameter(

                                        ParameterSpec.builder(ClassName.get(typeElement), "object")
                                                .addAnnotation(
                                                        AnnotationSpec.builder(PathVariable.class)
                                                                .build()
                                                )
                                                .build()
                                )
                                .addCode("repo.save(object);\n" +
                                        "return object;\n")
                                .returns( ClassName.get(typeElement) )
                                .build()
                ).addMethod(
                        MethodSpec.methodBuilder("delete")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(ResponseBody.class)
                                .addAnnotation(
                                        AnnotationSpec.builder(RequestMapping.class)
                                                .addMember("path", "$S", "/{id}")
                                                .addMember("method", "$T.DELETE", RequestMethod.class)
                                                .build()
                                )
                                .addParameter(
                                        ParameterSpec.builder(ClassName.get(typeElement), "id")
                                                .addAnnotation(
                                                        AnnotationSpec.builder(PathVariable.class)
                                                                .build()
                                                )
                                                .build()
                                )
                                .addCode("repo.delete(id);\n" +
                                        "return $S;", "OK")
                                .returns(String.class)
                                .build()
                )

                .build();

        JavaFile javaFile = JavaFile.builder("ru.ogres.tools.repo", typeSpec)
                .build();
        javaFile.writeTo(filer);


    }

}


