package com.thoughtworks.pact.verify.api;

import com.thoughtworks.pact.verify.PactTestService;
import com.thoughtworks.pact.verify.junit.JunitReport;
import com.thoughtworks.pact.verify.junit.TestSuites;
import com.thoughtworks.pact.verify.pact.PactFile;
import com.thoughtworks.pact.verify.pact.Pacts;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.LogFactoryImpl;

import java.io.File;

/**
 * Created by xfwu on 20/07/2017.
 */
public class Pacter {

    /**
     * verify service provider by pact json files, and result report would dump to ${pactsDir}/report
     * @param pactsDir directory of pact json files
     * @param urlRoot  verify target server address
     * @return false if there is no pact json files
     */
    public static Boolean verify(File pactsDir, String urlRoot) {
        String reportDirPath = pactsDir.getAbsolutePath() + File.separator + "report";
        File reportDir = new File(reportDirPath);
        reportDir.mkdirs();
        return verify(pactsDir,urlRoot,reportDir);
    }

    /**
     * verify service provider by pact json files
     * @param pactsDir directory of pact json files
     * @param urlRoot  verify target server address
     * @param reportDir directory of junit xml report dump to
     * @return false if there is no pact json files
     */
    public static Boolean verify(File pactsDir, String urlRoot, File reportDir) {
        return verify(pactsDir,urlRoot,reportDir,null);
    }

    /**
     * verify service provider by pact json files
     * @param pactsDir directory of pact json files
     * @param urlRoot  verify target server address
     * @param reportDir directory of junit xml report dump to
     * @param logLevel  log level for developer debug trace
     * @return false if there is no pact json files
     */
    public static Boolean verify(File pactsDir, String urlRoot, File reportDir,String logLevel) {
        if(logLevel != null && logLevel.trim().length() > 0){
            LogFactory.getFactory().setAttribute(LogFactoryImpl.LOG_PROPERTY, "org.apache.commons.logging.impl.SimpleLog");
            System.setProperty("org.apache.commons.logging.simplelog.defaultlog", logLevel);
        }
        scala.collection.immutable.List<Pacts> pactsList = PactFile.loadPacts(pactsDir);
        if(pactsList.isEmpty()) {
            return false;
        }
        scala.collection.immutable.List<TestSuites> tss =  PactTestService.testPacts(pactsList, urlRoot);
        JunitReport.dumpJUnitReport(reportDir.getAbsolutePath(),tss);
        return true;
    }

}
