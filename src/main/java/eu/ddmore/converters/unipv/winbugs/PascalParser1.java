/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.converters.unipv.winbugs;

import crx.converter.tree.*;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author cristiana
 */
class PascalParser1 extends PascalParser {

    protected static final String templateFile = "PascalTemplate.txt";
    protected static final String templateCovFile = "PascalTemplateCov.txt";
    protected String pascalDiffEq = new String();
    private final String ODECALLNAME = "function.model.wbdev";

    public PascalParser1() throws IOException {
        super();
        this.odeCallName = ODECALLNAME;

    }

    // assigns to theta[i] the equations parameters (pascal)
    // example: k21 := theta [0];
    protected String pascalThetaAssignement() {
        StringBuilder sb = new StringBuilder();
        int ind = indPAS0;
        String format = "%s %s %s%s%s%s;\n\t\t";
        String formatCov = "%s %s %s%slastindex+%s%s;\n\t\t";
        for (SymbolRef s : theta_Parameters) {
            if (!isIndependentVariableSym(s.getSymbIdRef())) {
                if (hasCovariate) {
                    sb.append(String.format(formatCov, s.getSymbIdRef(), pascalAssignSymbol, parName, leftArrayBracket, ind++, rightArrayBracket));
                } else {
                    sb.append(String.format(format, s.getSymbIdRef(), pascalAssignSymbol, parName, leftArrayBracket, ind++, rightArrayBracket));
                }
            }
        }
        return sb.toString();
    }

    // assigns to covariates elements of array p (pascal)
    // example: W:=p[0]; D:=p[1];
    protected String pascalCovAssignEqLines() {
        StringBuilder sb = new StringBuilder();
        StringBuilder sb1 = new StringBuilder();
        int ind = indPAS0;
        String format = "%s %s %s%s%s%s;\n\t\t";

        List<String> names = new ArrayList<>();
        if (usedOdeContCovNames.size() > 0) {
            names = Util.getNames(usedOdeContCovNames);

            for (String id : names) {
                sb.append(String.format(format, id, pascalAssignSymbol, parContCovName, leftArrayBracket, ind++, rightArrayBracket));
            }
        }

        ind = indPAS0;
        if (usedOdeCatCovNames.size() > 0) {
            names = Util.getNames(usedOdeCatCovNames);

            for (String id : names) {
                sb.append(String.format(format, id, pascalAssignSymbol, parCatCovName, leftArrayBracket, ind++, rightArrayBracket));
                if (covCatMap.get(id) != null) {
                    sb1.append(covBlockMap.get(id));
                }
            }
        }

        return sb.toString() + sb1.toString();
    }

    protected String getCovBlocks() {
        StringBuilder sb = new StringBuilder();

        for (String id : Util.getList(usedOdeCatCovNames)) {
            if (covCatMap.get(id) != null) {
                sb.append(Util.clean(covBlockMap.get(id)));
            }
        }

        return sb.toString();
    }

    // assigns to theta[i] the equations parameters (winbugs)
    // example: theta[0] <- k21
    protected List<String> winbugsOdeParAssignement() {
        List<String> lines = new ArrayList<>();
        int ind = indWB0;
        String format;
        int indice;
        String symbol;
        boolean isIndividual = false;
        isIndividual = checkIndiv();
        for (SymbolRef s : theta_Parameters) {
            symbol = getSymbol(s);
            indice = ind++;
            if (!isIndividual || isDosingTime(symbol)) {
                format = "%s%s%s%s %s %s";
                lines.add(String.format(format, parName, leftArrayBracket, indice, rightArrayBracket, assignSymbol, symbol));
            } else {
                format = "%s%s%s,%s%s %s %s%s";
                lines.add(String.format(format, parName, leftArrayBracket, IND_S, indice, rightArrayBracket, assignSymbol, symbol, ""));
            }
        }
        return lines;
    }

