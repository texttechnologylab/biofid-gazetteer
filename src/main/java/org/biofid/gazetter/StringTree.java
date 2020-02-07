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
		ImmutableList<String> taxa = ImmutableList.of("Wald", "Wald Lichtung");
		ImmutableList<String> uriList = ImmutableList.of("https://www.wikidata.org/wiki/Q4421", "https://www.wikidata.org/wiki/Q4358873");
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
		
		String query = "Im Wald ist eine Lichtung. Auf der Wald Lichtung ist nichts.";
		IndexingMap<String> lookup = new IndexingMap<>();
		for (String taxon : taxa) {
			lookup.add(taxon);
		}
		System.out.println(stringTree.findAllMatches(query, lookup).stream().map(Arrays::toString).collect(Collectors.joining(", ")));
	}
}
