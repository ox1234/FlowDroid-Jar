package util;

import com.google.common.collect.Iterators;
import soot.MethodOrMethodContext;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.*;

public class CallGraphUtil {
    static Map<MethodOrMethodContext, Set<MethodOrMethodContext>> performanceErrorMap = new HashMap<>();
    static Map<MethodOrMethodContext, Set<MethodOrMethodContext>> resultErrorMap = new HashMap<>();

    public static void checkTooManyEdges(CallGraph callGraph){
        for(Edge edge : callGraph){
            MethodOrMethodContext src = edge.getSrc();
            Iterator<Edge> outEdges = callGraph.edgesOutOf(src);
            while(outEdges.hasNext()){
                Edge outEdge = outEdges.next();
                if(performanceErrorMap.containsKey(src)){
                    performanceErrorMap.get(src).add(outEdge.getTgt());
                }else{
                    performanceErrorMap.put(src, new HashSet<>(Collections.singleton(outEdge.getTgt())));
                }
            }
            for(Map.Entry<MethodOrMethodContext, Set<MethodOrMethodContext>> entry : performanceErrorMap.entrySet()){
                if(entry.getValue().size() > 100){
                    MethodOrMethodContext key = entry.getKey();
                    Set<MethodOrMethodContext> value = entry.getValue();
                    resultErrorMap.put(key, value);
                }
            }
        }
        System.out.printf("There is %d potential performanceError\n", performanceErrorMap.size());
    }
}
