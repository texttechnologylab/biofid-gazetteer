package org.biofid.gazetter.TreeSearch;

import org.apache.logging.log4j.util.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on 07.02.20.
 */
public class StringTreeNode implements ITreeNode {

    public final StringTreeNode parent;
    public final ConcurrentHashMap<String, StringTreeNode> children;
    public String value;
    private Pattern pattern = Pattern.compile("[^\\p{Alnum}-]", Pattern.UNICODE_CHARACTER_CLASS);


    /**
     * Create a root node.
     */
    public StringTreeNode() {
        this.parent = null;
        this.children = new ConcurrentHashMap<>();
        this.value = null;
    }

    /**
     * Create a regular node.
     *
     * @param parent
     */
    public StringTreeNode(StringTreeNode parent) {
        this.parent = parent;
        this.children = new ConcurrentHashMap<>();
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
            synchronized (this.children) {
                this.value = value;
                return;
            }
        }

        int index = getIndex(subString);

        String key;
        key = getKey(subString, index);
        synchronized (this.children) {
            if (!this.children.containsKey(key)) {
                this.children.put(key, new StringTreeNode(this));
            }
        }
        if (index > 0) {
            this.children.get(key).insert(subString.substring(index + 1), value);
        } else {
            this.children.get(key).insert("", value);
        }
    }

    public String traverse(@Nonnull String subString, @Nullable String lastValue) {
        if (subString.length() == 0) {
            return this.value == null ? lastValue : this.value;
        }

        int index = getIndex(subString);
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

    private int getIndex(String subString) {
        int index = -1;
        Matcher matcher = pattern.matcher(subString);
        if (matcher.find()) {
            index = matcher.start();
        }
        return index;
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

    public String traverse(@Nonnull String fullString) {
        return this.traverse(fullString, null);
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