    // assigns to theta[i] the equations parameters (winbugs)
    // example: theta[0] <- k21
    protected List<String> winbugsOdeCovParAssignement() {
        List<String> lines = new ArrayList<>();
        int ind = indWB0;
        String thetaFormat;
        int indice;

        List<String> names = new ArrayList<>();
        names = Util.getNames(usedOdeContCovNames);

        int startIndex = 1 + 2 * getNoTransfCovContNumber();
        lines.add(String.format("%s[%s,1] %s n_cov_cont\n", parName, IND_S, assignSymbol));
        ind++;
        thetaFormat = "%s[%s,%s] %s %s%s\n";
        for (String id : names) {
            lines.add(String.format(thetaFormat, parName, IND_S, ind++, assignSymbol, maxNamePrefix, id));
        }
        thetaFormat = "%s[%s,%s] %s %s%s[%s]\n";
        for (String id : names) {
            lines.add(String.format(thetaFormat, parName, IND_S, ind++, assignSymbol, NtNamePrefix, id, IND_S));
        }
        thetaFormat = "\t%s%s%s,%s%s %s %s%s%s%s,i%s\n";
        String maxN;
        StringBuilder sb = new StringBuilder();
        StringBuilder sb1 = new StringBuilder();
        String s0, s1;
        StringBuilder block = new StringBuilder();
        s0 = "i+" + startIndex;
        for (String id : names) {
            maxN = maxNamePrefix + id;
            block.append(String.format("for (i in 1 : %s){ \n", maxN));
            block.append(String.format(thetaFormat, parName,
                    leftArrayBracket, IND_S, s0, rightArrayBracket,
                    assignSymbol, gridPrefix, id,
                    leftArrayBracket, IND_S, rightArrayBracket
            ));

            s1 = s0 + "+" + maxN;
            block.append("}\n");
            ind++;
            block.append(String.format("for (i in 1 : %s){ \n", maxN));
            block.append(String.format(thetaFormat, parName,
                    leftArrayBracket, IND_S, s1, rightArrayBracket,
                    assignSymbol, id, "",
                    leftArrayBracket, IND_S, rightArrayBracket));
            block.append("}\n\n");
            s0 = s0 + "+2*" + maxN;

            ind++;
        }
        s0 = s0.substring(2);
        lines.add(block.toString());
        boolean isIndividual = isodeParIndividual() || lexer.getCovariateBlocks().size() > 0;
        String thetaParFormat;
        indice = 1;
        for (SymbolRef s : theta_Parameters) {
            String symbol = getSymbol(s);
            if (!isIndividual) {
                thetaParFormat = "\t%s%s%s+%s%s %s %s\n";
                lines.add(String.format(thetaParFormat, parName,
                        leftArrayBracket, s0, ++indice, rightArrayBracket,
                        assignSymbol, symbol));

            } else {
                thetaParFormat = "\t%s%s%s,%s+%s%s %s %s\n";
                lines.add(String.format(thetaParFormat, parName,
                        leftArrayBracket, IND_S, s0, ++indice, rightArrayBracket,
                        assignSymbol, symbol));
            }
        }
        return Util.getUniqueString(lines);
    }

    // declares the parameters in the pascal model (pascal)
    // it includes both theta parameters and any variable the state depends on
    // example: k21: REAL;
    protected String pascalParametersDeclaration() {
        List<String> lines = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        String format = "%s: REAL;\n\t\t";
        String s;
        for (Map.Entry<String, List<SymbolRef>> es : parVariablesFromMap.entrySet()) {
            s = es.getKey();
            if (isInList(Util.getList(odeParameters1), s) && !isInList(Util.getList(theta_Parameters), s)) {
                if (isPiecewiseVar(s)) {
                    lines.add(String.format(format, piecewiseSuffix + "_" + s));
                } 
                else {
                    lines.add(String.format(format, s + upperSuffixDerDepLabel));
                }
            }
        }
        for (SymbolRef s1 : leafOdeParameters) {
            if (!isIn(s1, theta_Parameters)
                    && !isCovariate(s1)) {
                if (!isPiecewiseVar(s1.getSymbIdRef())) {
                    lines.add(String.format(format, doAppendSuffix(s1)));
                } else {
                    lines.add(String.format(format, piecewiseSuffix + "_" + s1.getSymbIdRef()));
                }
            } else {
                if (!isIndependentVariableSym(s1.getSymbIdRef()) || !hasDiffEquations) {
                    lines.add(String.format(format, s1.getSymbIdRef()));
                    if (covCatMap.get(s1.getSymbIdRef()) != null) {
                        lines.add(String.format(format, covCatMap.get(s1.getSymbIdRef())));
                    }
                }
            }
        }
        for (SymbolRef cv : usedOdeCatCovNames) {
            lines.add(String.format(format, piecePrefix + cv.getSymbIdRef()));
            lines.add(String.format(format, cv.getSymbIdRef()));
        }
        for (SymbolRef cv : usedOdeContCovNames) {
            lines.add(String.format(format, cv.getSymbIdRef()));
        }
        lines = Util.getUniqueString(lines);
        for (String s0 : lines) {
            sb.append(s0);
        }

        return sb.toString();
    }

// adds diff. eq. and variable assignements (pascal)
// example: dQ_UNIPVdt[1] := ((k12 * Q_UNIPV[0]) - varQ3_UNIPV);
// example: varQ2_UNIPV := (k21 * Q_UNIPV[1]);

