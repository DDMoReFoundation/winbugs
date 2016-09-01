/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.converters.unipv.winbugs;

import crx.converter.engine.*;
import static crx.converter.engine.PharmMLTypeChecker.*;
import crx.converter.tree.*;
import crx.converter.spi.blocks.*;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;
import eu.ddmore.libpharmml.dom.maths.Binop;
import eu.ddmore.libpharmml.dom.maths.Condition;
import eu.ddmore.libpharmml.dom.maths.ExpressionValue;
import eu.ddmore.libpharmml.dom.maths.FunctionCallType;
import eu.ddmore.libpharmml.dom.maths.Piece;
import eu.ddmore.libpharmml.dom.maths.Piecewise;
import eu.ddmore.libpharmml.dom.maths.Uniop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBElement;

/**
 *
 * @author cristiana
 */
public abstract class PascalParser extends Parser {

    protected static final String templateFile = "";
    protected static final String piecewiseTemplateFileName = "TemplatePiecewise.txt";
    protected static final String piecewiseFileName = "FunctionPiecewise";
    protected static final String covariateFileName = "FunctionCovariate";

    protected String outputDirPascal1;
    protected String outputDirPascal2;
    protected String outputDirPascal2Mod;
    protected String outputDirPiecewisePascal;
    protected String piecewisePascalName;

    public void setPiecewisePascalName(String piecewisePascalName) {
        this.piecewisePascalName = piecewisePascalName;
    }

    public String getPiecewisePascalName() {
        return piecewisePascalName;
    }

    public void setOutputDirPiecewisePascal(String outputDirPiecewisePascal) {
        this.outputDirPiecewisePascal = outputDirPiecewisePascal;
    }
    protected String odeCallName;

    public String getOutputDirPiecewisePascal() {
        return outputDirPiecewisePascal;
    }

    protected String icLabel = "IC";

    protected static String outputPascalFile = "";
    protected List<String> odeParamLines = new ArrayList<>();
    protected String parName = "theta";
    protected String parContCovName = "p_cont";
    protected String parCatCovName = "p_cat";
    protected int indPAS0 = 0;
    protected int indWB0 = 1; // starting index for Pascal
    protected File pascalFile;
    protected File pascalModFile;
    protected File pascalPMetricsFileName;

    protected static final String PMetricsFileName = "PKModels.txt";
    protected static final String pmetricsTemplate = "PKModels.txt";
//    protected String pascalBody;
    protected String pascalMod;
    protected String[] pascalPiecewise;
    protected List<String> pascalCatCov = new ArrayList<>();
    protected String pascalPKmodel;
    protected String min_format = "MIN(%s, %s)", max_format = "MAX(%s, %s)", rem_format = "REM(%s, %s)";
    protected String logx_format = "(Math.Ln(%s)/Math.Ln(%s))", root_format = "MathFunc.Power(%s,1.0/(%s))";

    public PascalParser() throws IOException {
        super();
    }

    abstract protected void pascalEquation(Object context, Node leaf);

    abstract protected void pascalPiecewiseEquation(Object context, Node leaf);

    protected void manageStateVariables(StructuralBlock sb, int nSb) throws FileNotFoundException, IOException {
        // to put in derivativeMap 
        List<DerivativeVariable> rvs = sb.getStateVariables();
        // puts in the derivativeMap the orignal symbId and the indexed parameter
        for (DerivativeVariable rv : rvs) {
            doDerivativeRef(rv.getSymbId());
        }

        if (derivativeSymbols.size() == 0) {
            return;
        }
        super.manageStateVariables(sb, nSb);
    }

    abstract protected List<String> pascalCodeFileGeneration() throws FileNotFoundException, IOException;

    abstract protected String pascalParametersDeclaration();
//    abstract protected String stateVarDecl(String format);
//    abstract protected String stateICDecl(String format);

    protected String stateVarDecl(String format) {
        StringBuilder sbVar = new StringBuilder();
        String sep = "";
        for (int i = 0; i < completeStateVariablesList.size(); i++) {
            sbVar.append(sep + varLabel + i);
            sep = ", ";
        }
        String line1 = String.format(format, sbVar.toString());
        return line1;
    }

