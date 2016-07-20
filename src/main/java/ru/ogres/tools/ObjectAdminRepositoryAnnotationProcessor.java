package ru.ogres.tools;

import org.springframework.data.repository.CrudRepository;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Created by zed on 20.07.16.
 */
@SupportedAnnotationTypes({ObjectAdminRepositoryAnnotationProcessor.OAR_TYPE})
public class ObjectAdminRepositoryAnnotationProcessor extends AbstractProcessor {

    final static String OAR_TYPE = "ru.ogres.tools.ObjectAdminRepository";

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

        if (!roundEnv.processingOver()) {
            for (TypeElement annotation : annotations) {
                for(Element e : roundEnv.getElementsAnnotatedWith(annotation)) {
                    //Type[] genericTypes =((ParameterizedType)(e.getClass().getGenericInterfaces())).getActualTypeArguments();
                    for(int i=0;i<e.getClass().getInterfaces().length;i++){
                        if (e.getClass().getInterfaces()[i] == CrudRepository.class){
                            this.build(
                                    ((ParameterizedType)(e.getClass().getGenericInterfaces()[i])).getActualTypeArguments()[0],
                                    ((ParameterizedType)(e.getClass().getGenericInterfaces()[i])).getActualTypeArguments()[1]
                                    );
                        }
                    }
                }
            }
        }

        return true;
    }

    private void build(Type entityType, Type keyType) {
        messager.printMessage(Diagnostic.Kind.NOTE, "Build repository for:" + entityType.getTypeName());
    }

}
