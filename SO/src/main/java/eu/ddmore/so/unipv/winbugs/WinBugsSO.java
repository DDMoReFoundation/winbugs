/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.so.unipv.winbugs;

import eu.ddmore.so.unipv.winbugs.parts.SOGenerator;
import eu.ddmore.so.unipv.winbugs.parts.Util;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author cristiana larizza UNIPV
 */



public class WinBugsSO {

    public static void main(String[] args) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
        String ioPath;
        if (args.length == 1) {
            ioPath = args[0];
        } else {
            System.out.println("Missing parameter");
            return;
        }

        System.out.println("SO generation START");
        System.out.println("inPath = " + ioPath);
        CodaLoader sg = new CodaLoader(ioPath);
        List<String> popVars = Util.getObjectsListByType(ioPath+"/"+sg.getModel(), "PopulationParameter");
        sg.setPopList(popVars);
        sg.loadData();

        SOGenerator tso = new SOGenerator("winbugs-SO", sg);
        tso.createSOBlock("blk1");
        tso.createRawResults("output");
        tso.createTaskInformation("success");
        tso.createEstimation();
        tso.createPopulationEstimates(null);
        tso.createPrecisionPopulationEstimates(ioPath + "/" + "popEstimates.csv");
        if (!sg.getIndivMap().isEmpty()) {
            tso.createIndividualEstimates(); 
            tso.createPrecisionIndividualEstimates(ioPath + "/" + "indivEstimates.csv"); 
        }
        String outXml = ioPath + "/" + sg.getModel() + ".SO.xml";
        tso.save(outXml);
        System.out.println("output saved in file  = " + outXml);
        System.out.println("tempo di esecuzione " + (System.currentTimeMillis() - sg.getTime()) / 6000 + " sec");
        System.out.println("SO generation END");
    }
}