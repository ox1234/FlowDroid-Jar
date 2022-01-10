package util;

import core.ModifiableFastHierarchy;
import soot.FastHierarchy;
import soot.Hierarchy;
import soot.Scene;
import soot.SootClass;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.util.MultiMap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class ClassHierarchyUtil {
    private final FastHierarchy hierarchy;

    public ClassHierarchyUtil(){
        hierarchy = Scene.v().getOrMakeFastHierarchy();
    }

    public void replaceSubClasses(String superClass, Collection<String> subClasses){
        SootClass superSootClass = Scene.v().getSootClassUnsafe(superClass);
        if(superSootClass.isPhantom()){
           return;
        }
        Set<SootClass> subSootClasses = new ConcurrentHashSet<>();
        for(String subClass : subClasses){
            SootClass subSootClass = Scene.v().getSootClassUnsafe(subClass);
            if(!subSootClass.isPhantom()){
                subSootClasses.add(subSootClass);
            }
        }
        try{
            // change subClass
            Field subClassField = FastHierarchy.class.getDeclaredField("classToSubclasses");
            subClassField.setAccessible(true);
            MultiMap<SootClass, SootClass> classToSubclasses = (MultiMap<SootClass, SootClass>) subClassField.get(hierarchy);
            classToSubclasses.remove(superSootClass);
            classToSubclasses.putAll(superSootClass, subSootClasses);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
