package org.biofid.gazetteer;

import com.google.common.collect.ImmutableSet;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.impl.XmiSerializationSharedData;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.*;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
import org.dkpro.core.io.conll.Conll2003Reader;
import org.dkpro.core.io.xmi.XmiReader;
import org.dkpro.core.io.xmi.XmiWriter;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.annotation.type.Process;
import org.texttechnologylab.annotation.type.*;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TestBIOfidGazetteer {
	
	private String sourceLocation = "src/test/resources/taxa.zip";
//    private String sourceLocation = "https://www.texttechnologylab.org/files/BIOfidTaxa.zip";
	
	//    @Test
	public void testRegularGazetteer() {
		try {
			final AnalysisEngine gazetterEngine = AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(
					BIOfidGazetteer.class,
					BIOfidGazetteer.PARAM_SOURCE_LOCATION, sourceLocation,
					BIOfidGazetteer.PARAM_USE_LOWERCASE, false));
			
			runTest(gazetterEngine);
		} catch (UIMAException e) {
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void testCharGazetteer() {
		try {
			final AnalysisEngine gazetterEngine = AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(
					BIOfidTreeGazetteer.class,
					BIOfidTreeGazetteer.PARAM_SOURCE_LOCATION, sourceLocation,
					BIOfidTreeGazetteer.PARAM_TAGGING_TYPE_NAME, Taxon.class.getName(),
					BIOfidTreeGazetteer.PARAM_USE_LOWERCASE, false,
					BIOfidTreeGazetteer.PARAM_USE_STRING_TREE, false,
					BIOfidTreeGazetteer.PARAM_USE_SENTECE_LEVEL_TAGGING, true
			));
			
			runTest(gazetterEngine);
		} catch (UIMAException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testCharGazetteerDocumentLevel() {
		try {
			final AnalysisEngine gazetterEngine = AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(
					BIOfidTreeGazetteer.class,
					BIOfidTreeGazetteer.PARAM_SOURCE_LOCATION, sourceLocation,
					BIOfidTreeGazetteer.PARAM_TAGGING_TYPE_NAME, Taxon.class.getName(),
					BIOfidTreeGazetteer.PARAM_USE_LOWERCASE, false,
					BIOfidTreeGazetteer.PARAM_USE_STRING_TREE, false
			));
			
			runTest(gazetterEngine);
		} catch (UIMAException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testStringGazetteer() {
		try {
			final AnalysisEngine gazetterEngine = AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(
					BIOfidTreeGazetteer.class,
					BIOfidTreeGazetteer.PARAM_SOURCE_LOCATION, sourceLocation,
					BIOfidTreeGazetteer.PARAM_TAGGING_TYPE_NAME, Taxon.class.getName(),
					BIOfidTreeGazetteer.PARAM_USE_LOWERCASE, true,
					BIOfidTreeGazetteer.PARAM_USE_STRING_TREE, true,
					BIOfidTreeGazetteer.PARAM_USE_SENTECE_LEVEL_TAGGING, true
			));
			
			runTest(gazetterEngine);
		} catch (UIMAException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testStringGazetteerDocumentLevel() {
		try {
			final AnalysisEngine gazetterEngine = AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(
					BIOfidTreeGazetteer.class,
					BIOfidTreeGazetteer.PARAM_SOURCE_LOCATION, sourceLocation,
					BIOfidTreeGazetteer.PARAM_TAGGING_TYPE_NAME, Taxon.class.getName(),
					BIOfidTreeGazetteer.PARAM_USE_LOWERCASE, true,
					BIOfidTreeGazetteer.PARAM_USE_STRING_TREE, true
			));
			
			runTest(gazetterEngine);
		} catch (UIMAException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testBIOfid() {
//		try {
//			final CollectionReader reader = CollectionReaderFactory.createReader(XmiReader.class,
//					XmiReader.PARAM_SOURCE_LOCATION, "src/test/resources/out/plain/",
//					Conll2003Reader.PARAM_PATTERNS, "[+]**.xmi"
//			);
//			AggregateBuilder aggregateBuilder = new AggregateBuilder();
//			for (ImmutablePair<String, String> pair : Arrays.asList(
//					ImmutablePair.of(Animal_Fauna.class.getSimpleName(), Animal_Fauna.class.getName()),
//					ImmutablePair.of(Archaea.class.getSimpleName(), Archaea.class.getName()),
//					ImmutablePair.of(Bacteria.class.getSimpleName(), Bacteria.class.getName()),
//					ImmutablePair.of(Chromista.class.getSimpleName(), Chromista.class.getName()),
//					ImmutablePair.of(Feeling_Emotion.class.getSimpleName(), Feeling_Emotion.class.getName()),
//					ImmutablePair.of(Food.class.getSimpleName(), Food.class.getName()),
//					ImmutablePair.of(Fungi.class.getSimpleName(), Fungi.class.getName()),
//					ImmutablePair.of(Habitat.class.getSimpleName(), Habitat.class.getName()),
//					ImmutablePair.of(Lichen.class.getSimpleName(), Lichen.class.getName()),
//					ImmutablePair.of(NaturalPhenomenon.class.getSimpleName(), NaturalPhenomenon.class.getName()),
//					ImmutablePair.of(Plant_Flora.class.getSimpleName(), Plant_Flora.class.getName()),
//					ImmutablePair.of(Protozoa.class.getSimpleName(), Protozoa.class.getName()),
//					ImmutablePair.of(Quantity_Amount.class.getSimpleName(), Quantity_Amount.class.getName()),
//					ImmutablePair.of(Reproduction.class.getSimpleName(), Reproduction.class.getName()),
//					ImmutablePair.of(Shape.class.getSimpleName(), Shape.class.getName()),
//					ImmutablePair.of(Substance.class.getSimpleName(), Substance.class.getName()),
//					ImmutablePair.of(Viruses.class.getSimpleName(), Viruses.class.getName())
//			)) {
//				aggregateBuilder.add(
//						String.format("BIOfidTreeGazetteer[%s]", pair.left),
//						AnalysisEngineFactory.createEngineDescription(
//								BIOfidTreeGazetteer.class,
//								BIOfidTreeGazetteer.PARAM_SOURCE_LOCATION, "src/test/resources/" + pair.left + ".list",
//								BIOfidTreeGazetteer.PARAM_TAGGING_TYPE_NAME, pair.right,
//								BIOfidTreeGazetteer.PARAM_USE_LOWERCASE, true,
//								BIOfidTreeGazetteer.PARAM_USE_STRING_TREE, true,
//								BIOfidTreeGazetteer.PARAM_USE_SENTECE_LEVEL_TAGGING, true
//						));
//			}
//			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(XmiWriter.class,
//					XmiWriter.PARAM_TARGET_LOCATION, "src/test/resources/out/annotated/",
//					XmiWriter.PARAM_USE_DOCUMENT_ID, true,
//					XmiWriter.PARAM_PRETTY_PRINT, true,
//					XmiWriter.PARAM_OVERWRITE, true
//			));
//			SimplePipeline.runPipeline(reader, aggregateBuilder.createAggregate());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		try {
//			final CollectionReader reader = CollectionReaderFactory.createReader(XmiReader.class,
//					XmiReader.PARAM_SOURCE_LOCATION, "src/test/resources/out/annotated/",
//					Conll2003Reader.PARAM_PATTERNS, "[+]**.xmi"
//			);
//			AggregateBuilder aggregateBuilder = new AggregateBuilder();
//			for (ImmutablePair<String, String> pair : Arrays.asList(
//					ImmutablePair.of(Attribute_Property.class.getSimpleName(), Attribute_Property.class.getName()),
//					ImmutablePair.of(Body_Corpus.class.getSimpleName(), Body_Corpus.class.getName()),
//					ImmutablePair.of(Cognition_Ideation.class.getSimpleName(), Cognition_Ideation.class.getName()),
//					ImmutablePair.of(Morphology.class.getSimpleName(), Morphology.class.getName()),
//					ImmutablePair.of(Motive.class.getSimpleName(), Motive.class.getName())
//			)) {
//				aggregateBuilder.add(
//						String.format("BIOfidTreeGazetteer[%s]", pair.left),
//						AnalysisEngineFactory.createEngineDescription(
//								BIOfidTreeGazetteer.class,
//								BIOfidTreeGazetteer.PARAM_SOURCE_LOCATION, "src/test/resources/" + pair.left + ".list",
//								BIOfidTreeGazetteer.PARAM_TAGGING_TYPE_NAME, pair.right,
//								BIOfidTreeGazetteer.PARAM_USE_LOWERCASE, true,
//								BIOfidTreeGazetteer.PARAM_USE_STRING_TREE, true,
//								BIOfidTreeGazetteer.PARAM_USE_SENTECE_LEVEL_TAGGING, true
//						));
//			}
//			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(XmiWriter.class,
//					XmiWriter.PARAM_TARGET_LOCATION, "src/test/resources/out/annotated/",
//					XmiWriter.PARAM_USE_DOCUMENT_ID, true,
//					XmiWriter.PARAM_PRETTY_PRINT, true,
//					XmiWriter.PARAM_OVERWRITE, true
//			));
//			SimplePipeline.runPipeline(reader, aggregateBuilder.createAggregate());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		try {
//			final CollectionReader reader = CollectionReaderFactory.createReader(XmiReader.class,
//					XmiReader.PARAM_SOURCE_LOCATION, "src/test/resources/out/annotated/",
//					Conll2003Reader.PARAM_PATTERNS, "[+]**.xmi"
//			);
//			AggregateBuilder aggregateBuilder = new AggregateBuilder();
//			for (ImmutablePair<String, String> pair : Arrays.asList(
//					ImmutablePair.of(Possession_Property.class.getSimpleName(), Possession_Property.class.getName()),
//					ImmutablePair.of(Relation.class.getSimpleName(), Relation.class.getName()),
//					ImmutablePair.of(Society.class.getSimpleName(), Society.class.getName()),
//					ImmutablePair.of(State_Condition.class.getSimpleName(), State_Condition.class.getName()),
//					ImmutablePair.of(Time.class.getSimpleName(), Time.class.getName())
//			)) {
//				aggregateBuilder.add(
//						String.format("BIOfidTreeGazetteer[%s]", pair.left),
//						AnalysisEngineFactory.createEngineDescription(
//								BIOfidTreeGazetteer.class,
//								BIOfidTreeGazetteer.PARAM_SOURCE_LOCATION, "src/test/resources/" + pair.left + ".list",
//								BIOfidTreeGazetteer.PARAM_TAGGING_TYPE_NAME, pair.right,
//								BIOfidTreeGazetteer.PARAM_USE_LOWERCASE, true,
//								BIOfidTreeGazetteer.PARAM_USE_STRING_TREE, true,
//								BIOfidTreeGazetteer.PARAM_USE_SENTECE_LEVEL_TAGGING, true
//						));
//			}
//			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(XmiWriter.class,
//					XmiWriter.PARAM_TARGET_LOCATION, "src/test/resources/out/annotated/",
//					XmiWriter.PARAM_USE_DOCUMENT_ID, true,
//					XmiWriter.PARAM_PRETTY_PRINT, true,
//					XmiWriter.PARAM_OVERWRITE, true
//			));
//			SimplePipeline.runPipeline(reader, aggregateBuilder.createAggregate());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		/*
		 * Single class tagging
		 */
		try {
			for (ImmutablePair<String, String> pair : Arrays.asList(
//					ImmutablePair.of(Act_Action_Activity.class.getSimpleName(), Act_Action_Activity.class.getName()),
					ImmutablePair.of(Artifact.class.getSimpleName(), Artifact.class.getName()),
					ImmutablePair.of(Communication.class.getSimpleName(), Communication.class.getName()),
					ImmutablePair.of(Event_Happening.class.getSimpleName(), Event_Happening.class.getName()),
					ImmutablePair.of(Group_Collection.class.getSimpleName(), Group_Collection.class.getName()),
					ImmutablePair.of(Location_Place.class.getSimpleName(), Location_Place.class.getName()),
					ImmutablePair.of(NaturalObject.class.getSimpleName(), NaturalObject.class.getName()),
					ImmutablePair.of(Person_HumanBeing.class.getSimpleName(), Person_HumanBeing.class.getName()),
					ImmutablePair.of(Process.class.getSimpleName(), Process.class.getName())
			)) {
				final CollectionReader reader = CollectionReaderFactory.createReader(XmiReader.class,
						XmiReader.PARAM_SOURCE_LOCATION, "src/test/resources/out/annotated/",
						Conll2003Reader.PARAM_PATTERNS, "[+]**.xmi"
				);
				AggregateBuilder aggregateBuilder = new AggregateBuilder();
				aggregateBuilder.add(
						String.format("BIOfidTreeGazetteer[%s]", pair.left),
						AnalysisEngineFactory.createEngineDescription(
								BIOfidTreeGazetteer.class,
								BIOfidTreeGazetteer.PARAM_SOURCE_LOCATION, "src/test/resources/" + pair.left + ".list",
								BIOfidTreeGazetteer.PARAM_TAGGING_TYPE_NAME, pair.right,
								BIOfidTreeGazetteer.PARAM_USE_LOWERCASE, true,
								BIOfidTreeGazetteer.PARAM_USE_STRING_TREE, true,
								BIOfidTreeGazetteer.PARAM_USE_SENTECE_LEVEL_TAGGING, true
						));
				aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(XmiWriter.class,
						XmiWriter.PARAM_TARGET_LOCATION, "src/test/resources/out/annotated/",
						XmiWriter.PARAM_USE_DOCUMENT_ID, true,
						XmiWriter.PARAM_PRETTY_PRINT, true,
						XmiWriter.PARAM_OVERWRITE, true
				));
				SimplePipeline.runPipeline(reader, aggregateBuilder.createAggregate());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			final CollectionReader reader = CollectionReaderFactory.createReader(XmiReader.class,
					XmiReader.PARAM_SOURCE_LOCATION, "src/test/resources/out/annotated/",
					Conll2003Reader.PARAM_PATTERNS, "[+]**.xmi"
			);
			AggregateBuilder aggregateBuilder = new AggregateBuilder();
			aggregateBuilder.add(
					String.format("BIOfidTreeGazetteer[%s]", Taxon.class.getSimpleName()),
					AnalysisEngineFactory.createEngineDescription(
							BIOfidTreeGazetteer.class,
							BIOfidTreeGazetteer.PARAM_SOURCE_LOCATION, "src/test/resources/BIOfidTaxa.zip",
							BIOfidTreeGazetteer.PARAM_TAGGING_TYPE_NAME, Taxon.class.getName(),
							BIOfidTreeGazetteer.PARAM_USE_LOWERCASE, true,
							BIOfidTreeGazetteer.PARAM_USE_STRING_TREE, true,
							BIOfidTreeGazetteer.PARAM_USE_SENTECE_LEVEL_TAGGING, true
					));
			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(XmiWriter.class,
					XmiWriter.PARAM_TARGET_LOCATION, "src/test/resources/out/annotated/",
					XmiWriter.PARAM_USE_DOCUMENT_ID, true,
					XmiWriter.PARAM_PRETTY_PRINT, true,
					XmiWriter.PARAM_OVERWRITE, true
			));
			SimplePipeline.runPipeline(reader, aggregateBuilder.createAggregate());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testClean() {
		try {
			new File("src/test/resources/out/plain/").mkdirs();
			CollectionReader reader = CollectionReaderFactory.createReader(XmiReader.class,
					XmiReader.PARAM_PATTERNS, "[+]**.xmi",
					XmiReader.PARAM_SOURCE_LOCATION, "../Utilities/src/test/out/xmi/",
					XmiReader.PARAM_LENIENT, true
			);
			JCas jCas = JCasFactory.createJCas();
			while (reader.hasNext()) {
				try {
					reader.getNext(jCas.getCas());
					final JCas initialView = jCas.getView("_InitialView");
					
					JCas copy = JCasFactory.createText(initialView.getDocumentText());
					
					ImmutableSet.of(Sentence.class, Token.class, Lemma.class, POS.class).forEach(cls -> {
						JCasUtil.select(initialView, cls).forEach(
								top -> AnnotationFactory.createAnnotation(copy, top.getBegin(), top.getEnd(), cls)
						);
					});
					
					DocumentMetaData documentMetaData = DocumentMetaData.get(initialView);
					DocumentMetaData.copy(initialView, copy);
					
					File file = new File("src/test/resources/out/plain/" + documentMetaData.getDocumentId() + ".xmi");
					if (file.exists()) {
						file.delete();
					}
					XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
					XmiCasSerializer.serialize(copy.getCas(), copy.getTypeSystem(), new FileOutputStream(file), true, sharedData);
				} catch (UIMAException | SAXException e) {
					e.printStackTrace();
				}
				jCas.reset();
			}
		} catch (IOException | UIMAException e) {
			e.printStackTrace();
		}
	}
	
	private void runTest(AnalysisEngine gazetterEngine) throws UIMAException {
		for (String fname : Arrays.asList("src/test/resources/9031034.xmi", "src/test/resources/4058393.xmi")) {
			try {
				File file = new File(fname);
				{
					JCas jCas = JCasFactory.createJCas();
					CasIOUtils.load(java.nio.file.Files.newInputStream(file.toPath()), null, jCas.getCas(), true);
					jCas.removeAllIncludingSubtypes(Taxon.type);
					
					StopWatch stopWatch = StopWatch.createStarted();
					SimplePipeline.runPipeline(jCas, gazetterEngine);
					XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
					XmiCasSerializer.serialize(jCas.getCas(), jCas.getTypeSystem(), new FileOutputStream(new File("/tmp/temp.xmi")), true, sharedData);
					System.out.printf("Finished tagging in %dms.\n", stopWatch.getTime(TimeUnit.MILLISECONDS));
					
					System.out.printf("Found %d taxa.\n", JCasUtil.select(jCas, Taxon.class).size());
					System.out.println(JCasUtil.select(jCas, Taxon.class).stream().map(taxon -> String.format("%s@(%d, %d): %s", taxon.getCoveredText(), taxon.getBegin(), taxon.getEnd(), taxon.getValue())).collect(Collectors.joining("\n")));
				}
			} catch (IOException | SAXException e) {
				e.printStackTrace();
			}
		}
	}
	
}