    // generazione FunctionCOvariate
    protected String pascalCatCovCodeFileGeneration(int num, String catVal, String id) throws FileNotFoundException, IOException {
        String format = getPascalTemplate(catCovTemplateFileName);
        String file;
        file = String.format(format,
                num,
                id,
                id,
                id,
                catVal,
                num);
        return file;
    }

    protected String pascalCatCovCodeBlockGeneration(String idInterp, String catVal, String idPiece) throws FileNotFoundException, IOException {
        String format = "IF %s = %s THEN;\n\t\t\t%s := 1;\n\t\tELSE;\n\t\t\t%s := 0;\n\t\tEND;";
        String file;
        file = String.format(format,
                delimit(idInterp),
                catVal,
                delimit(idPiece),
                delimit(idPiece));
        return file;
    }

    protected String stateICDecl(String format) {
        String sep = "";
        StringBuilder sbIc = new StringBuilder();
//        String line1 = String.format(format, sb.toString());
        for (int i = 0; i < completeStateVariablesList.size(); i++) {
            sbIc.append(sep + icLabel + i);
            sep = ", ";
        }

        String line2 = String.format(format, sbIc.toString());
        return line2;
    }

    private String generatePiecewiseVarDeclaration(String pwId) { // 8 aprile 2016
        List<String> lines = new ArrayList<>();
        String format = "%s: REAL;\n\t\t";
        StringBuilder sb = new StringBuilder("");
        int num = pascalVarMap.values().size();
        for (Map.Entry<String, Integer> pw : piecewiseIndexMap.entrySet()) {
            if (!pascalVarMap.containsKey(pwId)) {
                pascalVarMap.put(pw.getKey(), num++);// 9/6/2016
            }
        }
        vars = new ArrayList<>();
        vars = parVariablesFromMap.get(pwId);
        for (SymbolRef sr : vars) {
            if (!parVariablesFromMap.containsKey(sr.getSymbIdRef())) {
                sb.append(String.format(format, sr.getSymbIdRef()));
            } else {
                sb.append(String.format(format, sr.getSymbIdRef() + upperSuffixDerDepLabel));
            }
        }
        List<SymbolRef> a = getPiecewiseParameters_Pascal(vars);
        for (SymbolRef s : a) {
            if (!vars.contains(s)) {
                if (!parVariablesFromMap.containsKey(s.getSymbIdRef())) {
                    sb.append(String.format(format, s.getSymbIdRef()));
                } else {
                    sb.append(String.format(format, s.getSymbIdRef() + upperSuffixDerDepLabel));
                }
            }
        }
        return Util.clean(sb.toString());
    }

    private String adjustPascalName(String s1) {
//        if (lexer.isIndividualParameter(s1) // VERIFICARE
//                || !isInList(getList(leafPiecewiseParameters), s1)) {
        if (parVariablesFromMap.containsKey(s1)) {
            s1 = s1.replaceAll(s1, delimit(s1 + upperSuffixDerDepLabel));
            s1 = Util.clean(s1);
        }
        return s1;
    }

    // generazione FunctionPiecewise
    protected String pascalPiecewiseCodeFileGeneration(int num, String id) throws FileNotFoundException, IOException {
        String format = getPascalTemplate(piecewiseTemplateFileName);
        pascalPiecewise[num - 1] = String.format(format,
                num,
                generatePiecewiseVarDeclaration(id),
                Util.clean(generatePiecewiseBlocks(id)),
                num);

        return pascalPiecewise[num - 1];
    }

    String getModuleName(File f) {
        String out = "";
        String path = f.getPath();
        String name = f.getName();
        if (path.contains("Pmetrics")) {
            out = "Pmetrics";
        } else {
            out = "WBDev";
        }
        out += name;
        return out.substring(0, out.indexOf(".txt"));
    }

