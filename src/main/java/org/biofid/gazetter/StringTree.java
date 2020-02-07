package org.biofid.gazetter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.texttechnologylab.utilities.collections.IndexingMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created on 07.02.20.
 */
public class StringTree {
	
	private final ImmutableList<Character> vocabulary;
	private final int vocabSize;
	private final boolean toLower;
	public final StringTreeNode root;
	
	public StringTree(Iterable<String> taxonSet, boolean toLower) {
//		this.vocabulary = ImmutableList.copyOf(ImmutableSet.copyOf(characterVocabulary));
		this.toLower = toLower;
		System.out.printf("%s: Building vocabulary..\n", this.getClass().getSimpleName());
		TreeSet<Character> characters = new TreeSet<>();
		for (String taxon : taxonSet) {
			for (int i = 0; i < taxon.length(); i++) {
				char c = this.toLower ? taxon.toLowerCase().charAt(i) : taxon.charAt(i);
				characters.add(c);
			}
		}
		this.vocabulary = ImmutableList.copyOf(characters);
		this.vocabSize = this.vocabulary.size();
		this.root = new StringTreeNode();
	}
	
	
	public StringTree(Iterable<String> taxonSet) {
		this(taxonSet, true);
	}
	
	public void insert(String string, final String taxon) {
		string = this.toLower ? string.toLowerCase() : string;
		char c = string.charAt(0);
		StringTreeNode child = this.getChild(c);
		child.insert(string.substring(1), taxon);
	}
	
	public StringTreeNode getChild(char c) {
		StringTreeNode child;
		if (!root.children.containsKey(c)) {
			child = new StringTreeNode(root);
			root.children.put(c, child);
		} else {
			child = root.children.get(c);
		}
		return child;
	}
	
	@Override
	public String toString() {
		return "StringTree{" +
				"\"vocabulary\":\"" + vocabulary +
				"\", \"vocabSize\":" + vocabSize +
				", \"nodes\":" + this.size() +
				", \"leafs\":" + this.leafs() +
				", \"root\":{" + root +
				"}}";
	}
	
	public int size() {
		return this.root.children.values().stream().mapToInt(StringTreeNode::size).sum();
	}
	
	public int leafs() {
		return this.root.children.values().stream().mapToInt(StringTreeNode::leafs).sum();
	}
	
	/**
	 * TODO: Remove
	 *
	 * @param query
	 * @param lookup
	 * @return
	 */
	public ArrayList<int[]> findAllMatches(String query, Map<String, Integer> lookup) {
		query = this.toLower ? query.toLowerCase() : query;
		ArrayList<int[]> matches = new ArrayList<>();
		int offset = 0;
		int pos = 0;
		StringTreeNode node = root;
		do {
			String substring = query.substring(offset);
			char c = substring.charAt(pos);
			node = node.children.get(c);
			if (node == null) {
				offset++;
				pos = 0;
				node = root;
			} else if (node.isLeaf()) {
				pos++;
				boolean b = substring.substring(pos).startsWith(node.substring);
				if (b) {
					int fullLength = pos + node.substring.length();
					System.out.println(String.format("\"%s':{\"start\": %d, \"end\": %d, \"match_length\": %d, \"full_length\":%d, \"taxon\":\"%s\"}",
							query.subSequence(offset, offset + fullLength), offset, offset + fullLength, pos, fullLength, node.taxon));
					matches.add(new int[]{offset, offset + fullLength, lookup.get(node.taxon)});
					offset += pos;
				} else {
					offset++;
				}
				pos = 0;
				node = root;
			} else {
				pos++;
			}
		} while (offset < query.length());
		return matches;
	}
	
