package core;

import soot.SootMethod;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;

public class JarInfoflow extends Infoflow {
    SootMethod currentRealEntry;
    public JarInfoflow(InfoflowJarConfiguration config){
        super.config = config;
    }

    @Override
    public void computeInfoflow(String appPath, String libPath, IEntryPointCreator entryPointCreator, ISourceSinkManager sourcesSinks) {
//        config.setSootIntegrationMode(InfoflowConfiguration.SootIntegrationMode.UseExistingInstance);
        super.computeInfoflow(appPath, libPath, entryPointCreator, sourcesSinks);
    }
}