    String getRootModuleName(String f) {
        String out = "";
        String path = f.substring(0, f.lastIndexOf("/"));
        String name = f.substring(f.lastIndexOf("/") + 1);
        if (path.contains("Pmetrics")) {
            out = "Pmetrics";
        } else {
            out = "WBDev";
        }
        out += name;
        return out;
    }

    @Override
    public void writeModelFunction(PrintWriter fout, StructuralBlock sb) throws IOException { 
        super.writeModelFunction(fout, sb);
        if (pascalBody != null) {
            if (this instanceof PascalParser2) {
                output(pascalBody, pascalFile, true);
                output(pascalMod, pascalModFile, true);
                String pmetrics = getPascalTemplate(pmetricsTemplate);
                int ngrid = Integer.parseInt(((PascalParser2) this).getTotGrid()); 
                int nState = this.derivativeSymbols.size();
                int npar = (int) Math.min(Math.round(2e5 / (ngrid)), 1000); 
                String pkBody = String.format(pmetrics, ngrid, npar, nState); 
                output(pkBody, pascalPMetricsFileName, true);
                PrintWriter out = new PrintWriter(new File(getWinbugsDir() + "/" + "toConv.txt"));
                if (hasDiffEquations) {
                    out.println("Pmetrics " + Util.getFileName(pascalFile));
                    out.println("Pmetrics " + Util.getFileName(pascalPMetricsFileName));

                    out.println("WBDev " + Util.getFileName(pascalModFile));
                    pmetricsList.add(getModuleName(pascalPMetricsFileName));
                    pmetricsList.add(getModuleName(pascalFile));
                    wbdevList.add(getModuleName(pascalModFile));
                }
                for (String nm : pwList) {
                    out.println("WBDev " + nm.substring(nm.indexOf("WBDev")));
                }
                for (Map.Entry<Integer, String> nm : covFilesMap.entrySet()) {
                    out.println("WBDev " + covariateFileName + nm.getKey());
                }

                out.close();
                for (String nm : pwList) {
                    wbdevList.add(getRootModuleName(getPiecewisePascalName()) + nm);
                }
                for (Map.Entry<Integer, String> nm : covFilesMap.entrySet()) {
                    wbdevList.add(getRootModuleName(getCovariatePascalName()) + nm.getKey());
                }

            }
        }
        piecewiseCodeFileGeneration();
        createCompileScriptWinbugs();

    }

    protected void piecewiseCodeFileGeneration() throws FileNotFoundException, IOException {
        if (!piecewiseMap.isEmpty()) {
            pascalPiecewise = new String[piecewiseMap.size()];
            if (this instanceof PascalParser2) {

                for (Map.Entry<String, String> pw : piecewiseMap.entrySet()) {
                    if(pwBugsList.contains(Util.clean(removeIndexes(pw.getKey())))){
                        int index = piecewiseIndexMap.get(Util.clean(removeIndexes(pw.getKey())));
                        String piecewiseFile = piecewiseFileName + index + pascalFileNameSuffix;
                        inPW = true;
                        output(pascalPiecewiseCodeFileGeneration(index, Util.clean(removeIndexes(pw.getKey()))),
                                new File(getPiecewisePascalName() + index + pascalFileNameSuffix), true);
                        pwList.add(getModuleName(new File(piecewiseFile)));
                        piecewiseFileList.add(piecewiseFile.substring(0, piecewiseFile.indexOf(".")));
                        inPW = false;
                    }
                }
                PrintWriter out = new PrintWriter(new FileWriter(getWinbugsDir() + "/" + "toConv.txt", true));
                out.close();
                out = new PrintWriter(new FileWriter(getWinbugsDir() + "/" + "toConv.txt", true));
                for (String nm : pwList) {
                    int p = nm.indexOf("Func");
                    out.println("WBDev " + nm.substring(p));
                }
                out.close();
            }
        }
    }

    public void setPascalFileName(File fileName) {
        this.pascalFile = fileName;
    }

    public void setPascalModFileName(File fileName) {
        this.pascalModFile = fileName;
    }

    public void setPascalPKmodel(String pascalPKmodel) {
        this.pascalPKmodel = pascalPKmodel;
    }

