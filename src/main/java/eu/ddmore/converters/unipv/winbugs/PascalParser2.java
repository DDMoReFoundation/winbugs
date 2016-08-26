/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.converters.unipv.winbugs;

import crx.converter.tree.*;
import crx.converter.spi.blocks.*;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.InitialCondition;
import eu.ddmore.libpharmml.dom.commontypes.StandardAssignable;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author cristiana
 */
class PascalParser2 extends PascalParser1 {

    protected static final String templateWBDev_Mod = "TemplatePascal_WBDev_Mod.txt";
    protected static final String templateRK45_Pmetrics_Mod = "TemplatePascal_RK45_Pmetrics_Mod.txt";
    protected static final String templateLSODA_Pmetrics_Mod = "TemplatePascal_LSODA_Pmetrics_Mod.txt";
    protected static final String templateLSODA_Pmetrics_Mod_Cov_CONT = "TemplatePascal_LSODA_Pmetrics_Mod_Cov_CONT.txt";
    protected static final String templateRK45_Pmetrics_Mod_Cov_CONT = "TemplatePascal_RK45_Pmetrics_Mod_Cov_CONT.txt";
    protected static final String templateLSODA_Pmetrics_Mod_Cov_CONT_CAT = "TemplatePascal_LSODA_Pmetrics_Mod_Cov_CONT_CAT.txt";
    protected static final String templateRK45_Pmetrics_Mod_Cov_CONT_CAT = "TemplatePascal_RK45_Pmetrics_Mod_Cov_CONT_CAT.txt";
    protected static final String templateLSODA_Pmetrics_Mod_Cov_CAT = "TemplatePascal_LSODA_Pmetrics_Mod_Cov_CAT.txt";
    protected static final String templateRK45_Pmetrics_Mod_Cov_CAT = "TemplatePascal_RK45_Pmetrics_Mod_Cov_CAT.txt";
    // protected final String modelName = lexer.getModelName();
    private String amtLabel = "amt";
    private String rateLabel = "rate";
    private String iiLabel = "ii";
    private String evidLabel = "evid";
    private String cmtLabel = "cmt";
    private String addlLabel = "addl";
    private String ssLabel = "ss";
    private int tot_covariate_size = 0;

    private final String ODECALLNAME = "function.model";

    public void setTot_covariate_size(int tot) {
        this.tot_covariate_size = tot;
    }

    public PascalParser2() throws IOException {
        super();
        this.odeCallName = ODECALLNAME;

    }

    public void setPascalModFileName(File fileName) {
        this.pascalModFile = fileName;
    }

    public void setPascalPKmetricsFileName(File fileName) {
        this.pascalPMetricsFileName = fileName;
    }

    public void setOutputDirPascal2(String outputDirPascal2) {
        this.outputDirPascal2 = outputDirPascal2;
    }

    public void setOutputDirPascal2Mod(String outputDirPascal2Mod) {
        this.outputDirPascal2Mod = outputDirPascal2Mod;
    }

