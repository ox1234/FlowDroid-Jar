package core;

import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.jimple.infoflow.entryPointCreators.BaseEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.SequentialEntryPointCreator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

// TODO
public class JarEntryPointCreator extends SequentialEntryPointCreator {
    public JarEntryPointCreator(){
        super(Collections.singleton("<org.apache.logging.log4j.spi.AbstractLogger: void info(java.lang.String)>"));
    }

    @Override
    protected SootMethod createDummyMainInternal() {
        return super.createDummyMainInternal();
    }

    @Override
    public Collection<String> getRequiredClasses() {
        return super.getRequiredClasses();
    }

    @Override
    public Collection<SootMethod> getAdditionalMethods() {
        return super.getAdditionalMethods();
    }

    @Override
    public Collection<SootField> getAdditionalFields() {
        return super.getAdditionalFields();
    }
}
