package core;

import soot.FastHierarchy;
import soot.Scene;
import soot.SootClass;

import java.util.Collection;
import java.util.Set;

public class ModifiableFastHierarchy extends FastHierarchy {

    public ModifiableFastHierarchy(){
        Scene.v().getOrMakeFastHierarchy();
    }

    public void replaceSubClasses(SootClass superClass, Set<SootClass> subClasses){
        classToSubclasses.remove(superClass);
        classToSubclasses.putAll(superClass, subClasses);
    }
}
