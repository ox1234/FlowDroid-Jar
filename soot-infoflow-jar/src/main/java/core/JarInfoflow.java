package core;

import soot.*;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import java.util.*;

public class JarInfoflow extends Infoflow {
    private final Set<Edge> virtualInvokeEdges = new HashSet<>();
    private final Set<MethodOrMethodContext> sinkCalledMethods = new HashSet<>();
    private final Set<MethodOrMethodContext> visitedNode = new HashSet<>();
    private final Set<Edge> notUsefulInvokeEdges = new HashSet<>();
    public JarInfoflow(InfoflowJarConfiguration config){
        super.config = config;
    }

    @Override
    public void computeInfoflow(String appPath, String libPath, IEntryPointCreator entryPointCreator, ISourceSinkManager sourcesSinks) {
        config.setSootIntegrationMode(InfoflowConfiguration.SootIntegrationMode.UseExistingInstance);
        super.computeInfoflow(appPath, libPath, entryPointCreator, sourcesSinks);
    }

    @Override
    protected void constructCallgraph() {
        // construct Call Graph, because it needs to be change
        PackManager.v().getPack("cg").apply();
        CallGraph callGraph = Scene.v().getCallGraph();
        changeCallGraph(callGraph);
        config.setSootIntegrationMode(InfoflowConfiguration.SootIntegrationMode.UseExistingCallgraph);
        super.constructCallgraph();
    }

    private void changeCallGraph(CallGraph callGraph){
        SootMethod sinkMethod = getSinkMethod();
        findVirtualInvoke(callGraph, sinkMethod);
        removeInterfaceEdges(callGraph);
        removeNotUsefulCall(callGraph);
    }

    private void findVirtualInvoke(CallGraph callGraph, MethodOrMethodContext methodOrMethodContext){
        sinkCalledMethods.add(methodOrMethodContext);
        Set<Edge> linkedSet = new LinkedHashSet<>();
        Iterator<Edge> intoEdges = callGraph.edgesInto(methodOrMethodContext);
        while(intoEdges.hasNext()){
            Edge edge = intoEdges.next();
            linkedSet.add(edge);
        }
        for(Edge edge : linkedSet){
            if(!edge.isSpecial() && edge.isInstance()){
                virtualInvokeEdges.add(edge);
            }
            if(visitedNode.contains(edge.getSrc())){
                continue;
            }else{
                visitedNode.add(edge.getSrc());
            }
            findVirtualInvoke(callGraph, edge.getSrc());
        }
    }

    private void removeInterfaceEdges(CallGraph callGraph){
        for(Edge virtualEdge : virtualInvokeEdges){
            MethodOrMethodContext src = virtualEdge.getSrc();
            Iterator<Edge> outEdges = callGraph.edgesOutOf(src);
            while(outEdges.hasNext()){
                Edge edge = outEdges.next();
                MethodOrMethodContext tgt = edge.getTgt();
                if(!sinkCalledMethods.contains(tgt)){
                    callGraph.removeEdge(edge, true);
                }
            }
        }
    }

    private void removeNotUsefulCall(CallGraph callGraph){
        for (Edge edge : callGraph) {
            if (edge.isClinit() || edge.isSpecial()) {
                notUsefulInvokeEdges.add(edge);
            }
        }

        for(Edge edge : notUsefulInvokeEdges){
            callGraph.removeEdge(edge, true);
        }
    }

    public SootMethod getSinkMethod(){
        SootClass startClass = Scene.v().loadClassAndSupport("javax.naming.InitialContext");
        for (SootMethod sootMethod : startClass.getMethods()) {
            if (sootMethod.getName().equals("lookup")) {
                RefType firstType = (RefType) sootMethod.getParameterType(0);
                if (firstType.getClassName().equals("java.lang.String")) {
                    return sootMethod;
                }
            }
        }
        return null;
    }
}
