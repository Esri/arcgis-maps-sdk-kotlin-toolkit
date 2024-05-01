package com.arcgismaps.toolkit.geoviewcompose

import androidx.compose.runtime.AbstractApplier
import com.arcgismaps.mapping.view.MapView

public abstract class Node {
    public val children: MutableList<Node> = mutableListOf<Node>()
}

// We would implement an Applier class like the following, which would teach compose how to
// manage a tree of Nodes.
public class NodeApplier(root: Node, internal val mapView: MapView) : AbstractApplier<Node>(root) {
    override fun insertTopDown(index: Int, instance: Node) {
        current.children.add(index, instance)
    }

    override fun insertBottomUp(index: Int, instance: Node) {
        // Ignored as the tree is built top-down.
    }

    override fun remove(index: Int, count: Int) {
        current.children.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.children.move(from, to, count)
    }

    override fun onClear() {
        root.children.clear()
    }
}