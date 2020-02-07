package org.biofid.gazetter;

import com.google.common.collect.Lists;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.resources.MappingProvider;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.SegmenterBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.texttechnologylab.annotation.type.Taxon;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * UIMA Engine for tagging taxa from taxonomic lists or gazetteers as resource.
 */
public class BIOfidTreeGazetteer extends SegmenterBase {
	
	/**
	 * Text and model language. Default is "de".
	 */
	public static final String PARAM_LANGUAGE = ComponentParameters.PARAM_LANGUAGE;
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = false, defaultValue = "de")
	protected static String language;
	
	/**
	 * Location from which the taxon data is read.
	 */
	public static final String PARAM_SOURCE_LOCATION = ComponentParameters.PARAM_SOURCE_LOCATION;
	@ConfigurationParameter(name = PARAM_SOURCE_LOCATION, mandatory = false, defaultValue = "https://www.texttechnologylab.org/files/BIOfidTaxa.zip")
	protected String[] sourceLocation;
	
	/**
	 * Minimum skip-gram string length
	 */
	public static final String PARAM_MIN_LENGTH = "pMinLength";
	@ConfigurationParameter(name = PARAM_MIN_LENGTH, mandatory = false, defaultValue = "5")
	protected Integer pMinLength;
	
	/**
	 * Boolean, if true use lower case.
	 */
	public static final String PARAM_USE_LOWERCASE = "pUseLowercase";
	@ConfigurationParameter(name = PARAM_USE_LOWERCASE, mandatory = false, defaultValue = "false")
	protected static Boolean pUseLowercase;
	
	/**
	 * Boolean, if true get all m-skip-n-grams for which n > 2 holds, not just 1-skip-(n-1)-grams.
	 */
	public static final String PARAM_GET_ALL_SKIPS = "pGetAllSkips";
	@ConfigurationParameter(name = PARAM_GET_ALL_SKIPS, mandatory = false, defaultValue = "false")
	protected static Boolean pGetAllSkips;
	
	/**
	 * Boolean, if not false, split taxa on spaces and hyphens too.
	 */
	public static final String PARAM_SPLIT_HYPEN = "pSplitHyphen";
	@ConfigurationParameter(name = PARAM_SPLIT_HYPEN, mandatory = false, defaultValue = "true")
	protected static Boolean pSplitHyphen;
	
	MappingProvider namedEntityMappingProvider;
	
	final AtomicInteger atomicTaxonMatchCount = new AtomicInteger(0);
	SkipGramGazetteerModel skipGramGazetteerModel;
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		namedEntityMappingProvider = new MappingProvider();
		namedEntityMappingProvider.setDefault(MappingProvider.LOCATION, "classpath:/org/hucompute/textimager/biofid/lib/ner-default.map");
		namedEntityMappingProvider.setDefault(MappingProvider.BASE_TYPE, NamedEntity.class.getName());
		namedEntityMappingProvider.setOverride(MappingProvider.LANGUAGE, "de");
		
		try {
			skipGramGazetteerModel = new TreeGazetteerModel(sourceLocation, pUseLowercase, language, pMinLength, pGetAllSkips, pSplitHyphen);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		process(aJCas, aJCas.getDocumentText(), 0);
	}
	
	@Override
	protected void process(JCas aJCas, String text, int zoneBegin) throws AnalysisEngineProcessException {
		namedEntityMappingProvider.configure(aJCas.getCas());
		
		atomicTaxonMatchCount.set(0);
		ArrayList<Token> tokens = Lists.newArrayList(JCasUtil.select(aJCas, Token.class));
		
		tagAllMatches(aJCas, tokens);
	}
	
	/**
	 * Find and tag all occurrences of the given taxon skip gram in the
	 *
	 * @param aJCas
	 * @param tokens
	 */
	private void tagAllMatches(JCas aJCas, final ArrayList<Token> tokens) {
		HashMap<Integer, Integer> tokenBeginIndex = new HashMap<>();
		HashMap<Integer, Integer> tokenEndIndex = new HashMap<>();
		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			tokenBeginIndex.put(token.getBegin(), i);
			tokenEndIndex.put(token.getEnd(), i);
		}
		
		String query = aJCas.getDocumentText();
		query = pUseLowercase ? query.toLowerCase() : query;
		StringTree stringTree = ((TreeGazetteerModel) skipGramGazetteerModel).stringTree;
		int offset = 0;
		int pos = 0;
		StringTreeNode node = stringTree.root;
		do {
			String substring = query.substring(offset);
			char c = substring.charAt(pos);
			node = node.children.get(c);
			if (node == null) {
				offset++;
				pos = 0;
				node = stringTree.root;
			} else if (node.isLeaf()) {
				pos++;
				boolean b = substring.substring(pos).startsWith(node.substring);
				if (b) {
					int fullLength = pos + node.substring.length();
//					System.out.println(String.format("\"%s':{\"start\": %d, \"end\": %d, \"match_length\": %d, \"full_length\":%d, \"taxon\":\"%s\"}",
//							query.subSequence(offset, offset + fullLength), offset, offset + fullLength, pos, fullLength, node.taxon));
					
					Token fromToken = tokens.get(tokenBeginIndex.get(offset));
					Token toToken = tokens.get(tokenBeginIndex.get(offset + fullLength));
					Taxon taxon = new Taxon(aJCas, fromToken.getBegin(), toToken.getEnd());
					
					String uris = skipGramGazetteerModel.taxonUriMap.get(node.taxon).stream()
							.map(URI::toString)
							.collect(Collectors.joining(","));
//					taxon.setValue(uris);
					aJCas.addFsToIndexes(taxon);
					
					offset += pos;
				} else {
					offset++;
				}
				pos = 0;
				node = stringTree.root;
			} else {
				pos++;
			}
		} while (offset < query.length());
	}
	
}
