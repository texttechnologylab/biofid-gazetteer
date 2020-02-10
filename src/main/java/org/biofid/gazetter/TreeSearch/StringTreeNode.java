package org.biofid.gazetter.TreeSearch;

import org.apache.logging.log4j.util.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created on 07.02.20.
 *
 * Currently only works on tokenized text!
 */
public class StringTreeNode implements ITreeNode {

    public final StringTreeNode parent;
    public final HashMap<String, StringTreeNode> children;
    public String value;


    /**
     * Create a root node.
     */
    public StringTreeNode() {
        this.parent = null;
        this.children = new HashMap<>();
        this.value = null;
    }

    /**
     * Create a regular node.
     *
     * @param parent
     */
    public StringTreeNode(StringTreeNode parent) {
        this.parent = parent;
        this.children = new HashMap<>();
        this.value = null;
    }

    public boolean hasValue() {
        return this.value != null;
    }

    public boolean isLeaf() {
        return this.children.isEmpty();
    }

    public void insert(String value) {
        this.insert(value.trim(), value);
    }

    public void insert(String subString, final String value) {
        if (subString.length() == 0) {
            this.value = value;
            return;
        }

        int index = subString.indexOf(" ");
        String key;
        key = getKey(subString, index);
        if (!this.children.containsKey(key)) {
            this.children.put(key, new StringTreeNode(this));
        }
        if (index > 0)
            this.children.get(key).insert(subString.substring(index), value);
    }

    private String getKey(String subString, int index) {
        String key;
        if (index > 0) {
            key = subString.substring(0, index);
        } else {
            key = subString;
        }
        return key;
    }

    public int size() {
        return 1 + this.children.values().stream().mapToInt(StringTreeNode::size).sum();
    }

    public int leafs() {
        if (this.children.size() == 0) {
            return 1;
        } else {
            return this.children.values().stream().mapToInt(StringTreeNode::leafs).sum();
        }
    }

    public int nodesWithValue() {
        int val = this.hasValue() ? 1 : 0;
        return val + this.children.values().stream().mapToInt(StringTreeNode::nodesWithValue).sum();
    }

    public String traverse(@Nonnull String subString) {
        return this.traverse(subString, null);
    }

    public String traverse(@Nonnull String subString, @Nullable String lastValue) {
        if (subString.length() == 0) {
            return this.value == null ? lastValue : this.value;
        }

        int index = subString.indexOf(" ");
        String key = this.getKey(subString, index);

        // save value if this node has one
        if (this.value != null) {
            lastValue = this.value;
        }

        if (this.children.containsKey(key))
            return this.children.get(key).traverse(subString.substring(key.length() + 1), lastValue);
        else
            return lastValue;
    }

    @Override
    public String toString() {
        String node = "";
        if (this.hasValue()) {
            node = String.format("\"isLeaf\":\"%b\", \"value\":\"%s\"", this.isLeaf(), value);
        }
        String children = "";
        if (!this.isLeaf()) {
            ArrayList<String> strings = new ArrayList<>();
            for (Map.Entry<String, StringTreeNode> entry : this.children.entrySet()) {
                strings.add(String.format("\"%s\": {%s}", entry.getKey(), entry.getValue().toString()));
            }
            children = String.join(",\n", strings) + "";
        }
        String s = node + (Strings.isNotBlank(node) && Strings.isNotBlank(children) ? ", " : "") + children;

        if (this.parent == null)
            return "{\"StringTree\": {" + s + "}}";
        else
            return s;
    }
}