    boolean intersection(List<String> aa, List<String> bb) {
        for (String a : aa) {
            for (String b : bb) {
                if (a.equals(b)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean isIndepVarInList(List<SymbolRef> SymList) {
        for (SymbolRef s : SymList) {
            if (isIndependentVariableSym(s.getSymbIdRef())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected List<String> pascalCodeFileGeneration() throws FileNotFoundException, IOException {
        String pascalParamAssignLines;
        String pascalParamDeclLines;
        String pascalCovAssignement;
        String varLinesEq;
        String pascalICAssignLines;
        String pascalStateVarAssign;
        List<String> lines = new ArrayList<>();
        List<String> varLines = new ArrayList<>();
//        List<String> thetaInitLines = new ArrayList<>();
        String equation = "";
        String current_symbol = "", operand = "";
        String format = "%s " + pascalAssignSymbol + " %s;";
        updateUniopMap();
        String modelName = getModelName();

        winbugsIndivLines = Util.cleanEmpty(winbugsIndivLines);
        for (String indivLine : winbugsIndivLines) {
            if (indivLine != null && indivLine.length() > 0) {
                current_symbol = delimit(indivLine.substring(0, indivLine.indexOf(assignSymbol)).trim());
                if (current_symbol.contains("probit")) {
                    operand = "probit";
                } else if (current_symbol.contains("logit")) {
                    operand = "logit";
                } else if (current_symbol.contains("log")) {
                    operand = "log";
                } else {
                    operand = "";
                }

                if (transfMap.containsKey(current_symbol)) {
                    current_symbol = transfMap.get(current_symbol); // recupero la variabile assegnata
                }
                equation = unaryOperandEqMap.get(current_symbol); // recupero l'equazione
                String symbol = Util.clean(removeIndexes(current_symbol)).trim();

                String pascal_symbol = delimit(Util.clean(removeIndexes(current_symbol)).trim() + upperSuffixDerDepLabel);
                String pascal_equation;
                String tmp;
                tmp = removeIndexes(indivLine.substring(indivLine.indexOf(assignSymbol) + assignSymbol.length(), indivLine.length()));
                if (equation != null) {
                    if (operand.length() > 0) {
                        pascal_equation = doInverseTransformation(operand, pascalIndivMap.get(current_symbol));
                    } else {
                        pascal_equation = pascalIndivMap.get(current_symbol);
                    }
                    // sostituire nome state var
                    pascal_equation = removeIndexes(pascal_equation);
                    if (!unaryOperandEqMap.containsKey(current_symbol)) {
                        varLines.add(String.format(format, current_symbol, tmp.trim()));
                    }
                    if (isInList(Util.getList(odeParameters), symbol)) {
                        String eq = String.format("%s " + pascalAssignSymbol + " %s;\n", pascal_symbol, pascal_equation);
                        pascalIndivLines.add(eq);
                        if (isInList(Util.getList(leafOdeParameters), symbol)
                                && !isInList(Util.getList(theta_Parameters), symbol)) {
                            pascalAssignIndivEqLinesMap.put(Util.clean(removeIndexes(current_symbol)), eq);
                        }
                    }
                } else {
                    pascal_equation = removeIndexes(pascalIndivMap.get(current_symbol));
                    if (isInList(Util.getList(odeParameters), symbol)) {
                        String eq = String.format("%s " + pascalAssignSymbol + " %s;\n", pascal_symbol, pascal_equation.trim());
                        pascalIndivLines.add(eq);
                        pascalAssignIndivEqLinesMap.put(Util.clean(removeIndexes(current_symbol)), eq);
                    }
                }
            }
        }

        StringBuilder pwLines = new StringBuilder();
        Map<String, String> pwLinesMap = new HashMap();
        StringBuilder[] pwSb = generateIFELSEBlocks();
        for (Map.Entry<String, String> pw : piecewiseMap.entrySet()) {
            String sId = Util.clean(removeIndexes(pw.getKey()));
            if (Util.getList(leafOdeParameters).contains(sId) || Util.getList(odeParameters).contains(sId)) {
                Integer id = piecewiseIndexMap.get(sId);
                int index = id.intValue();
                pwLines.append(cleanPascal("\n\t\t" + pwSb[index - 1]));
                pwLinesMap.put(sId, cleanPascal(pwSb[index - 1].toString()));
            }
        }

//        Set<String> otherVar = new HashSet<>();
        Map<String, String> completeVarAssignEq = new HashMap<>();
        pascalParamDeclLines = Util.clean(pascalParametersDeclaration()); // modificare
        pascalParamAssignLines = Util.clean(pascalThetaAssignement()); // modificare
        pascalICAssignLines = Util.clean(pascalICAssignement());
        completeVarAssignEq = new HashMap<>(pascalAssignIndivEqLines);
//        completeVarAssignEq.putAll(pwLinesMap);
//        completeVarAssignEq.putAll(pascalAssignIndivEqLinesMap);
        completeVarAssignEq.putAll(pascalAssignVarEqLines); // togliere

        for (String line : pascalIndivLines) {
            completeVarAssignEq.put(Util.clean(line.substring(0, line.indexOf(pascalAssignSymbol))), line); // ok
        }

//        otherVar = completeVarAssignEq.keySet();
        Map<String, String> eqLines = new HashMap<>();
        String val, tmp;
        for (Map.Entry<String, String> line : completeVarAssignEq.entrySet()) {
            val = line.getValue();
            tmp = Util.clean(val.substring(0, val.indexOf(pascalAssignSymbol)).trim());
            if (tmp.endsWith(upperSuffixDerDepLabel)) {
                tmp = tmp.substring(0, tmp.indexOf(upperSuffixDerDepLabel));
            }
            eqLines.put(Util.clean(tmp), val);
        }
        eqLines.putAll(pwLinesMap);
//       eqLines = checkEquations(eqLines);
        // verifico se mancano equazioni necessarie e le costruisco
//        Map<String, String> tmpEq = new HashMap<>();
//        for (String var : eqLines.keySet()) {
//            if (parVariablesFromMap.get(var) != null) {
//                List<SymbolRef> depV = parVariablesFromMap.get(var);
//                for (SymbolRef v : depV) {
//                    if (eqLines.keySet().contains(v.getSymbIdRef()) || parVariablesFromMap.get(v)==null || isInList(Util.getList(theta_Parameters), v.getSymbIdRef())) {
//                        continue;
//                    } else {
//                        if (isContinuousCovariate(v) || isTransformedCovariate(v)) {
//                            String id, eq;
//                            id = v.getSymbIdRef();
//                            System.out.println("manca equazione della variabile " + id);
//                            Object o = lexer.getAccessor().fetchElement(id);
//                            if (o instanceof TransformedCovariate) {
//                                TransformedCovariate tc = (TransformedCovariate) o;
//                                eq = String.format("%s := %s;\n", id+upperSuffixDerDepLabel,pascalNamesTransform(myParse(tc.getParent())));
//                                tmpEq.put(id, eq);
//                            } else {
//                                CovariateDefinition  tc = (CovariateDefinition) o;
//                                eq = String.format("%s := %s;\n", id+upperSuffixDerDepLabel,pascalNamesTransform(myParse(tc)));
//                                tmpEq.put(id, eq);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        eqLines.putAll(tmpEq);

        completeVarAssignEq = new HashMap<>(eqLines);
//        otherVar = eqLines.keySet();
        
        // COMMENTO  da togliere
        pascalAssignIndivEq = cleanPascal(concat(sortVarLines(eqLines, getSortedOde()), "\t\t"));
        pascalIndivEq = cleanPascal(concat(Util.getUniqueString(pascalIndivLines), "\t\t")); // secondo blocco
        pascalAssignVarEq = cleanPascal(concat(sortVarLines(completeVarAssignEq, getSortedOde()), "\t\t")); // toINSERT 
        pascalStateVarAssign = cleanPascal(pascalVarAssignLines());
        pascalIndivEq = cleanPascal(concat(Util.getUniqueString(pascalIndivLines), "\t\t")); // secondo blocco
        varLinesEq = cleanPascal(concat(varLines, "\t\t"));
        pascalCovAssignement = cleanPascal(pascalCovAssignEqLines());
        pascalDiffEq = cleanPascal(concat(pascalDiffEqLines, "\t\t")).trim(); //modificare
        String nomeTemplate;
        if (DEBUG) {
            outDebug.println("ODE solver: " + getOdeSolver());
        }
        switch (getOdeSolver()) {
            case ConverterProvider.RK45SOLVER:
                if (usedOdeCatCovNames.size() > 0) {
                    if (usedOdeContCovNames.size() > 0) {
                        nomeTemplate = templateRK45_Pmetrics_Mod_Cov_CONT_CAT;
                    } else {
                        nomeTemplate = templateRK45_Pmetrics_Mod_Cov_CAT;
                    }
                } else if (usedOdeContCovNames.size() > 0) {
                    nomeTemplate = templateRK45_Pmetrics_Mod_Cov_CONT;
                } else {
                    nomeTemplate = templateRK45_Pmetrics_Mod;
                }
                break;

            case ConverterProvider.LSODASOLVER:
                if (usedOdeCatCovNames.size() > 0) {
                    if (usedOdeContCovNames.size() > 0) {
                        nomeTemplate = templateLSODA_Pmetrics_Mod_Cov_CONT_CAT;
                    } else {
                        nomeTemplate = templateLSODA_Pmetrics_Mod_Cov_CAT;
                    }
                } else if (usedOdeContCovNames.size() > 0) {
                    nomeTemplate = templateLSODA_Pmetrics_Mod_Cov_CONT;
                } else {
                    nomeTemplate = templateLSODA_Pmetrics_Mod;
                }
                break;
            default:
                throw new UnsupportedOperationException(getOdeSolver() + " not available");
        }
//        if(DEBUG) outDebug.println("<<- template " + nomeTemplate);
//        String nomeTemplate = hasCovariate ? templateCov : template; // templateWBDev_Mod
        format = getPascalTemplate(nomeTemplate);
// generazione ODEPascal
//        outDebug.println(
//                "templateWBDev_Mod = " + templateWBDev_Mod);
        String formatMod = getPascalTemplate(templateWBDev_Mod);

//        int totCov = usedOdeCatCovNames.size() + usedOdeContCovNames.size();
        //m.nParameter: 1 + 2*n°cov + 2*sum_i(max_m[i])*n°cov 
        //+ n° di parametri del modello +3*n°eq. differenziali
        //m.F1Index: 1 + 2*n°cov + 2*sum_i(max_m[i])*n°cov 
        //+ n° di parametri del modello + n°eq. differenziali
        //m.tlag1Index: 1 + 2*n°cov + 2*sum_i(max_m[i])*n°cov 
        //+n° di parametri del modello +2*n°eq. differenziali
        //m.nCmt: n° di equazioni differenziali
        int m_nParameter, m_F1Index, m_tlag1Index, nTheta = theta_Parameters.size();
        if (isIndepVarInList(theta_Parameters)) {
            nTheta--;
        }
        m_nParameter = Integer.parseInt(this.num1) + nTheta + 3 * derivativeSymbols.size();
        m_F1Index = Integer.parseInt(this.num1) + nTheta + derivativeSymbols.size();
        m_tlag1Index = Integer.parseInt(this.num1) + nTheta + 2 * derivativeSymbols.size();
// NO PIECEWISE
        if (piecewiseMap.isEmpty()) {
            // CON COVARIATE
//            if (hasCovariate) {
            if (usedOdeCatCovNames.size() > 0) {
                if (usedOdeContCovNames.size() > 0) {
// CONT + CAT
                    pascalBody = String.format(format,
                            modelNum,
                            derivativeSymbols.size(),
                            parName,
                            upperStateLabel,
                            upperStateLabel,
                            pascalParamDeclLines,
                            usedOdeContCovNames.size(),
                            usedOdeCatCovNames.size(),
                            Util.genPVect(usedOdeContCovNames, parContCovName),
                            Util.genPVect(usedOdeCatCovNames, parCatCovName),
                            getCovBlocks(),
                            pascalParamAssignLines + pascalICAssignLines + pascalStateVarAssign
                            + pascalAssignIndivEq
                            + varLinesEq
                            + pascalDiffEq,
                            m_nParameter, //m.nParameter
                            m_F1Index, //m.F1Index
                            m_tlag1Index, //m.tlag1Index
                            derivativeSymbols.size(), //m.nCmt
                            modelNum
                    );
                } else {
//  CAT
                    pascalBody = String.format(format,
                            modelNum,
                            derivativeSymbols.size(),
                            parName,
                            upperStateLabel,
                            upperStateLabel,
                            pascalParamDeclLines,
                            usedOdeCatCovNames.size(),
                            pascalCovAssignement,
                            pascalParamAssignLines + pascalICAssignLines,
                            pascalStateVarAssign
                            //                                        +pascalIndivEq
                            + pascalAssignIndivEq
                            //                                        + pascalAssignVarEq 
                            + varLinesEq
                            + pascalDiffEq,
                            m_nParameter, //m.nParameter
                            m_F1Index, //m.F1Index
                            m_tlag1Index, //m.tlag1Index
                            derivativeSymbols.size(), //m.nCmt
                            modelNum
                    );
                }
            } else if (usedOdeContCovNames.size() > 0) {
// CONT 
                pascalBody = String.format(format,
                        modelNum,
                        derivativeSymbols.size(),
                        parName,
                        upperStateLabel,
                        upperStateLabel,
                        pascalParamDeclLines,
                        usedOdeContCovNames.size(),
                        pascalCovAssignement,
                        pascalParamAssignLines + pascalICAssignLines,
                        pascalStateVarAssign + pascalAssignIndivEq + varLinesEq + pascalDiffEq,
                        m_nParameter, //m.nParameter
                        m_F1Index, //m.F1Index
                        m_tlag1Index, //m.tlag1Index
                        derivativeSymbols.size(), //m.nCmt
                        modelNum
                );
//                lines.addAll(winbugsOdeParAssignement());
            } else {
                // SENZA COVARIATE
                pascalBody = String.format(format,
                        modelNum,
                        derivativeSymbols.size(),
                        parName,
                        upperStateLabel,
                        upperStateLabel,
                        pascalParamDeclLines,
                        pascalParamAssignLines + pascalICAssignLines,
                        pascalStateVarAssign + pascalAssignIndivEq + varLinesEq + pascalDiffEq,
                        m_nParameter, //m.nParameter
                        m_F1Index, //m.F1Index
                        m_tlag1Index, //m.tlag1Index
                        derivativeSymbols.size(),
                        modelNum);
//                lines.addAll(winbugsOdeParAssignement());
            }
        } else {
// CON ODE E CON PIECEWISE
            inPW = false;

// CON COVARIATE
//            if (hasCovariate) {
            if (usedOdeCatCovNames.size() > 0) {
                if (usedOdeContCovNames.size() > 0) {
// CONT + CAT
                    pascalBody = String.format(format,
                            modelNum,
                            derivativeSymbols.size(),
                            parName,
                            upperStateLabel,
                            upperStateLabel,
                            pascalParamDeclLines,
                            usedOdeContCovNames.size(),
                            usedOdeCatCovNames.size(),
                            Util.genPVect(usedOdeContCovNames, parContCovName),
                            Util.genPVect(usedOdeCatCovNames, parCatCovName),
                            //                                pascalCovAssignement,
                            pascalParamAssignLines + pascalICAssignLines,
                            pascalStateVarAssign + pascalAssignIndivEq + varLinesEq + pascalDiffEq,
                            m_nParameter, //m.nParameter
                            m_F1Index, //m.F1Index
                            m_tlag1Index, //m.tlag1Index
                            derivativeSymbols.size(), //m.nCmt
                            modelNum
                    );
                } else {
//  CAT
                    pascalBody = String.format(format,
                            modelNum,
                            derivativeSymbols.size(),
                            parName,
                            upperStateLabel,
                            upperStateLabel,
                            pascalParamDeclLines,
                            usedOdeCatCovNames.size(),
                            pascalCovAssignement,
                            pascalParamAssignLines + pascalICAssignLines,
                            pascalStateVarAssign + pascalAssignIndivEq + varLinesEq + pascalDiffEq,
                            m_nParameter, //m.nParameter
                            m_F1Index, //m.F1Index
                            m_tlag1Index, //m.tlag1Index
                            derivativeSymbols.size(), //m.nCmt
                            modelNum
                    );
                }
            } else if (usedOdeContCovNames.size() > 0) {
// CONT 
                pascalBody = String.format(format,
                        modelNum,
                        derivativeSymbols.size(),
                        parName,
                        upperStateLabel,
                        upperStateLabel,
                        pascalParamDeclLines,
                        usedOdeContCovNames.size(),
                        pascalCovAssignement,
                        pascalParamAssignLines + pascalICAssignLines + pascalStateVarAssign + pascalAssignIndivEq + varLinesEq,
                        pascalDiffEq,
                        m_nParameter, //m.nParameter
                        m_F1Index, //m.F1Index
                        m_tlag1Index, //m.tlag1Index
                        derivativeSymbols.size(), //m.nCmt
                        modelNum
                );
            } else {
                pascalBody = String.format(format,
                        modelNum,
                        derivativeSymbols.size(),
                        parName,
                        upperStateLabel,
                        upperStateLabel,
                        pascalParamDeclLines,
                        usedOdeContCovNames.size(),
                        pascalCovAssignement,
                        pascalParamAssignLines + pascalICAssignLines,
                        pascalStateVarAssign + pascalAssignIndivEq + varLinesEq + pascalDiffEq,
                        m_nParameter, //m.nParameter
                        m_F1Index, //m.F1Index
                        m_tlag1Index, //m.tlag1Index
                        derivativeSymbols.size(), //m.nCmt
                        modelNum
                );
            }

//            lines.addAll(winbugsOdeParAssignement());
        }
        lines.addAll(winbugsOdeParAssignement());
        pascalMod = String.format(formatMod, modelNum, "" + modelNum, "" + modelNum, modelNum);
        return lines;
    }

    /*
     q[i,1:N_t[i]] <- TwoCompExample(grid[ind_subj,1:N_t[ind_subj]], amt[ind_subj,1:N_t[ind_subj]], 
     rate[ind_subj,1:N_t[ind_subj]], ii[ind_subj,1:N_t[ind_subj]], evid[ind_subj,1:N_t[ind_subj]], 
     cmt[ind_subj,1:N_t[ind_subj]], 
     addl[ind_subj,1:N_t[ind_subj]], ss[ind_subj,1:N_t[ind_subj]], theta[ind_subj,])
     */
// generates the call statement to the pascal solver (winbugs)
// example: q_unipv[1:N_t, 1:2]<- one.comp.model(initial_value[1:2], grid[1:N_t], theta[1:2],0.0, 0.001)
    @Override
    protected List<String> winbugsPascalOdeCallGen() throws FileNotFoundException, IOException {
        List<String> lines = new ArrayList<>();
        // TOCHECK forse dipende dal numero di parameter blocks
        int derivativeNumber = completeStateVariablesList.size();
        String odeFormat;
        String NT_loop = NT;

        if (n_loops == 2) {
            NT_loop = NT_INDIV;
        }
        if (checkIndiv() || hasCovariate || odeInitialValueSubjDep) {
            odeFormat = "%s[%s,1:%s, 1:%s]" + assignSymbol + " " + this.odeCallName + modelNum
                    + "(%s[%s,1:%s], %s[%s,1:%s],  %s[%s,1:%s], %s[%s,1:%s], %s[%s,1:%s],"
                    + " %s[%s,1:%s], %s[%s,1:%s], %s[%s,1:%s], %s[%s,])";
            pascalOdeCall = String.format(odeFormat,
                    stateLabel,
                    IND_S,
                    NT_loop,
                    derivativeNumber + "",
                    gridLabel,
                    IND_S,
                    NT_INDIV + "",
                    amtLabel,
                    IND_S,
                    NT_INDIV + "",
                    rateLabel,
                    IND_S,
                    NT_INDIV + "",
                    iiLabel,
                    IND_S,
                    NT_INDIV + "",
                    evidLabel,
                    IND_S,
                    NT_INDIV + "",
                    cmtLabel,
                    IND_S,
                    NT_INDIV + "",
                    addlLabel,
                    IND_S,
                    NT_INDIV + "",
                    ssLabel,
                    IND_S,
                    NT_INDIV + "",
                    parName,
                    IND_S);

        } else {
            odeFormat = "%s[%s,1:%s, 1:%s]" + assignSymbol + " " + this.odeCallName + modelNum
                    + "(%s[%s,1:%s], %s[%s,1:%s],  %s[%s,1:%s], %s[%s,1:%s], %s[%s,1:%s],"
                    + " %s[%s,1:%s], %s[%s,1:%s], %s[%s,1:%s], %s[])";
            pascalOdeCall = String.format(odeFormat,
                    stateLabel,
                    IND_S,
                    NT_loop,
                    derivativeNumber + "",
                    gridLabel,
                    IND_S,
                    NT_INDIV + "",
                    amtLabel,
                    IND_S,
                    NT_INDIV + "",
                    rateLabel,
                    IND_S,
                    NT_INDIV + "",
                    iiLabel,
                    IND_S,
                    NT_INDIV + "",
                    evidLabel,
                    IND_S,
                    NT_INDIV + "",
                    cmtLabel,
                    IND_S,
                    NT_INDIV + "",
                    addlLabel,
                    IND_S,
                    NT_INDIV + "",
                    ssLabel,
                    IND_S,
                    NT_INDIV + "",
                    parName);

        }
        lines.add(pascalOdeCall);
//        updateNum1();
        if (hasDiffEquations) {
            lines.addAll(pascalCodeFileGeneration());
        }
//        outDebug.println("winbugsPascalOdeCallGen");
        return lines;
    }

    

    private boolean checkInitValSubDep() {
        boolean flag = false;
        for (StructuralBlock sb : lexer.getStructuralBlocks()) {
            flag = flag || checkInitValSubDep(sb);
        }
        return flag;
    }

    private boolean checkInitValSubDep(StructuralBlock _sb) {
        // verifico se le initial condition dipendono dal soggetto (setto il flag odeInitialValueSubjDep)
        for (int i = 0; i < _sb.getStateVariables().size(); i++) {
            DerivativeVariable var = _sb.getStateVariables().get(i);
            lexer.setCurrentStructuralBlock(_sb);

            InitialCondition ic = var.getInitialCondition();
            StandardAssignable initV = ic.getInitialValue(); // InitialValue initV = ic.getInitialValue(); CRI 0.7.3
            String initialCondition = "0";
            TreeMaker tm = lexer.getTreeMaker();
            BinaryTree bt;
            if (initV != null) {
                if (initV.getAssign() != null) {
                    // valore scalare
                    if (initV.getAssign().getScalar() != null) {
                        initialCondition = "" + initV.getAssign().getScalar().valueToString();
                    } else if (initV.getAssign().getSymbRef() != null) {
                        // riferimento a parametro
                        initialCondition = "" + doSymbolRef(initV.getAssign().getSymbRef());
//                    } else if (initV.getAssign().getEquation() != null) { 0.7.3
                    } else if (initV.getAssign() != null) {
                        bt = tm.newInstance(ic);
//                        initialCondition = parse(initV, lexer.getStatement(ic));
                        initialCondition = parse(initV, bt);
                    }
                    if (initialCondition.contains(IND_S)) {
                        odeInitialValueSubjDep = true;
                        break;
                    }
                }
            }
        }
        return odeInitialValueSubjDep;
    }

    @Override
    protected List<String> winbugsOdeCovParAssignement() {
        List<String> lines = new ArrayList<>();
        lines.addAll(winbugsOdeParAssignement());
        return lines;
    }

    private String genIndexNames(String index, List<SymbolRef> odeCovNames, int start) {
        String covNames = index;
        String format = "%s%s%s,%s%s %s %s";
        int ind = start;
        List<String> lines = new ArrayList<>();
        // genero righe del tipo: theta[ind_subj,4] <- max_m_AGE
        for (String id : Util.getNames(odeCovNames)) {
            covNames += "+2*" + maxNamePrefix + id;
            lines.add(String.format(format, parName, leftArrayBracket, IND_S, ind, rightArrayBracket, assignSymbol, maxNamePrefix + id));
            ind++;
        }
        return covNames;
    }

    private List<String> genCovSumList(List<SymbolRef> odeCovNames, Map<String, String> accessors, boolean categ) {
        List<String> lines = new ArrayList<>();
        int ind;
        String covNames;
        if (!categ) {
            ind = Integer.parseInt(accessors.get("ind"));
            covNames = accessors.get("contNames");
        } else {
            ind = Integer.parseInt(accessors.get("ind"));
            covNames = accessors.get("catNames");
        }

//        int is0 = 0;
        String format = "%s%s%s,%s%s %s %s";

        String index = "";
        if (usedOdeCatCovNames.size() > 0 || usedOdeContCovNames.size() > 0) { // 27 luglio
//        if (hasCovariate) {
            if (covNames.trim().length() > 0) {
                index = covNames + "+" + ind;
            } else {
                index = "" + ind;
            }
//            is0 = (ind + 2 * usedOdeContCovNames.size()); // (1 + 2 * usedOdeContCovNames.size()) getNoTransfCovContNumber()
            if (usedOdeContCovNames.size() > 0 && !categ) { // 27 luglio
//            if (!categ) {
                // genero la riga: theta[ind_subj,1] <- n_cov_cont
                lines.add(String.format(format, parName, leftArrayBracket, IND_S, index, rightArrayBracket, assignSymbol, "n_cov_cont"));
            }
            if (usedOdeCatCovNames.size() > 0 && categ) { // 27 luglio
                // genero la riga: theta[ind_subj,1] <- n_cov_cat
                lines.add(String.format(format, parName, leftArrayBracket, IND_S, index, rightArrayBracket, assignSymbol, "n_cov_cat"));
            }
            if (usedOdeContCovNames.size() > 0) {
                if (usedOdeCatCovNames.size() > 0) {
                    covNames = Util.genCompleteIndexNames(index, Util.merge(usedOdeContCovNames, usedOdeCatCovNames), maxNamePrefix);
                } else {
                    covNames = genIndexNames(index, usedOdeContCovNames, ind);
                }
            } else if (usedOdeCatCovNames.size() > 0) {
                covNames = Util.genCompleteIndexNames(index, usedOdeCatCovNames, maxNamePrefix);
            }
        }
        ind++;

//        salvo l'idice a cui sono arrivata per riutilizzarlo dopo nella costruzione dei theta
        if (!categ) {
            accessors.put("ind", ind + "");
            accessors.put("contNames", covNames);
        } else {
            accessors.put("ind", ind + "");
            accessors.put("catNames", covNames);
        }
        return lines;
    }

    private List<String> genCovAssign(List<SymbolRef> odeCovNames, Map<String, String> accessors, boolean categ, String prefix) {// TOCHECK indexes
        List<String> lines = new ArrayList<>();
        String thetaFormat;
        if (prefix.equals(maxNamePrefix)) {
            thetaFormat = parName + leftArrayBracket + IND_S + ",%s" + rightArrayBracket + " " + assignSymbol + " %s" + "\n";
        } else {
            thetaFormat = parName + leftArrayBracket + IND_S + ",%s" + rightArrayBracket + " " + assignSymbol + " %s" + leftArrayBracket + IND_S + rightArrayBracket + "\n";
        }
        int ind;
        if (!categ) {
            ind = Integer.parseInt(accessors.get("ind"));
        } else {
            ind = Integer.parseInt(accessors.get("ind"));
        }
        for (String id : Util.getNames(odeCovNames)) {
            lines.add(String.format(thetaFormat, ind, prefix + id));
            ind++;
        }
        accessors.put("ind", ind + "");

        return lines;
    }

    private List<String> genThetaCovAssign(List<SymbolRef> thetaNames, Map<String, String> accessors, boolean categ) {
        List<String> lines = new ArrayList<>();
        String symbol, s1 = "", start;

        if (usedOdeContCovNames.size() > 0 && usedOdeCatCovNames.size() > 0) {
            start = "" + (2 * usedOdeContCovNames.size() + 2 * usedOdeCatCovNames.size() + 2)
                    //                    (Integer.parseInt(accessors.get("ind")) - 1)
                    + Util.getThetaIndex(usedOdeContCovNames, usedOdeCatCovNames, maxNamePrefix)
                    + " +";// genero la riga: theta[ind_subj,1] <- n_cov
        } else if (usedOdeContCovNames.size() > 0) {
            start = "" + (2 * usedOdeContCovNames.size() + 1)
                    //                    (Integer.parseInt(accessors.get("ind")) - 1)
                    + Util.getThetaIndex(usedOdeContCovNames, usedOdeCatCovNames, maxNamePrefix)
                    + " +";// genero la riga: theta[ind_subj,1] <- n_cov
        } else if (usedOdeCatCovNames.size() > 0) {
            start = "" + (2 * usedOdeCatCovNames.size() + 1)
                    //                    (Integer.parseInt(accessors.get("ind")) - 1)
                    + Util.getThetaIndex(usedOdeContCovNames, usedOdeCatCovNames, maxNamePrefix)
                    + " +";// genero la riga: theta[ind_subj,1] <- n_cov
        } else {
            start = "";
        }
        int indice = 1;
        for (SymbolRef s : thetaNames) {//theta_Parameters) {
            symbol = getSymbol(s);
            if (!isIndependentVariableSym(s.getSymbIdRef())) {
                s1 = start + indice;
                indice++;
                {
                    String formatI = "%s%s%s,%s%s %s %s%s\n";
                    String formatNI = "%s%s%s%s %s %s%s\n";
                    if (!odeInitialValueSubjDep && !hasCovariate) {
                        lines.add(String.format(formatNI, parName, leftArrayBracket, s1, rightArrayBracket, assignSymbol, symbol, ""));
                    } else if (isDosingTime(s.getSymbIdRef())) {
                        lines.add(String.format(formatNI, parName, leftArrayBracket, IND_S + "," + s1, rightArrayBracket, assignSymbol, symbol, IND_SUBJ));
                    } else {
                        lines.add(String.format(formatI, parName, leftArrayBracket, IND_S, s1, rightArrayBracket, assignSymbol, symbol, ""));
                    }
                }
            }
        }

        accessors.put("ind", indice + "");
        if (!categ) {
            accessors.put("contNames", s1);
        } else {
            accessors.put("catNames", s1);
        }
        return lines;
    }

    @Override
    protected List<String> winbugsOdeParAssignement() {
        List<String> lines = new ArrayList<>();
        int ind = indWB0;
        int n_cov_cont = usedOdeContCovNames.size();
        int n_cov_cat = usedOdeCatCovNames.size();
        odeInitialValueSubjDep = checkInitValSubDep() || checkIndiv();
        Map<String, String> accessors = new HashMap<>();
        accessors.put("ind", ind + "");
        accessors.put("contNames", "");
        accessors.put("catNames", "");
        if (usedOdeContCovNames.size() > 0) {
            lines.addAll(genCovSumList(usedOdeContCovNames, accessors, false));
            ind += usedOdeContCovNames.size() + 1;
            lines.addAll(genCovAssign(usedOdeContCovNames, accessors, false, maxNamePrefix));
            lines.addAll(genCovAssign(usedOdeContCovNames, accessors, false, NtNamePrefix));
            ind += usedOdeContCovNames.size();
        }

        if (usedOdeCatCovNames.size() > 0) {
            lines.addAll(genCovSumList(usedOdeCatCovNames, accessors, true));
            ind += usedOdeCatCovNames.size() + 1;
            lines.addAll(genCovAssign(usedOdeCatCovNames, accessors, true, maxNamePrefix));
            lines.addAll(genCovAssign(usedOdeCatCovNames, accessors, true, NtNamePrefix));
            ind += usedOdeCatCovNames.size();
        }

        lines.addAll(genThetaCovAssign(theta_Parameters, accessors, true));
        return lines;
    }

    @Override
    protected String pascalParametersDeclaration() {
        String format = "%s: REAL;\n\t\t";
        String l = super.pascalParametersDeclaration();
        return l + stateVarDecl(format) + stateICDecl(format);
    }

    private List<String> genGridLines(int nCov, List<String> names) {
        String tmpGridIndex = "", gridIndex;
        int iCov = 1;

        int start;
        if (usedOdeContCovNames.size() > 0 && usedOdeCatCovNames.size() > 0) {
            start = 2 * (1 + usedOdeContCovNames.size() + usedOdeCatCovNames.size());
        } else if (usedOdeContCovNames.size() > 0) {
            start = 1 + 2 * usedOdeContCovNames.size();
        } else if (usedOdeCatCovNames.size() > 0) {
            start = 1 + 2 * usedOdeCatCovNames.size();
        } else {
            start = 1 + names.size();
        }
        gridIndex = "i+" + start;
        tmpGridIndex = "i+" + start;
        List<String> gridLines = new ArrayList<>();
        String maxN, thetaFormat = "%s%s%s,%s%s %s %s%s%s%s,i%s\n";

        // creo i due cicli (per ogni covariata) che assegnano ai theta la gliglia della covariata e i valori
//        gridIndex = "i+" + start;
//        tmpGridIndex = "i+" + start;
        for (String id : names) {
            iCov++;
            maxN = maxNamePrefix + id;
            gridLines.add(String.format("for (i in 1 : %s){ \n", maxN));
            gridLines.add(String.format(thetaFormat, parName,
                    leftArrayBracket, IND_S, gridIndex, rightArrayBracket,
                    assignSymbol, gridPrefix, id,
                    leftArrayBracket, IND_S, rightArrayBracket
            ));
            tmpGridIndex += "+2*" + maxN;
            gridIndex += "+" + maxN;
            gridLines.add("}\n");
            gridLines.add(String.format("for (i in 1 : %s){ \n", maxN));
            gridLines.add(String.format(thetaFormat, parName,
                    leftArrayBracket, IND_S, gridIndex, rightArrayBracket,
                    assignSymbol, id, "",
                    leftArrayBracket, IND_S, rightArrayBracket));
            gridLines.add("}\n\n");
            gridIndex = tmpGridIndex;
        }
        return gridLines;
    }

    protected List<String> winbugsOdeInitialValues(StructuralBlock _sb) {
        List<String> lines = new ArrayList<>();
        List<String> tmpICLines = new ArrayList<>();
        List<String> gridLines = new ArrayList<>();

        // verifico se le initial condition dipendono dal soggetto (setto il flag odeInitialValueSubjDep)
        odeInitialValueSubjDep = checkInitValSubDep(_sb) || checkIndiv();
//        String s0, tmpGridIndex = "", s2, gridIndex;
        // creo i due cicli (per ogni covariata) che assegnano ai theta la gliglia della covariata e i valori
        // righe relative alle covariate
        gridLines.addAll(genGridLines(usedOdeContCovNames.size(), Util.getNames(usedOdeContCovNames, usedOdeCatCovNames)));
        tmpICLines.addAll(genIcTlagLines(_sb, Util.getNames(usedOdeContCovNames, usedOdeCatCovNames)));
        lines.add(concat(gridLines, "\t"));
        lines.addAll(tmpICLines);
//        lines.addAll(tmpFLines);
//        lines.addAll(tmpTLAGLines);
        return lines;
    }

    private List<String> genIcTlagLines(StructuralBlock _sb, List<String> names) {
        List<String> lines = new ArrayList<>();
        List<String> tmpICLines = new ArrayList<>();
        List<String> tmpFLines = new ArrayList<>();
        List<String> tmpTLAGLines = new ArrayList<>();
        String formatInit = "%s[%s] <- %s #%s %s\n";
        String formatIndivInit = "%s[%s,%s] <- %s #%s %s\n";
        int nInitTime = 0;
        int nTheta = theta_Parameters.size();
        if (isIndepVarInList(theta_Parameters)) {
            nTheta--;
        }
        int nState = completeStateVariablesList.size();
        String fVal = "1", tlagVal = "0";
        String s2, s3;
        int nCov = names.size();

        int start;
        String fIndex;
        for (int i = 0; i < nState; i++) {
            fVal = "1";
            tlagVal = "0";
            DerivativeVariable var = _sb.getStateVariables().get(i);
            lexer.setCurrentStructuralBlock(_sb);
            String tmp = getFVal(var.getSymbId());
            fVal = tmp != null ? tmp : fVal;
            tmp = getTlagVal(var.getSymbId());
            tlagVal = tmp != null ? tmp : tlagVal;
            StandardAssignable initT = var.getInitialCondition().getInitialTime();// InitialTime initT = var.getInitialCondition().getInitialTime(); CRI 0.7.3
            String initialCondition = getStateInitialCondition(var);

            int startT = nState + nTheta;// MOD
            if (usedOdeCatCovNames.size() > 0 && usedOdeContCovNames.size() > 0) {
                fIndex = 2 + 2 * nCov + "";
            } else {
                fIndex = 1 + 2 * nCov + "";
            }
            for (String id : names) {
                fIndex += "+2*" + maxNamePrefix + id;
            }
            start = nTheta + 1;

            if (!hasCovariate) {
                if (!odeInitialValueSubjDep) {
                    tmpICLines.add(String.format(formatInit, parName,
                            nTheta + 1 + i, initialCondition,
                            "IC", var.getSymbId()));
                } else {
                    tmpICLines.add(String.format(formatIndivInit, parName,
                            IND_S, nTheta + 1 + i, initialCondition,
                            "IC", var.getSymbId()));
                }
            } else {
                tmpICLines.add(String.format(formatIndivInit, parName,
                        IND_S, fIndex + "+" + (i + start),
                        initialCondition, "IC", var.getSymbId()));
            }

            if (!hasCovariate) {
                s2 = "" + (nTheta + nState + i + 1);
            } else {
                s2 = fIndex + "+" + (nTheta + nState + i + 1);
            }
            if (initT != null) {
                nInitTime++;

                if (!hasCovariate && !odeInitialValueSubjDep) {
                    tmpFLines.add(String.format(formatInit, parName,
                            s2, fVal, "F", var.getSymbId()));
                } else {
                    tmpFLines.add(String.format(formatIndivInit, parName,
                            IND_S, s2, fVal, "F", var.getSymbId()));
                }
            } else {
                if (hasCovariate) {
                    s2 = fIndex + "+" + (startT + 1 + i);
                } else {
                    s2 = "" + (nTheta + nState + i);
                }
                if (!hasCovariate && !odeInitialValueSubjDep) {
                    tmpFLines.add(String.format(formatInit, parName,
                            s2, fVal, "F", var.getSymbId()));
                } else if (!hasCovariate) {
                    tmpFLines.add(String.format(formatIndivInit, parName,
                            IND_S, s2, fVal,
                            "F", var.getSymbId()));
                } else {
                    tmpFLines.add(String.format(formatIndivInit, parName,
                            IND_S, s2, fVal,
                            "F", var.getSymbId()));
                }

                if (nInitTime
                        > 1) {
                    throw new IllegalStateException("Multiple ODE initial times are not allowed.");
                }
            }

            if (hasCovariate) {
                s3 = fIndex + "+" + (startT + nState + i + 1);
            } else {
                s3 = "" + (nTheta + 2 * nState + i + 1);
            }
            if (!hasCovariate && !odeInitialValueSubjDep) {
                tmpTLAGLines.add(String.format(formatInit, parName,
                        s3, tlagVal, "TLAG", var.getSymbId()));
            } else if (!hasCovariate) {
                tmpTLAGLines.add(String.format(formatIndivInit, parName,
                        IND_S, s3, tlagVal,
                        "TLAG", var.getSymbId()));
            } else {
                tmpTLAGLines.add(String.format(formatIndivInit, parName,
                        IND_S, s3, tlagVal,
                        "TLAG", var.getSymbId()));
            }

        }
        lines.addAll(tmpICLines);
        lines.addAll(tmpFLines);
        lines.addAll(tmpTLAGLines);
        return lines;
    }

    protected boolean checkIndiv() {
        boolean isIndividual = false;// = isodeParIndividual() || lexer.getCovariateBlocks().size() > 0;
        String symbol;
        for (SymbolRef s : theta_Parameters) {
            symbol = getSymbol(s);
            if (symbol.contains(IND_S)) {
                isIndividual = true;
            }
        }
        return isIndividual || odeInitialValueSubjDep;
    }

    private String pascalICAssignement() {
        StringBuilder sb = new StringBuilder();
        int nTheta = theta_Parameters.size();
        if (isIndepVarInList(theta_Parameters)) {
            nTheta--;
        }
        int ind = indPAS0 + nTheta;
        String format = "%s %s %s%s%s%s;\n\t\t";
        String formatCov = "%s %s %s%slastindex+%s%s;\n\t\t";
        for (int i = 0; i < completeStateVariablesList.size(); i++) {
            if (hasCovariate) {
                sb.append(String.format(formatCov, icLabel + i, pascalAssignSymbol, parName, leftArrayBracket, ind++, rightArrayBracket));
            } else {
                sb.append(String.format(format, icLabel + i, pascalAssignSymbol, parName, leftArrayBracket, ind++, rightArrayBracket));
            }

        }
        return sb.toString();
    }

    private String pascalVarAssignLines() {
        StringBuilder sb = new StringBuilder();
        String format = "%s %s %s%s%s%s+%s;\n\t\t";
        String formatCov = "%s %s %s%s%s%s+%s;\n\t\t";

        for (int i = 0; i < completeStateVariablesList.size(); i++) {
            if (hasCovariate) {
                sb.append(String.format(formatCov, varLabel + i, pascalAssignSymbol, upperStateLabel, leftArrayBracket, i, rightArrayBracket, icLabel + i));
            } else {
                sb.append(String.format(format, varLabel + i, pascalAssignSymbol, upperStateLabel, leftArrayBracket, i, rightArrayBracket, icLabel + i));
            }

        }
        return sb.toString();
    }
}
