/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.so.unipv.winbugs.parts;

import eu.ddmore.libpharmml.dom.commontypes.AnnotationType;
import eu.ddmore.libpharmml.dom.dataset.DataSet;
import eu.ddmore.libpharmml.dom.dataset.ExternalFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.math3.stat.StatUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author cristiana larizza UNIPV
 */
public class Util {

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

    public static String createList(boolean id, String[] ss, String sep) {
        if (id) {
            String[] sss;
            sss = new String[ss.length + 1];
            sss[0] = "ID";

            int k = 1;
            for (String s : ss) {
                sss[k++] = s;
            }
            return createList(sss, sep);
        } else {
            return createList(ss, sep);
        }
    }

    public static DataSet addRows(List<String[]> vals, DataSet ds) {
        for (String[] v : vals) {
            ds.createRow(v);
        }
        return ds;
    }

    public static List<String[]> getVals(Map<String, Parameter> pars, int nRows) {
        List<String[]> vals = new ArrayList<>();
        String[] val;
        int k;
        for (int i = 0; i < nRows; i++) {
            k = 0;
            val = new String[pars.size()];
            for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
                val[k++] = ("" + outPar.getValue().getArrayValues()[i]);
            }
            vals.add(val);
        }
        return vals;
    }

    public static List<String[]> getMedian(Map<String, Parameter> pars) {
        List<String[]> vals = new ArrayList<>();
        String[] val;
        int k = 0;
        val = new String[pars.size()];
        for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
            val[k++] = ("" + StatUtils.percentile(outPar.getValue().getArrayValues(), 50));
        }
        vals.add(val);

        return vals;
    }

    public static List<String[]> getMean(Map<String, Parameter> pars) {
        List<String[]> vals = new ArrayList<>();
        String[] val;
        int k = 0;
        val = new String[pars.size()];
        for (Map.Entry<String, Parameter> outPar : pars.entrySet()) {
            val[k++] = ("" + StatUtils.mean(outPar.getValue().getArrayValues()));
        }
        vals.add(val);

        return vals;
    }

    public static boolean isInList(List<String> list, String s) {
        for (String s0 : list) {
            if (s0.equals(s)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInList(String[] list, String s) {
        for (String s0 : list) {
            if (s0.equals(s)) {
                return true;
            }
        }
        return false;
    }

    public static ExternalFile createEF(String path, String id, String annotation) {
        ExternalFile ef = createEF(path, id);
        AnnotationType at = new AnnotationType();
        at.setValue(annotation);
        ef.setDescription(at);
        return ef;
    }

    public static ExternalFile createEF(String path, String id) {
        ExternalFile ef = new ExternalFile();
        ef.setPath(path);
        ef.setOid(id);
        return ef;
    }

    public static String getName(String n) {
        if (!n.contains("[")) {
            return n;
        } else {
            return n.substring(0, n.indexOf("["));
        }
    }

    public static String getSimpleFileName(String name) {
        int idx = name.lastIndexOf("/");
        name = idx >= 0 ? name.substring(idx + 1) : name;
        return name;
    }

    public static ExternalFile createEF(String path, String id, ExternalFile.Delimiter delim) {
        ExternalFile ef = new ExternalFile();
        ef.setPath(path);
        ef.setOid(id);
        ef.setDelimiter(delim);
        return ef;
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

    public static List<String> getObjectsListByType(String nameFile, String oName) throws SAXException, ParserConfigurationException, IOException {
        List<String> list = new ArrayList<>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(nameFile + ".xml");

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
        return getUniqueString(list);
    }

    protected static String removeIndexes(String in) {
        String out = in;
        if (!out.contains("[")) {
            return out;
        }
        out = out.substring(0, out.indexOf("["));;
        return out;
    }

}
