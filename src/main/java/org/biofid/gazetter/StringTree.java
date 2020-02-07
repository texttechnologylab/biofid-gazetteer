package org.biofid.gazetter;

/**
 * Created on 07.02.20.
 */
public class StringTree {
	
	private final boolean toLower;
	public final StringTreeNode root;
	
	public StringTree(boolean toLower) {
		this.toLower = toLower;
		this.root = new StringTreeNode();
	}
	
	public StringTree() {
		this(true);
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

//	/**
//	 * TODO: Remove
//	 *
//	 * @param query
//	 * @param lookup
//	 * @return
//	 */
//	public ArrayList<int[]> findAllMatches(String query, Map<String, Integer> lookup) {
//		query = this.toLower ? query.toLowerCase() : query;
//		ArrayList<int[]> matches = new ArrayList<>();
//		int offset = 0;
//		int pos = 0;
//		StringTreeNode node = root;
//
//		// Last match data for longest match search
//		int lastStart = 0;
//		int lastEnd = 0;
//		int lastPos = 0;
//		String lastMatch = null;
//
//		do {
//			String substring = query.substring(offset);
//			char c = substring.charAt(pos);
//			node = node.children.get(c);
//
//			// If the current node does not have a child with the current character,
//			// step out of the tree walk and start with next offset
//			if (node == null) {
//				offset++;
//				pos = 0;
//				node = root;
//				if (lastMatch != null) {
//					System.out.println(String.format("\"%s':{\"start\": %d, \"end\": %d, \"match_length\": %d, \"full_length\":%d, \"taxon\":\"%s\"}",
//							query.subSequence(lastStart, lastEnd), lastStart, lastEnd, lastPos, lastEnd - lastStart, lastMatch));
//					matches.add(new int[]{lastStart, lastEnd, lookup.get(lastMatch)});
//					lastMatch = null;
//				}
//			} else if (node.hasValue()) {
//				// If the current node has a taxon value,
//				// check if it is a leaf
//				pos++;
//				int fullLength = pos + node.substring.length();
//				if (node.isLeaf()) {
//					// Check for shortened tree path
//					boolean b = substring.substring(pos).startsWith(node.substring);
//					if (b) {
//						System.out.println(String.format("\"%s':{\"start\": %d, \"end\": %d, \"match_length\": %d, \"full_length\":%d, \"taxon\":\"%s\"}",
//								query.subSequence(offset, offset + fullLength), offset, offset + fullLength, pos, fullLength, node.taxon));
//						matches.add(new int[]{offset, offset + fullLength, lookup.get(node.taxon)});
//						offset += fullLength;
//					} else {
//						offset++;
//					}
//					pos = 0;
//					node = root;
//				} else {
//					// If it is not a leaf, save the current match and continue searching
//					lastStart = offset;
//					lastEnd = offset + fullLength;
//					lastPos = pos;
//					lastMatch = node.taxon;
//				}
//			} else {
//				pos++;
//			}
//		} while (offset < query.length());
//		return matches;
//	}
//
//	/**
//	 * TOOD: remove
//	 *
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		ImmutableList<String> taxa = ImmutableList.of("Wald", "Wald Lichtung");
//		ImmutableList<String> uriList = ImmutableList.of("https://www.wikidata.org/wiki/Q4421", "https://www.wikidata.org/wiki/Q4358873");
//		ArrayList<HashSet<URI>> uris = new ArrayList<>();
//		for (String string : uriList) {
//			try {
//				uris.add(Sets.newHashSet(new URI(string)));
//			} catch (URISyntaxException e) {
//				e.printStackTrace();
//			}
//		}
//		StringTree stringTree = new StringTree(taxa, false);
//		for (String taxon : taxa) {
//			stringTree.insert(taxon, taxon);
//		}
//		System.out.println(stringTree.toString());
//
//		String query = "Im Wald ist eine Lichtung. Auf der Wald Lichtung ist nichts.";
//		IndexingMap<String> lookup = new IndexingMap<>();
//		for (String taxon : taxa) {
//			lookup.add(taxon);
//		}
//		System.out.println(stringTree.findAllMatches(query, lookup).stream().map(Arrays::toString).collect(Collectors.joining(", ")));
//	}
}
