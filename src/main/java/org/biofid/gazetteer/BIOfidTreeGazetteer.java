package org.biofid.gazetteer;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.biofid.gazetteer.models.ITreeGazetteerModel;
import org.biofid.gazetteer.models.SkipGramGazetteerModel;
import org.biofid.gazetteer.models.StringTreeGazetteerModel;
import org.biofid.gazetteer.search.ITreeNode;
import org.dkpro.core.api.parameter.ComponentParameters;
import org.dkpro.core.api.resources.MappingProvider;
import org.dkpro.core.api.segmentation.SegmenterBase;
import org.dkpro.core.tokit.RegexSegmenter;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	public static final String PARAM_TOKEN_BOUNDARY_REGEX = "tokenBoundaryRegex";
	@ConfigurationParameter(
			name = PARAM_TOKEN_BOUNDARY_REGEX,
			defaultValue = "[^\\p{Alnum}-]+"
	)
	private String tokenBoundaryRegex;
	
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
	
	SkipGramGazetteerModel skipGramGazetteerModel;
	private ArrayList<Annotation> tokens;
	private ConcurrentHashMap<Integer, Integer> tokenBeginIndex;
	private Type taggingType;
	private AnalysisEngine regexSegmenter;
	private int skipGramTreeDepth;
	private ITreeNode skipGramTreeRoot;
	private JCas localJCas;
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		namedEntityMappingProvider = new MappingProvider();
		namedEntityMappingProvider.setDefault(MappingProvider.LOCATION, "classpath:/org/hucompute/textimager/biofid/lib/ner-default.map");
		namedEntityMappingProvider.setDefault(MappingProvider.BASE_TYPE, NamedEntity.class.getName());
		namedEntityMappingProvider.setOverride(MappingProvider.LANGUAGE, language);
		
		try {
			regexSegmenter = AnalysisEngineFactory.createEngine(RegexSegmenter.class,
					RegexSegmenter.PARAM_TOKEN_BOUNDARY_REGEX, tokenBoundaryRegex,
					RegexSegmenter.PARAM_WRITE_TOKEN, true,
					RegexSegmenter.PARAM_WRITE_FORM, false,
					RegexSegmenter.PARAM_WRITE_SENTENCE, false);
			
			getLogger().info(String.format("Initializing StringTreeGazetteerModel for %s", Class.forName(pTaggingTypeName).getSimpleName()));
			skipGramGazetteerModel = new StringTreeGazetteerModel(sourceLocation, pUseLowercase, language, pMinLength, pGetAllSkips, pSplitHyphen, pAddAbbreviatedTaxa, tokenBoundaryRegex);
			skipGramTreeRoot = ((ITreeGazetteerModel) skipGramGazetteerModel).getTree();
			skipGramTreeDepth = skipGramTreeRoot.depth();
			
			localJCas = JCasFactory.createJCas();
		} catch (IOException | ClassNotFoundException | UIMAException e) {
			throw new ResourceInitializationException(e);
		}
	}
	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		process(aJCas, aJCas.getDocumentText(), 0);
	}
	
	@Override
	protected void process(JCas originalJCas, String text, int zoneBegin) throws AnalysisEngineProcessException {
		namedEntityMappingProvider.configure(originalJCas.getCas());
		taggingType = originalJCas.getTypeSystem().getType(pTaggingTypeName);
		tokenBeginIndex = new ConcurrentHashMap<>();
		
		if (originalJCas.getDocumentText().trim().length() == 0) {
			getLogger().debug(String.format("Skipping empty %s", taggingType.getShortName()));
			return;
		}
		
		getLogger().debug(String.format("Tagging %s", taggingType.getShortName()));
		try {
			localJCas.reset();
			localJCas.setDocumentText(originalJCas.getDocumentText());
			localJCas.setDocumentLanguage(originalJCas.getDocumentLanguage());
			
			SimplePipeline.runPipeline(localJCas, regexSegmenter);
			
			if (pUseSentenceLevelTagging) {
				JCasUtil.select(originalJCas, Sentence.class).forEach(
						sentence -> localJCas.addFsToIndexes(new Sentence(localJCas, sentence.getBegin(), sentence.getEnd()))
				);
			}
			
			Collection<Sentence> sentences = JCasUtil.select(localJCas, Sentence.class);
			if (!pUseSentenceLevelTagging || sentences.isEmpty()) {
				tagEntireDocumentText(originalJCas, localJCas);
			} else {
				int sentencesLength = sentences.stream().map(Sentence::getCoveredText).collect(Collectors.joining(" ")).length();
				getLogger().debug(String.format("Tagging sentences. Coverage: %d/%d", sentencesLength, localJCas.getDocumentText().length()));
				tagSentences(originalJCas, localJCas, sentences);
			}
		} catch (UIMAException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}
	
	private void tagEntireDocumentText(JCas originalJCas, JCas localJCas) {
		getLogger().debug(String.format(
				"%s, tagging entire document text.",
				pUseSentenceLevelTagging ? "PARAM_FORCE_DOCUMENT_TEXT_TAGGING=true" : "Found no sentences"
				)
		);
		
		ArrayList<String> query = getDocumentLevelQuery(localJCas);
		findAllMatches(skipGramTreeRoot, query, 0).forEach(m -> addAnnotation(originalJCas, m));
	}
	
	private ArrayList<String> getDocumentLevelQuery(JCas aJCas) {
		ArrayList<String> query = new ArrayList<>();
		tokens = Lists.newArrayList(JCasUtil.select(aJCas, Lemma.class));
		if (!pUseLemmata || tokens.isEmpty()) {
			tokens = Lists.newArrayList(JCasUtil.select(aJCas, Token.class));
		}
		for (Annotation token : tokens) {
			String qText = getAnnotationText(token);
			query.add(qText);
		}
		return query;
	}
	
	private void tagSentences(JCas originalJCas, JCas localJCas, Collection<Sentence> sentences) {
		tokens = Lists.newArrayList(JCasUtil.select(localJCas, Lemma.class));
		final ConcurrentHashMap<Sentence, Collection<Annotation>> sentenceIndex;
		if (pUseLemmata && !tokens.isEmpty()) {
			sentenceIndex = new ConcurrentHashMap<>(JCasUtil.indexCovered(localJCas, Sentence.class, Lemma.class));
			for (int i = 0; i < tokens.size(); i++) {
				tokenBeginIndex.put(tokens.get(i).getBegin(), i);
			}
		} else {
			sentenceIndex = new ConcurrentHashMap<>(JCasUtil.indexCovered(localJCas, Sentence.class, Token.class));
			tokens = Lists.newArrayList(JCasUtil.select(localJCas, Token.class));
			for (int i = 0; i < tokens.size(); i++) {
				tokenBeginIndex.put(tokens.get(i).getBegin(), i);
			}
		}
		sentences.stream()
				.parallel()
				.flatMap(sentence -> {
					ImmutablePair<Integer, ArrayList<String>> pair = getSentenceList(sentenceIndex, sentence);
					Integer sentenceOffset = pair.left;
					ArrayList<String> query = pair.right;
					if (sentenceOffset < 0 || query.size() == 0) {
						return Stream.empty();
					}
					return findAllMatches(skipGramTreeRoot, query, sentenceOffset).stream();
				})
				.collect(Collectors.toList())
				.forEach(m -> addAnnotation(originalJCas, m));
	}
	
	/**
	 * Get a list of tokens or lemmata covered by this sentence.
	 *
	 * @param sentenceIndex The JCas containing the sentence.
	 * @param sentence      The sentence in question.
	 * @return A list of token or lemma values.
	 */
	private ImmutablePair<Integer, ArrayList<String>> getSentenceList(Map<Sentence, Collection<Annotation>> sentenceIndex, Sentence sentence) {
		ArrayList<String> arrayList = new ArrayList<>();
		ArrayList<Annotation> annotations = new ArrayList<>(sentenceIndex.get(sentence));
		
		int sentenceBeginIndex = -1;
		if (annotations.size() > 0) {
			sentenceBeginIndex = tokenBeginIndex.get(annotations.get(0).getBegin());
			annotations.forEach(
					annotation -> arrayList.add(getAnnotationText(annotation))
			);
		}
		return ImmutablePair.of(sentenceBeginIndex, arrayList);
	}
	
	/**
	 * Get the text for this annotation. Returns the lemma value if the annotation is a {@link Lemma} and its value is
	 * not empty or null. Defaults to {@link Annotation#getCoveredText()} otherwise.
	 *
	 * @param annotation The annotation to get the text for.
	 * @return The text for this annotation.
	 */
	private String getAnnotationText(Annotation annotation) {
		if (annotation instanceof Lemma) {
			String text = ((Lemma) annotation).getValue();
			if (text == null || text.isEmpty() || text.equals("--") || text.equals("_")) {
				text = annotation.getCoveredText();
			}
			return pUseLowercase ? text.toLowerCase() : text;
		} else {
			return pUseLowercase ? annotation.getCoveredText().toLowerCase() : annotation.getCoveredText();
		}
	}
	
	private ArrayList<Match> findAllMatches(ITreeNode root, final ArrayList<String> query, int globalOffset) {
		ArrayList<Match> matches = new ArrayList<>();
		int offset = 0;
		do {
			List<String> subList = query.subList(offset, Math.min(query.size(), offset + skipGramTreeDepth));
			ImmutablePair<String, Integer> matchedString = root.traverse(subList);
			if (!Strings.isNullOrEmpty(matchedString.left) && matchedString.right > -1) {
				int start = offset;
				int end = offset + matchedString.right;
				matches.add(new Match(start + globalOffset, end + globalOffset, matchedString.left));
				offset += matchedString.right;
			}
			offset += 1;
		} while (offset < (query.size() - skipGramTreeDepth) && offset > -1);
		return matches;
	}
	
	
	private void addAnnotation(JCas aJCas, Match match) {
		try {
			Annotation fromToken = tokens.get(match.start);
			Annotation toToken = tokens.get(match.end);
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
			System.err.println(match.value);
			e.printStackTrace();
		}
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
	
}
