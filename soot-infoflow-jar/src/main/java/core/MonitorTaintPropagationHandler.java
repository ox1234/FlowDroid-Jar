package core;

import soot.Unit;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;

import java.util.Set;

public class MonitorTaintPropagationHandler implements TaintPropagationHandler {

    @Override
    public void notifyFlowIn(Unit stmt, Abstraction taint, InfoflowManager manager, FlowFunctionType type) {
//        System.out.println("-------------------------------------------Propagate-----------------------------------------------");
//        System.out.printf("Current Taint Stmt: %s\n", stmt.toString());
//        System.out.printf("Propagate Taint Variable: %s\n", taint.toString());
//        System.out.println("-------------------------------------------Propagate-----------------------------------------------");
    }

    @Override
    public Set<Abstraction> notifyFlowOut(Unit stmt, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing, InfoflowManager manager, FlowFunctionType type) {
        System.out.println("-------------------------------------------Generate-----------------------------------------------");
        System.out.printf("Current Taint Stmt: %s\n", stmt.toString());
        System.out.printf("Current Taint flow function: %s\n", type);
        System.out.printf("Incoming Taint Variable: %s\n", incoming.toString());
        System.out.println("Outgoing Taint Variable is:");
        for(Abstraction abstraction : outgoing){
            System.out.println("\t" + abstraction);
        }
        System.out.println("-------------------------------------------Generate-----------------------------------------------");
        return outgoing;
    }
}
