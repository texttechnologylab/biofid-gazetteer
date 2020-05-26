package org.biofid.gazetteer.models;

import org.biofid.gazetteer.tree.ITreeNode;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface ITreeGazetteerModel {
	Map<String, String> getSkipGramTaxonLookup();
	
	Set<String> getSortedSkipGramSet();
	
	Map<String, HashSet<URI>> getTaxonUriMap();
	
	ITreeNode getTree();
}
