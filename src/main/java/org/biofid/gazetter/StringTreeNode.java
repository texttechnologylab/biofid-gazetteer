package org.biofid.gazetter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created on 07.02.20.
 */
public class StringTreeNode {
	
	public String substring;
	public String taxon;
	public StringTreeNode parent;
	public HashMap<Character, StringTreeNode> children;
	
	
	/**
	 * Create a root node.
	 */
	public StringTreeNode() {
		this.parent = null;
		this.children = new HashMap<>();
		this.taxon = null;
	}
	
	/**
	 * Create a regular node.
	 *
	 * @param parent
	 */
	public StringTreeNode(StringTreeNode parent) {
		this.parent = parent;
		this.children = new HashMap<>();
		this.taxon = null;
	}
	
	/**
	 * Create a leaf node.
	 *
	 * @param parent
	 */
	public StringTreeNode(StringTreeNode parent, String substring, String taxon) {
		this.parent = parent;
		this.children = null;
		this.substring = substring;
		this.taxon = taxon;
	}
	
	public boolean isLeaf() {
		return this.taxon != null;
	}
	
	public void insert(String subString, final String taxon) {
		// Create new child node for former leaf
		if (this.isLeaf()) {
			String formerSubstring = this.substring;
			String formerTaxon = this.taxon;
			this.substring = null;
			this.taxon = null;
			this.children = new HashMap<>();
			
			StringTreeNode child = new StringTreeNode(this, formerSubstring.substring(1), formerTaxon);
			this.children.put(formerSubstring.charAt(0), child);
		}
		
		// Create new child for new taxon
		char key = subString.length() > 0 ? subString.charAt(0) : Character.MIN_VALUE;
		String nextSubstring = subString.length() > 0 ? subString.substring(1) : "";
		if (this.children.containsKey(key)) {
			this.children.get(key).insert(nextSubstring, taxon);
		} else {
			StringTreeNode child = new StringTreeNode(this, nextSubstring, taxon);
			this.children.put(key, child);
		}
	}
	
	public int size() {
		if (this.isLeaf())
			return 1;
		return 1 + this.children.values().stream().mapToInt(StringTreeNode::size).sum();
	}
	
	public int leafs() {
		if (this.isLeaf())
			return 1;
		return this.children.values().stream().mapToInt(StringTreeNode::leafs).sum();
	}
	
	@Override
	public String toString() {
		if (this.isLeaf()) {
			return String.format("\"isLeaf\":\"True\", \"substring\":\"%s\", \"taxon\":\"%s\"", substring, taxon);
		} else {
			ArrayList<String> strings = new ArrayList<>();
			for (Map.Entry<Character, StringTreeNode> entry : this.children.entrySet()) {
				strings.add(String.format("\"%s\": {%s}", entry.getKey(), entry.getValue().toString()));
			}
			return "" + String.join(",\n", strings) + "";
		}
	}
}
