/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.converters.unipv.winbugs;

import static crx.converter.engine.PharmMLTypeChecker.isCovariateTransform;
import static crx.converter.engine.PharmMLTypeChecker.isDerivative;
import static crx.converter.engine.PharmMLTypeChecker.isDistributionParameter;
import static crx.converter.engine.PharmMLTypeChecker.isIndividualParameter;
import static crx.converter.engine.PharmMLTypeChecker.isLocalVariable;
import static crx.converter.engine.PharmMLTypeChecker.isPopulationParameter;
import crx.converter.spi.ILexer;
import static eu.ddmore.converters.unipv.winbugs.Parser.isInList;
import eu.ddmore.libpharmml.dom.commontypes.CommonVariableDefinition;
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLElement;
import eu.ddmore.libpharmml.dom.commontypes.PharmMLRootType;
import eu.ddmore.libpharmml.dom.commontypes.Rhs;
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef;
import eu.ddmore.libpharmml.dom.commontypes.VariableDefinition;
import eu.ddmore.libpharmml.dom.modeldefn.CommonParameter;
import eu.ddmore.libpharmml.dom.modeldefn.CovariateTransformation;
import eu.ddmore.libpharmml.dom.modeldefn.Distribution;
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter;
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable;
import eu.ddmore.libpharmml.dom.modeldefn.PopulationParameter;
import eu.ddmore.libpharmml.dom.probonto.DistributionParameter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author cristianalarizza
 */
public class Util {

    public static String delimiter = "\"";

    public static PharmMLElement getDistributionType(ParameterRandomVariable context) { 

        Distribution dist = context.getDistribution();
        if (dist.getUncertML() != null) {
            if (dist.getUncertML().getAbstractCategoricalMultivariateDistribution() != null) {
                return dist.getUncertML().getAbstractCategoricalMultivariateDistribution().getValue();
            } else if (dist.getUncertML().getAbstractCategoricalUnivariateDistribution() != null) {
                return dist.getUncertML().getAbstractCategoricalUnivariateDistribution().getValue();
            } else if (dist.getUncertML().getAbstractContinuousMultivariateDistribution() != null) {
                return dist.getUncertML().getAbstractContinuousMultivariateDistribution().getValue();
            } else if (dist.getUncertML().getAbstractContinuousUnivariateDistribution() != null) {
                return dist.getUncertML().getAbstractContinuousUnivariateDistribution().getValue();
            } else {
                throw new RuntimeException("Distribution Type not supported");
            }
        } else if (dist.getProbOnto() != null) {
            return dist.getProbOnto();
        }
        return null; 
    }

    public static List<String> getNames(List<SymbolRef> l) {
        List<String> list = new ArrayList<>();
        for (SymbolRef s : l) {
            list.add(s.getSymbIdRef());
        }
        return list;
    }

    public static List<String> append(List<String> lines, List<String> linesIn) {
        for (String l : linesIn) {
            l = l.trim();
        }
        if (linesIn != null) {
            lines.addAll(linesIn);
        }
        return lines;
    }

    public static String getDataFromDataFile(String dataFile, String name){
        String start = "list(";
        String data = dataFile.substring(dataFile.indexOf(start)+start.length());
        int tmp = data.indexOf(", "+name);
        if(tmp == -1)
            tmp = data.indexOf(","+name);
        if(tmp == -1)
            tmp = data.indexOf(name);
        String last = data.substring(tmp+name.length()+1);
        int pos = last.indexOf("=");
        last = last.substring(pos+1).trim();
        switch(last.substring(0, 2)){
            case "st":
                pos = last.indexOf("))")+2;
                last = last.substring(0, pos);
               
                break;
            case "c(":
                last = last.substring(0, last.indexOf(")")+1);
                
                break;
            default:
                last = last.substring(0,last.indexOf(","));
                break;          
        }
        return last;
    }
    
    private static String getName(String n) {
        if (!n.contains("[")) {
            return n;
        } else {
            return n.substring(0, n.indexOf("["));
        }
    }

    public static List<String> getNames(List<SymbolRef> l1, List<SymbolRef> l2) {
        List<String> list = new ArrayList<>();
        for (SymbolRef s : l1) {
            list.add(s.getSymbIdRef());
        }
        for (SymbolRef s : l2) {
            list.add(s.getSymbIdRef());
        }
        return list;
    }

    public static String getThetaIndex(List<SymbolRef> cont, List<SymbolRef> cat, String maxNamePrefix) {
        String out = "";
        if (cont.size() > 0) {
            if (cat.size() > 0) {
                out = genCompleteIndexNames("", merge(cont, cat), maxNamePrefix);
            } else {
                out = genCompleteIndexNames("", cont, maxNamePrefix);
            }
        } else if (cat.size() > 0) {
            out = genCompleteIndexNames("", cat, maxNamePrefix);
        }
        return out;
    }