    protected void pascalEquation(Object context, Node leaf) {
        String id = null;
        String stmt = pascalVariableEquation(context, leaf);
        if (context instanceof DerivativeVariable) {
            pascalDiffEqLines.add(pascalDiffEquation(context, leaf));
        } else if (context instanceof VariableDefinition) {
            id = ((VariableDefinition) context).getSymbId();
            if (toBeInPascal(context, id)) {
                pascalAssignVarEqLines.put(id, stmt);
            }
            if(toBeInFuncPW(context, id)){
                 pascalAssignVarEqLines.put(id, stmt);
            }
        } else if (context instanceof IndividualParameter
                || context instanceof PopulationParameter) {
            if (context instanceof IndividualParameter) {
                id = ((IndividualParameter) context).getSymbId();
            } else if (context instanceof PopulationParameter) {
                id = ((PopulationParameter) context).getSymbId();
            }
            if (toBeInPascal(context, id)) {
                pascalAssignVarEqLines.put(id, stmt);
            }
            if(toBeInFuncPW(context, id)){
                 pascalAssignVarEqLines.put(id, stmt);
            }
        } else if (context instanceof CovariateTransformation) {
            id = ((CovariateTransformation) context).getTransformedCovariate().getSymbId();
            {
                if (toBeInPascal(context, id)) {
                    pascalAssignVarEqLines.put(id, stmt);
                }
                if(toBeInFuncPW(context, id)){
                 pascalAssignVarEqLines.put(id, stmt);
            }
            }
        }
    }

