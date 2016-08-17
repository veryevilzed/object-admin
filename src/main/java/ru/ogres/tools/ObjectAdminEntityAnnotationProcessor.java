package ru.ogres.tools;

import com.squareup.javapoet.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

        List<Data> objectData = new ArrayList<>();

        for (TypeElement typeElement : ElementFilter.typesIn(entityAnnotated)) {
            ObjectAdminEntity objectAdminEntity = typeElement.getAnnotation(ObjectAdminEntity.class);
            if (objectAdminEntity == null || objectAdminEntity.hidden())
                continue;
            objectData.add(new Data(objectAdminEntity.name().equals("") ?  typeElement.getSimpleName().toString() : objectAdminEntity.name(),  typeElement.getSimpleName().toString()));
        }

        for (TypeElement typeElement : ElementFilter.typesIn(entityAnnotated)) {
            System.out.println("Element: " + typeElement.getSimpleName());
            ObjectAdminEntity objectAdminEntity = typeElement.getAnnotation(ObjectAdminEntity.class);
            if (objectAdminEntity == null)
                continue;
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
                TypeSpec repository = null;
                if (objectAdminEntity.createRepositoryAutomaticaly())
                    repository = buildRepository(typeElement, keyType, keyName);
                buildWebHandler(objectAdminEntity, typeElement, keyType, keyName, repository, fields, objectData);
            }catch (IOException e){
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


    private void buildWebHandler(ObjectAdminEntity objectAdminEntity, TypeElement typeElement, TypeMirror keyType, String keyName, TypeSpec repository, Set<Field> fields, List<Data> objectData) throws IOException {
        FieldSpec repositoryField;

        if (repository == null) {
            repositoryField = FieldSpec.builder(
                    ParameterizedTypeName.get(ClassName.get(ObjectAdminRepository.class),
                            TypeName.get(typeElement.asType()),
                            TypeName.get(keyType)), "repo", Modifier.PUBLIC)
                    .addAnnotation(Autowired.class)
                    .build();
        }else{
            repositoryField = FieldSpec.builder(TypeVariableName.get(repository.name), "repo", Modifier.PUBLIC)
                    .addAnnotation(Autowired.class)
                    .build();
        }

        String name = typeElement.getSimpleName() + "WebHandler";





        TypeSpec typeSpec = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Controller.class)
                .addAnnotation(
                        AnnotationSpec.builder(RequestMapping.class)
                                .addMember("path", "$S", "${object-admin.url:/admin/rest}/" + typeElement.getSimpleName().toString())
                                .build()
                )
                .addField(repositoryField)
                .addField(
                        FieldSpec.builder(String.class, "adminPath", Modifier.PUBLIC)
                                .addAnnotation(
                                        AnnotationSpec.builder(Value.class)
                                                .addMember("value", "$S", "${object-admin.url:/admin/rest}/")
                                                .build()
                                )
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
                .addField(
                        FieldSpec.builder(String.class, "schemasPath", Modifier.PUBLIC)
                                .addAnnotation(
                                        AnnotationSpec.builder(Value.class)
                                                .addMember("value", "$S", "${object-admin.schemas-path:/__oa__schemas}")
                                                .build()
                                )
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("list")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(
                                AnnotationSpec.builder(RequestMapping.class)
                                        .addMember("path", "$S", "")
                                        .addMember("method", "$T.GET", RequestMethod.class)
                                        .build()
                        ).addParameter(
                                ParameterSpec.builder(Integer.class, "p")
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
                        .addCode(Tools.buildTable(objectAdminEntity.name().equals("") ?  typeElement.getSimpleName().toString() : objectAdminEntity.name(), typeElement, fields, "p", "defaultPageSize"))
                        .addCode("map.put($S, adminPath+$S);", "current_path", typeElement.getSimpleName().toString())
                        .addCode("map.put($S, $L);\n"+
                                 "return templatePrefix+$S;\n", "oa_static", "staticPath", "list")
                        .build()
                )

                .addMethod(
                        MethodSpec.methodBuilder("detail")
                                .addModifiers(Modifier.PUBLIC)
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
                                .addParameter(ModelMap.class, "map", Modifier.FINAL)
                                .returns(String.class)
                                .addCode("map.put($S, adminPath+$S+$S+$L);", "current_path", typeElement.getSimpleName().toString(), "/", "id")
                                .addCode("map.put($S, repo.findOne($L));", "object", "id")
                                .addCode("map.put($S, $S);", "object_name", objectAdminEntity.name().equals("") ?  typeElement.getSimpleName().toString() : objectAdminEntity.name())
                                .addCode("map.put($S, adminPath+$S);", "back", typeElement.getSimpleName().toString())
                                .addCode("map.put($S, schemasPath+$S+$S);", "schema_url", "/", objectAdminEntity.schema().equals("") ? StringUtils.uncapitalize(typeElement.getSimpleName().toString())+".json" : objectAdminEntity.schema())
                                .addCode("map.put($S, adminPath+$S+$S);", "insert_url", typeElement.getSimpleName().toString(), "/api")
                                .addCode("map.put($S, adminPath+$S+$S+$L);", "update_url", typeElement.getSimpleName().toString(), "/api/", "id")
                                .addCode("map.put($S, $L);\n"+
                                        "return templatePrefix+$S;\n", "oa_static", "staticPath", "detail")
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("schemas")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(ResponseBody.class)
                                .addAnnotation(
                                        AnnotationSpec.builder(RequestMapping.class)
                                                .addMember("path", "$S", "/schema.json")
                                                .addMember("method", "$T.GET", RequestMethod.class)
                                                .build()
                                )
                                .addCode("return $S;\n", "")
                                .returns( String.class )
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("singleGet")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(ResponseBody.class)
                                .addAnnotation(
                                        AnnotationSpec.builder(RequestMapping.class)
                                                .addMember("path", "$S", "/api/{id}")
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
                                                .addMember("path", "$S", "/api")
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
                                                .addMember("path", "$S", "/api/{id}")
                                                .addMember("method", "$T.DELETE", RequestMethod.class)
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


