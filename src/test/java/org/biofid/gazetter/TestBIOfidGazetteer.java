package org.biofid.gazetter;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
import org.junit.Test;
import org.texttechnologylab.annotation.type.Taxon;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;

public class TestBIOfidGazetteer {

    @Test
    public void testCharGazetteer() {
        try {
            String sourceLocation = "src/test/resources/taxa.zip";
//			String sourceLocation = "https://www.texttechnologylab.org/files/BIOfidTaxa.zip";
            final AnalysisEngine gazetterEngine = AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(
                    BIOfidTreeGazetteer.class,
                    BIOfidTreeGazetteer.PARAM_SOURCE_LOCATION, sourceLocation,
                    BIOfidTreeGazetteer.PARAM_USE_LOWERCASE, false));

            File file = new File("src/test/resources/9031034.xmi");
            {
                JCas jCas = JCasFactory.createJCas();
                CasIOUtils.load(java.nio.file.Files.newInputStream(file.toPath()), null, jCas.getCas(), true);
                jCas.removeAllIncludingSubtypes(Taxon.type);

                SimplePipeline.runPipeline(jCas, gazetterEngine);
                XmiCasSerializer.serialize(jCas.getCas(), new FileOutputStream(new File("/tmp/temp.xmi")));

                System.out.println(JCasUtil.select(jCas, Taxon.class).stream().map(taxon -> String.format("%s@(%d, %d): %s", taxon.getCoveredText(), taxon.getBegin(), taxon.getEnd(), taxon.getValue())).collect(Collectors.joining("\n")));
            }
        } catch (UIMAException | IOException | SAXException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testStringGazetteer() {
        try {
            String sourceLocation = "src/test/resources/taxa.zip";
//			String sourceLocation = "https://www.texttechnologylab.org/files/BIOfidTaxa.zip";
            final AnalysisEngine gazetterEngine = AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(
                    BIOfidTreeGazetteer.class,
                    BIOfidTreeGazetteer.PARAM_SOURCE_LOCATION, sourceLocation,
                    BIOfidTreeGazetteer.PARAM_USE_LOWERCASE, false,
                    BIOfidTreeGazetteer.PARAM_USE_STRING_TREE, true));

            File file = new File("src/test/resources/9031034.xmi");
            {
                JCas jCas = JCasFactory.createJCas();
                CasIOUtils.load(java.nio.file.Files.newInputStream(file.toPath()), null, jCas.getCas(), true);
                jCas.removeAllIncludingSubtypes(Taxon.type);

                SimplePipeline.runPipeline(jCas, gazetterEngine);
                XmiCasSerializer.serialize(jCas.getCas(), new FileOutputStream(new File("/tmp/temp.xmi")));

                System.out.println(JCasUtil.select(jCas, Taxon.class).stream().map(taxon -> String.format("%s@(%d, %d): %s", taxon.getCoveredText(), taxon.getBegin(), taxon.getEnd(), taxon.getValue())).collect(Collectors.joining("\n")));
            }
        } catch (UIMAException | IOException | SAXException e) {
            e.printStackTrace();
        }
    }

}
