import com.sun.tools.corba.se.idl.StringGen;
import core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.android.results.xml.InfoflowResultsSerializer;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.entryPointCreators.SequentialEntryPointCreator;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.methodSummary.data.provider.MemorySummaryProvider;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.taintWrappers.ITaintWrapperDataFlowAnalysis;
import soot.jimple.infoflow.taintWrappers.TaintWrapperSet;
import soot.options.Options;
import util.ClassHierarchyUtil;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class SetupJarApplication implements ITaintWrapperDataFlowAnalysis {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    protected ISourceSinkDefinitionProvider sourceSinkProvider;

    protected InfoflowJarConfiguration config;
    JarEntryPointCreator entryPointCreator = null;

    protected Set<SootClass> entrypoints = null;

    protected ITaintPropagationWrapper taintWrapper;

    protected ISourceSinkManager sourceSinkManager = null;

    protected IInfoflowConfig sootConfig = new SootConfigForJar();
    protected BiDirICFGFactory cfgFactory = null;

    protected Set<Stmt> collectedSources = null;
    protected Set<Stmt> collectedSinks = null;

    protected Set<PreAnalysisHandler> preprocessors = new HashSet<>();
    protected Set<ResultsAvailableHandler> resultsAvailableHandlers = new HashSet<>();
    protected TaintPropagationHandler taintPropagationHandler = null;
    protected TaintPropagationHandler backwardsPropagationHandler = null;

    protected JarInfoflow infoflow;

    /**
     * Class for aggregating the data flow results obtained through multiple runs of
     * the data flow solver.
     *
     * @author Steven Arzt
     *
     */
    private static class MultiRunResultAggregator implements ResultsAvailableHandler {

        private final InfoflowResults aggregatedResults = new InfoflowResults();
        private InfoflowResults lastResults = null;
        private IInfoflowCFG lastICFG = null;

        @Override
        public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results) {
            this.aggregatedResults.addAll(results);
            this.lastResults = results;
            this.lastICFG = cfg;
        }

        /**
         * Gets all data flow results aggregated so far
         *
         * @return All data flow results aggregated so far
         */
        public InfoflowResults getAggregatedResults() {
            return this.aggregatedResults;
        }

        /**
         * Gets the total number of source-to-sink connections from the last partial
         * result that was added to this aggregator
         *
         * @return The results from the last run of the data flow analysis
         */
        public InfoflowResults getLastResults() {
            return this.lastResults;
        }

        /**
         * Clears the stored result set from the last data flow run
         */
        public void clearLastResults() {
            this.lastResults = null;
            this.lastICFG = null;
        }

        /**
         * Gets the ICFG that was returned together with the last set of data flow
         * results
         *
         * @return The ICFG that was returned together with the last set of data flow
         *         results
         */
        public IInfoflowCFG getLastICFG() {
            return this.lastICFG;
        }

    }


    public SetupJarApplication(InfoflowJarConfiguration config){
        this.config = config;
    }

    public InfoflowResults runInfoflow() throws IOException {
        String sourceSinkFile = config.getSourceSinkFile();
        if(sourceSinkFile == null || sourceSinkFile.isEmpty()){
            throw new RuntimeException("No source/sink file specified for the data flow analysis");
        }
        String fileExtension = sourceSinkFile.substring(sourceSinkFile.lastIndexOf("."));
        ISourceSinkDefinitionProvider parser = null;
        if (fileExtension.equals(".txt")) {
            parser = PermissionMethodParser.fromFile(sourceSinkFile);
        }
        return runInfoflow(parser);
    }

    public InfoflowResults runInfoflow(ISourceSinkDefinitionProvider sourcesAndSinks){
        this.collectedSources = new HashSet<>();
        this.collectedSinks = new HashSet<>();
        this.sourceSinkProvider = sourcesAndSinks;
        this.infoflow = null;

        if(config.getSootIntegrationMode() == InfoflowConfiguration.SootIntegrationMode.CreateNewInstance){
            G.reset();
            initializeSoot();
        }

        parseEntryPoints();

        MultiRunResultAggregator resultAggregator = new MultiRunResultAggregator();

        if(entrypoints == null || entrypoints.isEmpty()){
            logger.warn("No entry points");
            return null;
        }

        List<SootClass> entryPointWorklist = new ArrayList<>(entrypoints);
        while(!entryPointWorklist.isEmpty()){
            SootClass entrypoint = entryPointWorklist.remove(0);
            processEntryPoint(sourcesAndSinks, resultAggregator, entryPointWorklist.size(), entrypoint);
        }

        this.infoflow = null;
        resultAggregator.clearLastResults();
        return resultAggregator.getAggregatedResults();
    }

    protected void processEntryPoint(ISourceSinkDefinitionProvider sourcesAndSinks,
                                     MultiRunResultAggregator resultAggregator,
                                     int numEntryPoints,
                                     SootClass entrypoint){
        resultAggregator.clearLastResults();

        createSourceSinkManager();

        final Collection<? extends ISourceSinkDefinition> sources = getSources();
        final Collection<? extends ISourceSinkDefinition> sinks = getSinks();
        logger.info("Running data flow analysis with {} sources and {} sinks...",
                sources == null ? 0 : sources.size(), sinks == null ? 0 : sinks.size());

        createMainMethod();

        infoflow = createInfoflow();
        infoflow.addResultsAvailableHandler(resultAggregator);
//        infoflow.setTaintPropagationHandler(taintPropagationHandler);
        infoflow.setTaintWrapper(this.taintWrapper);

        infoflow.computeInfoflow(String.join(File.pathSeparator, config.getJarFiles()), null, entryPointCreator, sourceSinkManager);

//        infoflow.runAnalysis(sourceSinkManager, entryPointCreator.getGeneratedMainMethod());
    }

    private JarInfoflow createInfoflow(){
        if (config.getSootIntegrationMode().needsToBuildCallgraph()) {
            if (entryPointCreator == null)
                throw new RuntimeException("No entry point available");
        }

        return createInfoflowInternal();
    }

    protected JarInfoflow createInfoflowInternal(){
        return new JarInfoflow(config);
    }

    private void createMainMethod(){
       entryPointCreator = createEntryPointCreator();
       entryPointCreator.setSubstituteClasses(Collections.singletonList("org.apache.logging.log4j.core.Logger"));
       entryPointCreator.setSubstituteCallParams(true);
    }

    private JarEntryPointCreator createEntryPointCreator(){
        return new JarEntryPointCreator();
    }

    public Collection<? extends ISourceSinkDefinition> getSources() {
        return this.sourceSinkProvider == null ? null : this.sourceSinkProvider.getSources();
    }

    public Collection<? extends ISourceSinkDefinition> getSinks() {
        return this.sourceSinkProvider == null ? null : this.sourceSinkProvider.getSinks();
    }

    public void createSourceSinkManager(){
        List<String> parameterTaintMethods = new ArrayList<>();
        parameterTaintMethods.add("<org.apache.logging.log4j.spi.AbstractLogger: void info(java.lang.String)>");
        this.sourceSinkManager = new JarSourceSinkManager(sourceSinkProvider, parameterTaintMethods, null);
    }

    protected void parseEntryPoints(){
        List<String> entries = config.getEntryClasses();
        this.entrypoints = new HashSet<>(entries.size());
        for(String className : entries){
            SootClass sc = Scene.v().getSootClassUnsafe(className);
            if(sc != null)
                this.entrypoints.add(sc);
        }
    }

    private void initializeSoot(){
        logger.info("Initializing Soot...");

        G.reset();
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        Options.v().set_process_dir(config.getJarFiles());
        Options.v().set_prepend_classpath(true);
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_keep_offset(false);
        Options.v().set_keep_line_number(true);
        Options.v().set_ignore_resolution_errors(true);
        Options.v().set_debug(true);
        if(sootConfig != null)
            sootConfig.setSootOptions(Options.v(), config);

        Options.v().set_soot_classpath(getClasspath());
        Main.v().autoSetOptions();
        configureCallgraph();

        logger.info("Loading jar classes...");
        Scene.v().loadNecessaryClasses();

        ClassHierarchyUtil classHierarchyUtil = new ClassHierarchyUtil();
//        classHierarchyUtil.replaceSubClasses(
//                "org.apache.logging.log4j.core.pattern.LogEventPatternConverter",
//                Collections.singletonList("org.apache.logging.log4j.core.pattern.MessagePatternConverter"));

        PackManager.v().getPack("wjpp").apply();
    }

    private String getClasspath(){
        String classPath = String.join(File.pathSeparator, config.getJarFiles());
        classPath = classPath + File.pathSeparator + Scene.defaultJavaClassPath();
        return classPath;
    }

    protected void configureCallgraph() {
        switch (config.getCallgraphAlgorithm()) {
            case AutomaticSelection:
            case SPARK:
                Options.v().setPhaseOption("cg.spark", "on");
                break;
            case GEOM:
                Options.v().setPhaseOption("cg.spark", "on");
                AbstractInfoflow.setGeomPtaSpecificOptions();
                break;
            case CHA:
                Options.v().setPhaseOption("cg.cha", "on");
                Options.v().setPhaseOption("cg.cha", "verbose:true");
                break;
            case RTA:
                Options.v().setPhaseOption("cg.spark", "on");
                Options.v().setPhaseOption("cg.spark", "rta:true");
                Options.v().setPhaseOption("cg.spark", "on-fly-cg:false");
                break;
            case VTA:
                Options.v().setPhaseOption("cg.spark", "on");
                Options.v().setPhaseOption("cg.spark", "vta:true");
                break;
            default:
                throw new RuntimeException("Invalid callgraph algorithm");
        }
        if (config.getEnableReflection())
            Options.v().setPhaseOption("cg", "types-for-invoke:true");
        configureCustomCallGraph();
    }

    protected void configureCustomCallGraph(){
//        Options.v().setPhaseOption("cg", "library:signature-resolution");
//        Options.v().setPhaseOption("cg", "safe-newinstance:true");
        Options.v().setPhaseOption("cg.spark", "field-based:true");
        Options.v().setPhaseOption("cg.spark", "types-for-sites:true");
        Options.v().setPhaseOption("cg.spark", "empties-as-allocs:true");
        Options.v().setPhaseOption("cg.spark", "propagator:iter");
    }

    @Override
    public void setTaintWrapper(ITaintPropagationWrapper taintWrapper) {
        this.taintWrapper = taintWrapper;
    }

    @Override
    public ITaintPropagationWrapper getTaintWrapper() {
        return null;
    }
}