	/**
	 * TOOD: remove
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		ImmutableList<String> taxa = ImmutableList.of("Sipodotus wallacii", "Weihnachtsinselkauz", "Acacia quinquinervia", "Hypsolebias hellneri", "Querquedula natator", "Camaroptera brachyura abessinica", "Dryopteris ×shorapanensis", "pallescens", "Melanospiza bicolor", "Sylvia mystacea mystacea", "Harpactes reinwardtii mackloti", "Myiarchus tuberculifer olivascens", "Trematomus scotti", "Prinia superciliosa superciliosa", "Pseudocorys", "Dendroica castanea", "Leucosarcia melanoleuca", "Gecinus levaillanti levaillanti", "Proteromonadidae", "Eristalis fraterculus", "Rotrücken-Ameisenfänger", "castaneiceps", "Platalea pygmea", "Relicanthus daphneae", "Isoetes gigantea", "Aegithalos", "Braunstirn-Regenpfeifer", "Coryphaispiza", "Robsonius thompsoni", "Myiophobus roraimae sadiecoatsae", "Wald");
		ImmutableList<String> uriList = ImmutableList.of("https://www.biofid.de/bio-ontologies/Aves#GBIF_2487491", "https://www.biofid.de/bio-ontologies/Aves#GBIF_2497815", "http://www.wikidata.org/entity/Q15288576", "http://www.wikidata.org/entity/Q20745403", "https://www.biofid.de/bio-ontologies/Aves#GBIF_9747558", "https://www.biofid.de/bio-ontologies/Aves#GBIF_6087949", "http://www.wikidata.org/entity/Q17181734", "https://www.biofid.de/bio-ontologies/Aves#GBIF_9060390", "https://www.biofid.de/bio-ontologies/Aves#GBIF_9426540", "https://www.biofid.de/bio-ontologies/Aves#GBIF_7342045", "https://www.biofid.de/bio-ontologies/Aves#GBIF_5232066", "https://www.biofid.de/bio-ontologies/Aves#GBIF_6174445", "http://www.wikidata.org/entity/Q2286619", "https://www.biofid.de/bio-ontologies/Aves#GBIF_8753029", "https://www.biofid.de/bio-ontologies/Aves#GBIF_4848593", "https://www.biofid.de/bio-ontologies/Aves#GBIF_2489903", "https://www.biofid.de/bio-ontologies/Aves#GBIF_2495910", "https://www.biofid.de/bio-ontologies/Aves#GBIF_8906002", "http://www.wikidata.org/entity/Q7251573", "http://www.wikidata.org/entity/Q14521285", "https://www.biofid.de/bio-ontologies/Aves#GBIF_2490017", "https://www.biofid.de/bio-ontologies/Aves#GBIF_7585017", "https://www.biofid.de/bio-ontologies/Aves#GBIF_8731063", "http://www.wikidata.org/entity/Q2603657", "http://www.wikidata.org/entity/Q17024223", "https://www.biofid.de/bio-ontologies/Aves#GBIF_9270076", "https://www.biofid.de/bio-ontologies/Aves#GBIF_2480315", "https://www.biofid.de/bio-ontologies/Aves#GBIF_4846612", "https://www.biofid.de/bio-ontologies/Aves#GBIF_7966412", "https://www.biofid.de/bio-ontologies/Aves#GBIF_6174544", "https://www.wikidata.org/wiki/Q4421");
		ArrayList<HashSet<URI>> uris = new ArrayList<>();
		for (String string : uriList) {
			try {
				uris.add(Sets.newHashSet(new URI(string)));
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		StringTree stringTree = new StringTree(taxa, false);
		for (String taxon : taxa) {
			stringTree.insert(taxon, taxon);
		}
		System.out.println(stringTree.toString());
		
		String query = "Der Rotrücken-Ameisenfänger läuft durch den Wald.";
		IndexingMap<String> lookup = new IndexingMap<>();
		for (String taxon : taxa) {
			lookup.add(taxon);
		}
		System.out.println(stringTree.findAllMatches(query, lookup).stream().map(Arrays::toString).collect(Collectors.joining(", ")));
	}
}