    protected void pascalPiecewiseEquation(Object context, Node leaf
    ) {
        String id = null, stmt = null;
        stmt = pascalVariableEquation(context, leaf);
        if (context instanceof VariableDefinition) {
            VariableDefinition v = (VariableDefinition) context;
            if (v.getAssign() != null && v.getAssign().getPiecewise() != null) {
                return;
            }
            id = ((VariableDefinition) context).getSymbId();
            if (isInList(context, piecewiseParameters)
                    && parVariablesFromMap.containsKey(id)) {
                pascalAssignVarEqLines.put(id, stmt); 

            }
        } else if (context instanceof PopulationParameter) {
            id = ((PopulationParameter) context).getSymbId();
        } else if (context instanceof IndividualParameter) {
            id = ((IndividualParameter) context).getSymbId();
        }
        if (piecewiseCompleteList.size() > 0 && !isInList(piecewiseVariablesId, id) && stmt != null) {
            if ((context instanceof IndividualParameter
                    && !isPiecewiseVar(((IndividualParameter) context).getSymbId()))
                    || (context instanceof PopulationParameter
                    && !isPiecewiseVar(((PopulationParameter) context).getSymbId()))) { 
                if (isInList(context, leafOdeParameters)
                        && !isInList(context, theta_Parameters)
                        && !isPiecewiseVar(id)) {
                    pascalAssignIndivEqLines.put(id, stmt);
                }
            }
        }
    }

// generates the call statement to the pascal solver (winbugs)
// example: q_unipv[1:N_t, 1:2]<- one.comp.model(initial_value[1:2], grid[1:N_t], theta[1:2],0.0, 0.001)
    @Override
    protected List<String> winbugsPascalOdeCallGen() throws FileNotFoundException, IOException {
        List<String> lines = new ArrayList<>();
        int derivativeNumber = completeStateVariablesList.size();
        String odeFormat;
        String NT_loop = NT;
        if (n_loops == 2) {
            NT_loop = NT_INDIV;
        }
        if (checkIndiv() || hasCovariate) {
            if (odeInitialValueSubjDep) {
                odeFormat = "%s[%s,1:%s, 1:%s]" + assignSymbol + " " + this.odeCallName + modelNum
                        + "(%s[%s,1:%s], %s[%s,1:%s], %s[%s,],%s, %s)\n";
                pascalOdeCall = String.format(odeFormat, stateLabel, IND_S, NT_loop,
                        derivativeNumber + "", initLabel, IND_S, derivativeNumber + "", gridLabel, IND_S,
                        NT_loop, parName, IND_S, originLabel, tolVal);
            } else {
                odeFormat = "%s[%s,1:%s, 1:%s]" + assignSymbol + " " + this.odeCallName + modelNum
                        + "(%s[1:%s], %s[%s,1:%s], %s[%s,],%s, %s)\n";
                pascalOdeCall = String.format(odeFormat, stateLabel, IND_S, NT_loop,
                        derivativeNumber + "", initLabel, derivativeNumber + "", gridLabel, IND_S,
                        NT_loop, parName, IND_S, originLabel, tolVal);
            }
        } else {
            if (odeInitialValueSubjDep) {
                odeFormat = "%s[%s,1:%s, 1:%s]" + assignSymbol + " " + this.odeCallName + modelNum
                        + "(%s[%s,1:%s], %s[%s,1:%s], %s[],%s, %s)\n";
                pascalOdeCall = String.format(odeFormat, stateLabel, IND_S, NT_loop,
                        derivativeNumber + "", initLabel, IND_S, derivativeNumber + "", gridLabel, IND_S,
                        NT_loop, parName, originLabel, tolVal);
            } else {
                odeFormat = "%s[%s,1:%s, 1:%s]" + assignSymbol + " " + this.odeCallName + modelNum
                        + "(%s[1:%s], %s[%s,1:%s], %s[],%s, %s)\n";
                pascalOdeCall = String.format(odeFormat, stateLabel, IND_S, NT_loop,
                        derivativeNumber + "", initLabel, derivativeNumber + "", gridLabel, IND_S,
                        NT_loop, parName, originLabel, tolVal);
            }
        }
        lines.add(pascalOdeCall);
        if (hasDiffEquations) {
            lines.addAll(pascalCodeFileGeneration());
        }
        return lines;
    }

    protected int getNoTransfCovContNumber() {

        List<CovariateDefinition> covs = lexer.getCovariates();
        int n_cont = 0;
        for (CovariateDefinition cov : covs) {
            if (cov.getContinuous() != null) {
                if (cov.getContinuous().getListOfTransformation().isEmpty()) {
                    n_cont++;
                }
            }
        }
        return n_cont; 
    }

    protected int getCovCatNumber() {

        List<CovariateDefinition> covs = lexer.getCovariates();
        int n_cat = 0;
        for (CovariateDefinition cov : covs) {
            if (cov.getCategorical() != null) {
                if (cov.getCategorical().getListOfCategory().size() > 1) {                  
                    n_cat++;
                }
            }
        }
        return n_cat; 
    }