    public File getPascalPMetricsFileName() {
        return pascalPMetricsFileName;
    }

    protected void append(String body, File f) throws FileNotFoundException, IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
        out.println(body);

        out.close();

    }

    public void setOutputDirPascal1(String outputDirPascal1) {
        this.outputDirPascal1 = outputDirPascal1;
    }

    String getPascalTemplate(String fName) throws FileNotFoundException, IOException {
        outDebug.println("<-- template " + fName);
        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(new FileReader(new File(winbugsTemplateDir + fName)));
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    protected String doDerivativeRef(String id) {
        String symbol = "";
        if (id != null) {
            Integer idx = getStateVarIndex(id);
            symbol = upperStateLabel + leftArrayBracket + IND_S + "," + idx + rightArrayBracket;
            derivativeMap.put(id, symbol);
            derivativeMapNew.put(id, varLabel + idx);
        }
        return symbol;
    }

    protected boolean isInOdeParameters1(VariableDefinition v) {
        for (SymbolRef sr : odeParameters1) {
            if (sr.getSymbIdRef().equals(v.getSymbId())) {
                return true;
            }
        }
        return false;
    }

    protected boolean isInOdeParameters1(SymbolRef s) {
        for (SymbolRef sr : odeParameters1) {
            if (sr.getSymbIdRef().equals(s.getSymbIdRef())) {
                return true;
            }
        }
        return false;
    }

    protected int getStateVarIndex(String id) {
        int i = 0;
        for (DerivativeVariable var : completeStateVariablesList) {
            if (var.getSymbId().equals(id)) {
                break;
            }
            i++;
        }
        return i;
    }

    @Override
    protected String doUnaryOperation(Uniop u_op, String leftStatement) {

        String pascalUnaryOperator;
        try {
            pascalUnaryOperator = doPascalTransformation(u_op.getOperator().getOperator(), leftStatement);
        } catch (UnsupportedOperationException e) {
            pascalUnaryOperator = super.doUnaryOperation(u_op, leftStatement);
        }
        pascalUnaryOperator = "(" + pascalUnaryOperator + ")";
        String winbugsUnaryOperator = super.doUnaryOperation(u_op, leftStatement);
        if (!delimit(removeIndexes(winbugsUnaryOperator)).equals(delimit(removeIndexes(pascalUnaryOperator)))) {
            String in1 = removeIndexes(winbugsUnaryOperator);
            String in2 = adjustSuffix(pascalUnaryOperator);
            uniOpList.add(in1);
            unaryOpMap.put(in1, in2);
            updateUniopMap();
        }
        return winbugsUnaryOperator;
    }

    private String adjustSuffix(String in) { 
        String newId;
        in = removeIndexes(in);
        for (SymbolRef s : stateV_Parameters) {
            if (!isIn(s, theta_Parameters) && !isCovariate(s)
                    && in.contains(delimit(s.getSymbIdRef()))) {
                if (parVariablesFromMap.containsKey(s.getSymbIdRef())) {
                    if (!isDerivativeVar(s.getSymbIdRef())) {
                        newId = delimit(s.getSymbIdRef() + upperSuffixDerDepLabel);
                        in = in.replaceAll(delimit(s.getSymbIdRef()), delimit(newId));
                    }
                }
            }
        }
        return in;
    }

    @Override
    protected String doBinaryOperation(Binop b_op, String leftStatement, String rightStatement) {
        String pascalBinaryOperator;
        try {
            pascalBinaryOperator = doPascalTransformation(b_op.getOperator().toString(), leftStatement, rightStatement);
        } catch (UnsupportedOperationException e) {
            pascalBinaryOperator = super.doBinaryOperation(b_op, leftStatement, rightStatement);
        }
        String winbugsBinaryOperator = super.doBinaryOperation(b_op, leftStatement, rightStatement);
        if (!delimit(removeIndexes(winbugsBinaryOperator)).equals(delimit(removeIndexes(pascalBinaryOperator)))) {
            binOpMap.put(delimit(removeIndexes(winbugsBinaryOperator)), delimit(removeIndexes(pascalBinaryOperator)));
        }
        return winbugsBinaryOperator;
    }

    protected boolean checkIndiv() {
        boolean isIndividual = false;
        String symbol;
        for (SymbolRef s : theta_Parameters) {
            symbol = getSymbol(s);
            if (symbol.contains(IND_S)) {
                isIndividual = true;
            }
        }

        return isIndividual;
    }

    public String doPiecewise(Piecewise pw) {
        String symbol = unassigned_symbol;

        List<Piece> pieces = pw.getPiece();
        Piece else_block = null;
        BinaryTree[] assignment_trees = new BinaryTree[pieces.size()];
        BinaryTree[] conditional_trees = new BinaryTree[pieces.size()];
        String[] conditional_stmts = new String[pieces.size()];
        String[] assignment_stmts = new String[pieces.size()];

        TreeMaker tm = lexer.getTreeMaker();
        Accessor a = lexer.getAccessor();

        int assignment_count = 0, else_index = -1;
        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);
            if (piece != null) {
                // Logical blocks
                Condition cond = piece.getCondition();
                if (cond != null) {
                    conditional_trees[i] = tm.newInstance(piece.getCondition());
                    conditional_trees[i] = doPutCategory(conditional_trees[i]);
                    if (cond.getOtherwise() != null) {
                        else_block = piece;
                        else_index = i;
                    }
                }

                // Assignment block
                BinaryTree assignment_tree = null;
                ExpressionValue expr = piece.getValue();
                if (isBinaryOperation(expr)) {
                    assignment_tree = tm.newInstance(rhs((Binop) expr, a));
                    assignment_count++;
                } else if (isConstant(expr)) {
                    assignment_tree = tm.newInstance(expr);
                    assignment_count++;
                } else if (isFunctionCall(expr)) {
                    assignment_tree = tm.newInstance(rhs((FunctionCallType) expr, a));
                    assignment_count++;
                } else if (isJAXBElement(expr)) {
                    assignment_tree = tm.newInstance(rhs((JAXBElement<?>) expr, a));
                    assignment_count++;
                } else if (isSymbolReference(expr)) {
                    assignment_tree = tm.newInstance(expr);
                    assignment_count++;
                } else if (isScalarInterface(expr)) {
                    assignment_tree = tm.newInstance(expr);
                    assignment_count++;
                } else {
                    throw new IllegalStateException("Piecewise assignment failed (expr='" + expr + "')");
                }

                if (assignment_tree != null) {
                    assignment_trees[i] = assignment_tree;
                }
            }
        }

        if (assignment_count == 0) {
            throw new IllegalStateException("A piecewise block has no assignment statements.");
        }

        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);

            if (conditional_trees[i] != null && assignment_trees[i] != null) {
                // Logical condition
                if (!piece.equals(else_block)) {
                    conditional_stmts[i] = "\t\t" + parse(piece, conditional_trees[i]);
                }

                // Assignment block
                assignment_stmts[i] = "\t\t" + parse(new Object(), assignment_trees[i]);
            }
        }

        int block_assignment = 0;
        StringBuilder block = new StringBuilder(" NaN;\n");
        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);
            if (piece == null) {
                continue;
            } else if (piece.equals(else_block)) {
                continue;
            }

            if (!(conditional_stmts[i] != null && assignment_stmts[i] != null)) {
                continue;
            }
            String operator = "if", format = "%s (%s) {\n %s <- %s \n";
            if (block_assignment > 0) {
                operator = "\t\t}\\t\\t else if ";
                format = "\t\t %s (%s) {\n %s <- %s \n";
            }

            block.append(String.format(format, operator, conditional_stmts[i], field_tag, assignment_stmts[i]));
            block_assignment++;
        }

        if (else_block != null && else_index >= 0) {
            block.append("} else {\n");
            String format = " %s <- %s\n";
            block.append(String.format(format, field_tag, assignment_stmts[else_index]));
        }
        block.append("}");
        if (assignment_count == 0) {
            throw new IllegalStateException("Piecewise statement assigned no conditional blocks.");
        }
        symbol = block.toString();
        return "";
    }

}
