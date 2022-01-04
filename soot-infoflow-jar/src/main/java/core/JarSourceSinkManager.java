package core;

import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.manager.DefaultSourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;

import java.util.Collection;
import java.util.List;

// TODO
public class JarSourceSinkManager extends DefaultSourceSinkManager {

    public JarSourceSinkManager(ISourceSinkDefinitionProvider sourcesAndSinks, List<String> parameterTaintMethods, List<String> returnTaintMethods) {
        super(sourcesAndSinks);
        super.setParameterTaintMethods(parameterTaintMethods);
        super.setReturnTaintMethods(returnTaintMethods);
    }
}
