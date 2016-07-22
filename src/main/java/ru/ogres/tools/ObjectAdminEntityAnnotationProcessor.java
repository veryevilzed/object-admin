package ru.ogres.tools;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.persistence.Entity;
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
        
    }

}
