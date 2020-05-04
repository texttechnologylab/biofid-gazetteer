package org.biofid.gazetter;


import org.apache.commons.lang3.StringUtils;

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
	
	public boolean hasValue() {
		return this.taxon != null;
	}
	
	public boolean isLeaf() {
		return this.children == null;
	}
	
	public void insert(String subString, final String taxon) {
		// Create new child node for former leaf
		if (this.hasValue()) {
			String formerSubstring = this.substring;
			String formerTaxon = this.taxon;
			this.children = new HashMap<>();
			
			char key;
			String substring;
			if (formerSubstring.length() > 0) {
				this.substring = null;
				this.taxon = null;
				key = formerSubstring.charAt(0);
				substring = formerSubstring.substring(1);
				StringTreeNode child = new StringTreeNode(this, substring, formerTaxon);
				this.children.put(key, child);
			}
		}
		
		// Create new child for new taxon
		if (subString.length() > 0) {
			char key = subString.charAt(0);
			String nextSubstring = subString.substring(1);
			if (this.children.containsKey(key)) {
				this.children.get(key).insert(nextSubstring, taxon);
			} else {
				StringTreeNode child = new StringTreeNode(this, nextSubstring, taxon);
				this.children.put(key, child);
			}
		} else {
			this.substring = "";
			this.taxon = taxon;
		}
		
	}
	
	public int size() {
		if (this.hasValue())
			return 1;
		return 1 + this.children.values().stream().mapToInt(StringTreeNode::size).sum();
	}
	
	public int leafs() {
		if (this.hasValue())
			return 1;
		return this.children.values().stream().mapToInt(StringTreeNode::leafs).sum();
	}
	
	@Override
	public String toString() {
		String node = "";
		if (this.hasValue()) {
			node = String.format("\"isLeaf\":\"True\", \"substring\":\"%s\", \"taxon\":\"%s\"", substring, taxon);
		}
		String children = "";
		if (this.children != null) {
			ArrayList<String> strings = new ArrayList<>();
			for (Map.Entry<Character, StringTreeNode> entry : this.children.entrySet()) {
				strings.add(String.format("\"%s\": {%s}", entry.getKey(), entry.getValue().toString()));
			}
			children = String.join(",\n", strings) + "";
		}
		return node + (StringUtils.isNotBlank(node) && StringUtils.isNotBlank(children) ? ", " : "") + children;
	}
}