    public static String genPVect(List<SymbolRef> list, String pName) {
        StringBuilder out = new StringBuilder();
        int ind = 0;
        for (SymbolRef sr : list) {
            out.append(String.format("%s := %s[%s];\n\t\t", sr.getSymbIdRef(), pName, ind));
            ind++;
        }
        return out.toString();
    }

    public static String genCompleteIndexNames(String index, List<SymbolRef> odeCovNames, String maxNamePrefix) {
        String covNames = index;
        for (String id : getNames(odeCovNames)) {
            covNames += "+2*" + maxNamePrefix + id;
        }
        return covNames;
    }

    public static List<String> cleanEmpty(List<String> in) {
        List<String> out = new ArrayList<>();
        for (String l : in) {
            if (l.trim().length() > 0) {
                out.add(l);
            }
        }
        return out;
    }

    public static List<SymbolRef> merge(List<SymbolRef> l1, List<SymbolRef> l2) {
        List<SymbolRef> out = new ArrayList<>(l1);
        for (SymbolRef sr : l2) {
            out.add(sr);
        }
        return out;
    }

    public static PharmMLElement getDistribution(Object context) { //TODO PRobOnto

        if (context instanceof ParameterRandomVariable) {
            ParameterRandomVariable prv = (ParameterRandomVariable) context;
            if (prv.getDistribution().getUncertML() != null) {
                return prv.getDistribution().getUncertML();
            } else if (prv.getDistribution().getProbOnto() != null) {
                return prv.getDistribution().getProbOnto();
            }
        } else if (context instanceof PopulationParameter) {
            PopulationParameter prv = (PopulationParameter) context;
            if (prv.getDistribution().getUncertML() != null) {
                return prv.getDistribution().getUncertML();
            } else if (prv.getDistribution().getProbOnto() != null) {
                return prv.getDistribution().getProbOnto();
            }
        }
        throw new UnsupportedOperationException("Unsupported distribution");
    }

    public static boolean isProbOnto(Distribution d) {
        return d.getProbOnto() == null ? false : true;

    }

    public static boolean isUncertML(Distribution d) {
        return d.getUncertML() == null ? false : true;

    }

    public static String sbConcat(StringBuilder[] ss) {
        StringBuilder so = new StringBuilder();
        for (StringBuilder sb : ss) {
            so.append(sb);
        }
        return so.toString();
    }

    public static String stringList(List<String> list) {
        StringBuilder so = new StringBuilder();
        boolean start = true;
        for (String s : list) {
            if (!start) {
                so.append(", ");
            }
            so.append(s);
            start = false;
        }
        return so.toString();
    }

    public static List<String> getList(List<SymbolRef> list) {
        List<String> sList = new ArrayList<>();
        for (SymbolRef sr : list) {
            sList.add(sr.getSymbIdRef());
        }
        return sList;
    }

    public static String getFileName(String name) {
        int idx = name.lastIndexOf("/");
        name = idx >= 0 ? name.substring(idx + 1) : name;
        idx = name.lastIndexOf("\\");
        name = idx >= 0 ? name.substring(idx + 1) : name;
        name = name.substring(0, name.indexOf("."));
        return name;
    }

    public static String getFileName(File src) {
        String name = src.getPath();
        int idx = name.lastIndexOf("/");
        name = idx >= 0 ? name.substring(idx + 1) : name;
        idx = name.lastIndexOf("\\");
        name = idx >= 0 ? name.substring(idx + 1) : name;
        name = name.substring(0, name.indexOf("."));
        return name;
    }

    public static List<String> doNormalProbOntoPriors() {
        List<String> dlines = new ArrayList<>();
        List<String> plines = new ArrayList<>();

        return mergeStrings(dlines, plines);
    }

    public static List<String> mergeStrings(List<String> get, List<String> get0) {
        get.addAll(get0);
        return getUniqueString(get);
    }