    // generates the pascal code based on the given template (pascal)
    @Override
    protected List<String> pascalCodeFileGeneration() throws FileNotFoundException, IOException {
        String pascalParamAssignLines;
        String pascalParamDeclLines;
        String pascalCovAssignement;
        String varLinesEq;
        List<String> lines = new ArrayList<>();
        List<String> varLines = new ArrayList<>();
        String format = "%s " + pascalAssignSymbol + " %s;\n";
        String formatTr = "%s(%s)";
        String modelName = getModelName();
        updateUniopMap();
        pascalParamDeclLines = Util.clean(pascalParametersDeclaration()); 
        pascalParamAssignLines = Util.clean(pascalThetaAssignement()); 
        pascalAssignIndivEq = Util.clean(concat(pascalAssignIndivEqLines, "\t\t"));
        pascalAssignVarEq = Util.clean(concat(pascalAssignVarEqLines, "\t\t"));
        pascalIndivEq = Util.clean(concat(Util.getUniqueString(pascalIndivLines), "\t\t"));
        varLinesEq = Util.clean(concat(varLines, "\t\t"));
        pascalCovAssignement = Util.clean(pascalCovAssignEqLines());
        pascalDiffEq = Util.clean(concat(pascalDiffEqLines, "\t\t")).trim(); 
        String nomeTemplate = hasCovariate ? templateCovFile : templateFile;
        format = getPascalTemplate(nomeTemplate);
        if (hasCovariate) {
            pascalBody = String.format(format,
                    modelNum,
                    derivativeSymbols.size(),
                    parName,
                    upperStateLabel,
                    upperStateLabel,
                    pascalParamDeclLines,
                    getNoTransfCovContNumber(),
                    pascalCovAssignement,
                    pascalParamAssignLines,
                    pascalAssignIndivEq + pascalAssignVarEq + pascalIndivEq + varLinesEq + pascalDiffEq,
                    modelNum);
            lines.addAll(winbugsOdeCovParAssignement());
        } else {
            pascalBody = String.format(format,
                    modelNum,
                    derivativeSymbols.size(),
                    parName,
                    upperStateLabel,
                    upperStateLabel,
                    pascalParamDeclLines,
                    pascalParamAssignLines,
                    pascalAssignIndivEq.toString() + pascalAssignVarEq.toString() + pascalIndivEq + varLinesEq + pascalDiffEq,
                    modelNum);
            lines.addAll(winbugsOdeParAssignement());
        }

        return lines;
    }

    // generates the differential equations (pascal)
    // example: dQ_UNIPVdt[0] := ((k21 * Q_UNIPV[2]) - ((k10 + k12) * Q_UNIPV[1]));
    protected String pascalDiffEquation(Object context, Node leaf) {
        String format = "%s " + pascalAssignSymbol + " %s;\n";
        String current_symbol = getSymbol((DerivativeVariable) context);
        // the derivative variable usedOdeContCovNames are saved in a list to be used in the method winbugsDerivativeDepVarAssignement()
        derivativeSymbols.add(getDerivativeSymbol((DerivativeVariable) context));
        // the line has to be transformed to substitute true usedOdeContCovNames with vector elements
        String tmp = new String(leaf.data.toString());
        String line = String.format(format, current_symbol, pascalNamesTransform(tmp));
        return line;
    }

    // generates the differential equations (pascal)
    // example: varQ2_UNIPV := (k21 * 1);
    protected String pascalVariableEquation(Object context, Node leaf) {
        String format = "%s " + pascalAssignSymbol + " %s;\n";
        String current_symbol = "";
        current_symbol = getSymbol(context);
        current_symbol = pascalNamesTransform(current_symbol);
        // the derivative variable usedOdeContCovNames are saved in a list to be used in the method winbugsDerivativeDepVarAssignement()
        // derivativeSymbols.add(getDerivativeSymbol((DerivativeVariable) context));
        // the line has to be transformed to substitute true usedOdeContCovNames with vector elements
        String tmp = new String(leaf.data.toString());
        tmp = pascalNamesTransform(tmp);
        String line = String.format(format, current_symbol, tmp);
        return line;
    }

    public String getSymbol(VariableDefinition o) {
        String symbol = doDerivativeDependentVariable(o); // var.getSymbId() + upperSuffixDerDepLabel (Parser)
        return delimit(symbol);
    }

    public String getSymbol(DerivativeVariable o) {
        String symbol = doDerivative(o); 
        return delimit(symbol);
    } 

    // appends to the left hand side of the differential eq. the right indexes
    public String getSymbol(ParameterRandomVariable o) {
        String symbol = doDerivativeRef(o.getSymbId());
        return delimit(symbol);
    } 

    // appends to the left hand side of the differential eq. the right indexes
    public String getSymbol(IndividualParameter o) {
        String symbol = doIndividualParameter(o);
        return delimit(symbol);
    } 

    // generates the left hand side of the differential eq. (pascal)
    // example: dQ_UNIPVdt[0]
    @Override
    protected String doDerivative(DerivativeVariable o) {
        String symbol = unassigned_symbol;
        String format = "";
        Integer idx = getStateVarIndex(o.getSymbId()); 
        format = "d" + upperStateLabel + "dt" + leftArrayBracket + "%s" + rightArrayBracket;
        symbol = String.format(format, idx);
        return symbol;
    }
}
