package org.biofid.gazetteer.models;

import org.biofid.gazetteer.search.ITreeNode;
import org.biofid.gazetteer.search.StringTreeNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.stream.Stream;

public class StringTreeGazetteerModel extends SkipGramGazetteerModel implements ITreeGazetteerModel {
	
	public final StringTreeNode tree;
	private final HashSet<String> filterSet;
	
	/**
	 * Create 1-skip-n-grams from each taxon in a file from a given list of files.
	 *
	 * @param aSourceLocations    An array of UTF-8 file locations containing a list of one taxon and any number of URIs
	 *                            (comma or space separated) per line.
	 * @param bUseLowercase       If true, use lower cased skip-grams.
	 * @param sLanguage           The language to be used as locale for lower casing.
	 * @param dMinLength          The minimum skip-gram length. All skip-grams (and taxa) with a length lower than this
	 *                            will be omitted.
	 * @param bAllSkips           If true, get all m-skip-n-grams of length n > 2.
	 * @param bSplitHyphen        If true, taxon tokens will be split at hyphens.
	 * @param bAddAbbreviatedTaxa
	 * @param tokenBoundaryRegex
	 * @param filterSet
	 * @throws IOException
	 */
	public StringTreeGazetteerModel(String[] aSourceLocations, Boolean bUseLowercase, String sLanguage, double dMinLength, boolean bAllSkips, boolean bSplitHyphen, boolean bAddAbbreviatedTaxa, String tokenBoundaryRegex, HashSet<String> filterSet) throws IOException {
		super(aSourceLocations, bUseLowercase, sLanguage, dMinLength, bAllSkips, bSplitHyphen, bAddAbbreviatedTaxa, 3);
		this.filterSet = filterSet;
		long startTime = System.currentTimeMillis();
		
		logger.info("Building tree..");
		
		tree = new StringTreeNode(tokenBoundaryRegex, bUseLowercase);
		sortedSkipGramSet.stream()
				.parallel()
				.filter(entry -> !filterSet.contains(entry.toLowerCase()))
				.forEach(tree::insert);
		
		logger.info(String.format("Finished building tree with %d nodes from %d skip-grams in %dms.",
				tree.size(), sortedSkipGramSet.size(), System.currentTimeMillis() - startTime
		));
	}
	
	@Override
	public ITreeNode getTree() {
		return this.tree;
	}
	
	public
	@Deprecated
	Stream<String> stream() {
		throw new UnsupportedOperationException();
	}
}
