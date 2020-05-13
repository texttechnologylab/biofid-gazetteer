package org.biofid.gazetteer.search;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ITreeNode {
    boolean hasValue();

    boolean isLeaf();

    void insert(String value);

    int size();

    int leafs();

    int nodesWithValue();

    String traverse(@Nonnull String subString);

    @Override
    String toString();
    
    String getValue();
}
