import core.InfoflowJarConfiguration;
import org.apache.commons.cli.*;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.cmd.AbortAnalysisException;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.generator.SummaryGenerationTaintWrapper;
import soot.jimple.infoflow.methodSummary.generator.SummaryGenerator;
import soot.jimple.infoflow.methodSummary.generator.gaps.GapManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

public class MainClass {
    protected final Options options = new Options();
    protected SetupJarApplication analyzer = null;

    // options
    private static final String OPTION_JAR_DIR = "d";
    private static final String OPTION_OUTPUT_DIR = "o";
    private static final String OPTION_SOURCE_SINK_FILE = "s";
    private static final String OPTION_ENTRY_CLASS = "e";

    protected MainClass() {
        initializeCommandLineOptions();
    }

    private void initializeCommandLineOptions() {
        options.addOption("?", "help", false, "Print this help message");
        options.addOption(OPTION_JAR_DIR, "jardir", true, "jars dir path");
        options.addOption(OPTION_OUTPUT_DIR, "output", false, "output path");
        options.addOption(OPTION_SOURCE_SINK_FILE, "sourcesink", true, "source sink file path");
        options.addOption(OPTION_ENTRY_CLASS, "entry", true, "entry class name");
    }

    public static void main(String[] args) throws Exception {
        MainClass main = new MainClass();
        main.run(args);
    }

    protected void run(String[] args) throws Exception {
        // We need proper parameters
        final HelpFormatter formatter = new HelpFormatter();
        if (args.length == 0) {
            formatter.printHelp("soot-infoflow-cmd [OPTIONS]", options);
            return;
        }

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            // Do we need to display the user manual?
            if (cmd.hasOption("?") || cmd.hasOption("help")) {
                formatter.printHelp("soot-infoflow-cmd [OPTIONS]", options);
                return;
            }

            final InfoflowJarConfiguration config = new InfoflowJarConfiguration();
            setInfoFlowConfiguration(cmd, config);
            config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.CHA);
            config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
            config.setStaticFieldTrackingMode(InfoflowConfiguration.StaticFieldTrackingMode.ContextFlowInsensitive);
            config.setStaticFieldTrackingMode(InfoflowConfiguration.StaticFieldTrackingMode.None);
            config.setAliasingAlgorithm(InfoflowConfiguration.AliasingAlgorithm.None);
            ITaintPropagationWrapper taintWrapper = initializeTaintWrapper();


            analyzer = createFlowDroidInstance(config);
            analyzer.setTaintWrapper(taintWrapper);
            analyzer.runInfoflow();

        } catch (AbortAnalysisException e) {
            // Silently return
        } catch (Exception e) {
            System.err.printf("The data flow analysis has failed. Error message: %s%n", e.getMessage());
            e.printStackTrace();
        }
    }

    private ITaintPropagationWrapper initializeTaintWrapper() {
        MethodSummaries methodSummaries = new MethodSummaries();
        GapManager gapManager = new GapManager();
        return new SummaryGenerationTaintWrapper(methodSummaries, gapManager);
    }

    protected SetupJarApplication createFlowDroidInstance(final InfoflowJarConfiguration config) {
        return new SetupJarApplication(config);
    }


    private void setInfoFlowConfiguration(CommandLine cmd, InfoflowJarConfiguration config) {
//        config.getAccessPathConfiguration().setAccessPathLength(100);
        config.setJarDir(cmd.getOptionValue(OPTION_JAR_DIR));
        config.setOutPath(cmd.getOptionValue(OPTION_OUTPUT_DIR));
        config.setSourceSinkFile(cmd.getOptionValue(OPTION_SOURCE_SINK_FILE));
        config.setEntryClasses(cmd.getOptionValue(OPTION_ENTRY_CLASS));
    }
}
