/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//package scratchpad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.debug.DebugFrame;
import net.sourceforge.htmlunit.corejs.javascript.debug.DebuggableScript;
import net.sourceforge.htmlunit.corejs.javascript.debug.Debugger;

/**
 *
 * @author Adam
 */
public class HtmlUnitCommandLineDebugger implements Debugger{

    private enum Scope {ACTIVATION, THISOBJ};

    private List<BreakPoint> breakpoints = new ArrayList<BreakPoint>();

    final private HashMap<String, List<String>> sources = new HashMap<String,List<String>>();

    private Scope currentScope = Scope.THISOBJ;

    public HtmlUnitCommandLineDebugger(){        
    }

    public void addBreakPoint(String sourceName, int lineNumber){
        breakpoints.add(new BreakPoint(sourceName, lineNumber));
    }

    private List<String> sourceList(String source) throws IOException{        
        List<String> returnVal = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new StringReader(source));
        String tmp;
        while((tmp = reader.readLine()) != null){
            returnVal.add(tmp);
        }
        return returnVal;
    }

    public void handleCompilationDone(Context context, DebuggableScript script, String source) {        
        try{
            synchronized(sources){
                if(!sources.containsKey(script.getSourceName())) {                    
                    sources.put(script.getSourceName(), sourceList(source));
                }
            }
        }catch(Throwable t){
            System.out.println("Could not read source!!!");
            t.printStackTrace();
        }
    }

    public DebugFrame getFrame(Context context, DebuggableScript script) {
        return new InternalDebugFrame(script, lookupBreakpoints(script), sources.get(script.getSourceName()));
    }

    private List<BreakPoint> lookupBreakpoints(DebuggableScript script){
        List<BreakPoint> returnValue = new ArrayList<BreakPoint>();
        for(BreakPoint p : breakpoints){
            if(p.sourceName.equals(script.getSourceName())) returnValue.add(p);
        }
        return returnValue;
    }

    public final class BreakPoint {
        private String sourceName;
        private int lineNumber;

        public BreakPoint(String sourceName, int lineNumber){
            this.sourceName = sourceName;
            this.lineNumber = lineNumber;
        }
    }

    public final class InternalDebugFrame implements DebugFrame{       

        private List<BreakPoint> breakPoints;

        private List<String> source;

        private DebuggableScript script;

        Stack<Scriptable> activationStack = new Stack<Scriptable>();

        Stack<Scriptable> thisObjStack = new Stack<Scriptable>();       

        int lastLine;

        public InternalDebugFrame(DebuggableScript script, List<BreakPoint> breakPoints, List<String> source){
            this.script = script;
            this.breakPoints = breakPoints;
            this.source = source;
        }

        private boolean isBreakPoint(int lineNumber){
            for(BreakPoint p : breakPoints){
                if(p.lineNumber == lineNumber) return true;
            }
            return false;
        }

        public void onEnter(Context ctx, Scriptable activation, Scriptable thisObj, Object[] params) {
            activationStack.push(activation);
            thisObjStack.push(thisObj);
        }

        public void onLineChange(Context context, int lineNumber) {
            lastLine = lineNumber;            
            if(isBreakPoint(lineNumber)){
                try{
                    executeBreakpoint(context);
                }catch(Throwable t){
                    System.out.println("Error debugging script: " + t.getMessage());
                }
            }
        }

        private void executeBreakpoint(Context ctx) throws IOException{
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            int startDisplayLine = 0;
            int endDisplayLine = source.size();
            if(lastLine > 5) startDisplayLine = lastLine - 5;
            if(lastLine < (source.size()-5)) endDisplayLine = lastLine + 5;
            String breakpointLine = source.get(lastLine-1);
            List<String> capture = source.subList(startDisplayLine, endDisplayLine);
            System.out.println("Breakpoint Reached:");
            for(String s : capture){
                if(s.equals(breakpointLine)) System.out.print(">>");
                System.out.println(s);
            }
            System.out.println();
            System.out.println("Select Action:");
            System.out.println("1) Continue");
            System.out.println("2) Execute Javascript");
            System.out.println("3) Switch to Activation Scope");
            System.out.println("4) Switch to 'this' Scope");
            System.out.println("5) Exit");
            int selection = Integer.parseInt(in.readLine());
            switch(selection){
                case 1:                    
                    return;
                case 2:
                    System.out.println("Enter Statements Below (Current Scope: " + (HtmlUnitCommandLineDebugger.this.currentScope == Scope.ACTIVATION ? "Activation" : "this") + "):");
                    System.out.print("js>");
                    StringBuilder exec = new StringBuilder();
                    String stmt;
                    stmt = in.readLine();
                    while(stmt != null && stmt.length() > 0){
                        exec.append(stmt);
                        System.out.print("js>");
                        stmt = in.readLine();
                    }
                    Scriptable scope;
                    if(HtmlUnitCommandLineDebugger.this.currentScope == Scope.ACTIVATION){
                        scope = activationStack.peek();
                    }else{
                        scope = thisObjStack.peek();
                    }
                    Scriptable jsArgs = Context.toObject(System.out, scope);
                    scope.put("out", scope, jsArgs);
                    try{
                        ctx.evaluateString(scope, exec.toString(), "Debug" + System.currentTimeMillis(), 1, null);
                    }catch(Throwable t){
                        System.out.println("Error executing your javascript: " + t.getMessage());
                    }                    
                    break;
                case 3:
                    HtmlUnitCommandLineDebugger.this.currentScope = Scope.ACTIVATION;
                    break;
                case 4:
                    HtmlUnitCommandLineDebugger.this.currentScope = Scope.THISOBJ;
                    break;
                case 5:
                    System.exit(1);
                    return;
                default:
                    return;
            }
            executeBreakpoint(ctx);
        }

        public void onExceptionThrown(Context ctx, Throwable t) {
            System.out.println("Exception thrown at line " + lastLine + ": " + t.getMessage());
        }

        public void onExit(Context ctx, boolean wasError, Object returnVal) {
            activationStack.pop();
            thisObjStack.pop();
        }

        public void onDebuggerStatement(Context arg0) {
            
        }
        
    }

}