    public static int getCorrVarIndex(String var, List<String> vars) {
        for (int i = 0; i < vars.size(); i++) {
            if (var.equals(vars.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public static String clean(String in) {
        return in.replaceAll(delimiter, "");
    }

    public static List<String> clean(List<String> in) {
        List<String> out = new ArrayList<>();
        for (String s : in) {
            if (s.trim().length() > 0) {
                out.add(Util.clean(s));
            }
        }
        return out;
    }

    public static String getSymbId(PharmMLRootType el) {
        if (el instanceof CommonVariableDefinition) {
            return ((CommonVariableDefinition) el).getSymbId();
        } else if (el instanceof CommonParameter) {
            return ((CommonParameter) el).getSymbId();
        }

        return "getSymbId - Unknown Id";
    }

    public static List<String> getPriorValueNew(DistributionParameter par, ILexer lexer) {
        List<String> priors = new ArrayList<>();

        String tmp = "";
        if (par == null) {
            return null;
        }
        Rhs ass = par.getAssign();
        if (ass.getSymbRef() != null) {
            priors.add(ass.getSymbRef().getSymbIdRef());
            if (lexer.getTreeMaker().newInstance(par) != null) {
                priors.add("");
            }
        } else if (ass.getScalar() != null) {
            priors.add(ass.getScalar().valueToString());
        }
        return priors;
    }

    public static String getPriorValueNew(PopulationParameter par) {
        if (par == null) {
            return null;
        }
        String tmp = "";
        Rhs ass = par.getAssign();
        if (ass.getSymbRef() != null) {
            tmp = ass.getSymbRef().getSymbIdRef();
        } else if (ass.getScalar() != null) {
            tmp = ass.getScalar().valueToString();
        }
        return tmp;
    }

    public static Rhs getPriorValue(Object o) {
        Rhs ret;
        if (o == null) {
            return null;
        }
        if (isCovariateTransform(o)) {
            ret = ((CovariateTransformation) o).getAssign();
        } else if (isDerivative(o)) {
            ret = ((DerivativeVariable) o).getAssign();
        } else if (isIndividualParameter(o)) {
            ret = ((IndividualParameter) o).getAssign();
        } else if (isPopulationParameter(o)) {
            ret = ((PopulationParameter) o).getAssign();
        } else if (isLocalVariable(o)) {
            ret = ((VariableDefinition) o).getAssign();
        } else if (isDistributionParameter(o)) {
            ret = ((DistributionParameter) o).getAssign();
        } else {
            throw new UnsupportedOperationException("prior value not available for " + o.getClass() + " datatype");

        }
        return ret;
    }

    public static List<String> getObjectsList(Document doc, String oName) {
        List<String> list = new ArrayList<>();
        NodeList nList = doc.getElementsByTagName(oName);
        for (int i = 0; i < nList.getLength(); i++) {
            for (int temp = 0; temp < nList.getLength(); temp++) {
                org.w3c.dom.Node nNode = nList.item(temp);
                if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    NodeList children = eElement.getChildNodes();
                    org.w3c.dom.Node child;
                    if (children.getLength() > 0) {
                        child = eElement.getFirstChild();
                        if (child != null && child.getNextSibling() != null) {
                            if (child.getNextSibling().getNodeName().equals("ct:Assign")) {
                                if (!list.contains(eElement.getAttribute("symbId")) && eElement.getAttribute("symbId").trim().length() > 0) {
                                    list.add(eElement.getAttribute("symbId"));
                                }
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

 public static List<String> getObjectsListByType(Document doc, String oName) {
        List<String> list = new ArrayList<>();
        NodeList nList = doc.getElementsByTagName(oName);
        for (int i = 0; i < nList.getLength(); i++) {
            for (int temp = 0; temp < nList.getLength(); temp++) {
                org.w3c.dom.Node nNode = nList.item(temp);
                if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    NodeList children = eElement.getChildNodes();
                    org.w3c.dom.Node child;
                    if (children.getLength() > 0) {
                        child = eElement.getFirstChild();
                        if (child != null && child.getNextSibling() != null) {
                                    list.add(eElement.getAttribute("symbId"));
                        }
                    }
                }
            }
        }
        return list;
    }

   protected static List<SymbolRef> getUniqueSymbolRef(List<SymbolRef> list0) {
        List<SymbolRef> list = new ArrayList<>();
        for (SymbolRef s : list0) {
            if (!isInList(list, s)) {
                list.add(s);
            }
        }
        return list;
    }

    protected static List<String> getUniqueString(List<String> list0) {
        List<String> list = new ArrayList<>();
        for (String s : list0) {
            s = s.trim();
        }
        for (String s : list0) {
            if (s.length() > 0 && !isInList(list, s)) {
                list.add(s);
            }
        }
        return list;
    }

    public static String createList(String[] ss, String sep) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (String s : ss) {
            if (first) {
                first = false;
                sb.append(s);
            } else {
                sb.append(sep).append(s);
            }
        }
        return sb.toString();
    }
    
    public static String createList(List<String> ss, String sep) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (String s : ss) {
            if (first) {
                first = false;
                sb.append(s);
            } else {
                sb.append(sep).append(s);
            }
        }
        return sb.toString();
    }
}
