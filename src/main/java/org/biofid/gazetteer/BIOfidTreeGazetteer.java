package org.biofid.gazetteer;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.biofid.gazetteer.models.CharTreeGazetteerModel;
import org.biofid.gazetteer.models.ITreeGazetteerModel;
import org.biofid.gazetteer.models.SkipGramGazetteerModel;
import org.biofid.gazetteer.models.StringTreeGazetteerModel;
import org.biofid.gazetteer.search.ITreeNode;
import org.dkpro.core.api.parameter.ComponentParameters;
import org.dkpro.core.api.resources.MappingProvider;
import org.dkpro.core.api.segmentation.SegmenterBase;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
	protected String language;
	
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
	protected Boolean pUseLowercase;
	
	/**
	 * Boolean, if true get all m-skip-n-grams for which n > 2 holds, not just 1-skip-(n-1)-grams.
	 */
	public static final String PARAM_GET_ALL_SKIPS = "pGetAllSkips";
	@ConfigurationParameter(name = PARAM_GET_ALL_SKIPS, mandatory = false, defaultValue = "false")
	protected Boolean pGetAllSkips;
	
	/**
	 * Boolean, if not false, split taxa on spaces and hyphens too.
	 */
	public static final String PARAM_SPLIT_HYPEN = "pSplitHyphen";
	@ConfigurationParameter(name = PARAM_SPLIT_HYPEN, mandatory = false, defaultValue = "true")
	protected Boolean pSplitHyphen;
	
	/**
	 * Boolean, if true, use StringTree implementation. Default: true.
	 */
	public static final String PARAM_USE_STRING_TREE = "pUseStringTree";
	@ConfigurationParameter(name = PARAM_USE_STRING_TREE, mandatory = false, defaultValue = "true")
	protected Boolean pUseStringTree;
	
	/**
	 * {@link Type} name (fully qualified class name) of the class to tag. Must subclass {@link NamedEntity}.
	 */
	public static final String PARAM_TAGGING_TYPE_NAME = "pTaggingTypeName";
	@ConfigurationParameter(
			name = PARAM_TAGGING_TYPE_NAME
	)
	private String pTaggingTypeName;
	
	/**
	 * If true, use {@link Lemma Lemmata} instead of {@link Token forms} for tagging. Default: true.
	 */
	public static final String PARAM_USE_LEMMATA = "pUseLemmata";
	@ConfigurationParameter(name = PARAM_USE_LEMMATA, mandatory = false, defaultValue = "true")
	private Boolean pUseLemmata;
	
	/**
	 * The pattern for the next-word-search after passing a single token/charater
	 */
	public static final String PARAM_NEXT_WORD_PATTERN = "pNextWordPattern";
	@ConfigurationParameter(
			name = PARAM_NEXT_WORD_PATTERN,
			defaultValue = "[ (){}\\[\\],.!;:\\-_]+.*"
	)
	private String pNextWordPattern;
	private Pattern nextWordPattern;
	
	/**
	 * Boolean, if true, run tagging over {@link Sentence Sentences} instead of the entire document text, which is run
	 * in parallel and should be faster. Do not use this if there are a a lot of abbreviations in the text that might
	 * interfere with correct end-of-sentence tagging.
	 * <p>
	 * Default: false.
	 */
	public static final String PARAM_USE_SENTECE_LEVEL_TAGGING = "pUseSentenceLevelTagging";
	@ConfigurationParameter(name = PARAM_USE_SENTECE_LEVEL_TAGGING, mandatory = false, defaultValue = "false")
	private boolean pUseSentenceLevelTagging;
	
	public static final String PARAM_ADD_ABBREVIATED_TAXA = "pAddAbbreviatedTaxa";
	@ConfigurationParameter(name = PARAM_ADD_ABBREVIATED_TAXA, mandatory = false, defaultValue = "true")
	private boolean pAddAbbreviatedTaxa;
	
	MappingProvider namedEntityMappingProvider;
	
	final AtomicInteger atomicTaxonMatchCount = new AtomicInteger(0);
	SkipGramGazetteerModel skipGramGazetteerModel;
	private ArrayList<Annotation> tokens;
	private HashMap<Integer, Integer> tokenBeginIndex;
	private HashMap<Integer, Integer> tokenEndIndex;
	private Type taggingType;
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		namedEntityMappingProvider = new MappingProvider();
		namedEntityMappingProvider.setDefault(MappingProvider.LOCATION, "classpath:/org/hucompute/textimager/biofid/lib/ner-default.map");
		namedEntityMappingProvider.setDefault(MappingProvider.BASE_TYPE, NamedEntity.class.getName());
		namedEntityMappingProvider.setOverride(MappingProvider.LANGUAGE, language);
		
		nextWordPattern = Pattern.compile(pNextWordPattern, Pattern.UNICODE_CHARACTER_CLASS);
		
		try {
			if (!pUseStringTree) {
				getLogger().info(String.format("Initializing CharTreeGazetteerModel for %s", Class.forName(pTaggingTypeName).getSimpleName()));
				skipGramGazetteerModel = new CharTreeGazetteerModel(sourceLocation, pUseLowercase, language, pMinLength, pGetAllSkips, pSplitHyphen, pAddAbbreviatedTaxa);
			} else {
				getLogger().info(String.format("Initializing StringTreeGazetteerModel for %s", Class.forName(pTaggingTypeName).getSimpleName()));
				skipGramGazetteerModel = new StringTreeGazetteerModel(sourceLocation, pUseLowercase, language, pMinLength, pGetAllSkips, pSplitHyphen, pAddAbbreviatedTaxa);
			}
		} catch (IOException | ClassNotFoundException e) {
			throw new ResourceInitializationException(e);
		}
	}
	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		process(aJCas, aJCas.getDocumentText(), 0);
	}
	
	@Override
	protected void process(JCas aJCas, String text, int zoneBegin) throws AnalysisEngineProcessException {
		namedEntityMappingProvider.configure(aJCas.getCas());
		taggingType = aJCas.getTypeSystem().getType(pTaggingTypeName);
		getLogger().debug(String.format("Tagging %s", taggingType.getShortName()));
		
		if (aJCas.getDocumentText().trim().length() == 0)
			return;
		
		if (!pUseLemmata)
			tokens = Lists.newArrayList(JCasUtil.select(aJCas, Token.class));
		else
			tokens = Lists.newArrayList(JCasUtil.select(aJCas, Lemma.class));
		
		tokenBeginIndex = new HashMap<>();
		tokenEndIndex = new HashMap<>();
		for (int i = 0; i < tokens.size(); i++) {
			Annotation token = tokens.get(i);
			tokenBeginIndex.put(token.getBegin(), i);
			tokenEndIndex.put(token.getEnd(), i);
		}
		
		tagAllMatches(aJCas);
	}
	
	private static class Match {
		
		final int start;
		final int end;
		final String value;
		
		public Match(int start, int end, String value) {
			this.start = start;
			this.end = end;
			this.value = value;
		}
	}
	
	/**
	 * Find and tag all occurrences of the given taxon skip gram in the
	 *
	 * @param aJCas
	 */
	private void tagAllMatches(JCas aJCas) {
		Collection<Sentence> sentences = JCasUtil.select(aJCas, Sentence.class);
		if (!pUseSentenceLevelTagging || sentences.isEmpty()) {
			getLogger().debug(
					String.format("%s, tagging entire document text.",
							pUseSentenceLevelTagging ? "PARAM_FORCE_DOCUMENT_TEXT_TAGGING=true" : "Found no sentences")
			);
			tagEntireText(aJCas);
		} else {
			int sentencesLength = sentences.stream().map(Sentence::getCoveredText).collect(Collectors.joining(" ")).length();
			getLogger().debug(String.format("Tagging sentences. Coverage: %d/%d", sentencesLength, aJCas.getDocumentText().length()));
			tagSentences(aJCas, sentences);
		}
	}
	
	private void tagEntireText(JCas aJCas) {
		String query = aJCas.getDocumentText();
		query = pUseLowercase ? query.toLowerCase() : query;
		ITreeNode root = ((ITreeGazetteerModel) skipGramGazetteerModel).getTree();
		
		findAllMatches(root, query, 0).forEach(m -> addAnnotation(aJCas, m));
	}
	
	private void tagSentences(JCas aJCas, Collection<Sentence> sentences) {
		final ITreeNode root = ((ITreeGazetteerModel) skipGramGazetteerModel).getTree();
		
		sentences.stream()
				.flatMap(sentence -> {
					String query = sentence.getCoveredText();
					query = pUseLowercase ? query.toLowerCase() : query;
					return findAllMatches(root, query, sentence.getBegin()).stream();
				})
				.forEach(m -> addAnnotation(aJCas, m));
	}
	
	private ArrayList<Match> findAllMatches(ITreeNode root, final String query, int globalOffset) {
		Matcher matcher = nextWordPattern.matcher(query);
		ArrayList<Match> matches = new ArrayList<>();
		int offset = 0;
		do {
			String substring = query.substring(offset);
			String matchedString = root.traverse(substring);
			if (!Strings.isNullOrEmpty(matchedString)) {
				int start = offset + globalOffset;
				int end = offset + matchedString.length() + globalOffset;
				// TODO: implement parameter which allows to ignore the token end rule
				if (tokenBeginIndex.containsKey(start) && tokenEndIndex.containsKey(end)) {
					matches.add(new Match(start, end, matchedString));
				}
				offset += matchedString.length();
			}
			if (matcher.find(offset)) {
				offset = matcher.start() + 1;
			} else {
				offset = -1;
			}
		} while (offset < query.length() && offset > -1);
		return matches;
	}
	
	
	private void addAnnotation(JCas aJCas, Match match) {
		try {
			Annotation fromToken = tokens.get(tokenBeginIndex.get(match.start));
			Annotation toToken = tokens.get(tokenEndIndex.get(match.end));
			NamedEntity annotation = (NamedEntity) aJCas.getCas().createAnnotation(taggingType, fromToken.getBegin(), toToken.getEnd());
			
			String tag = skipGramGazetteerModel.getSkipGramTaxonLookup().get(match.value);
			String uris = skipGramGazetteerModel.getTaxonUriMap().get(tag).stream()
					.map(URI::toString)
					.collect(Collectors.joining(", "));
			annotation.setValue(uris);
			
			aJCas.addFsToIndexes(annotation);
		} catch (NullPointerException e) {
			// FIXME: Remove this
			System.err.println(e.getMessage());
			System.err.println(aJCas.getDocumentText().substring(match.start, match.end + 10));
			System.err.println(match.value);
			e.printStackTrace();
		}
	}
	
}
