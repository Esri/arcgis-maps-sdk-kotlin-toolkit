/*
 *
 *  Copyright 2024 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.subcomposition

import androidx.compose.material3.Text
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
public fun Subcomposition(modifier: Modifier = Modifier) {
    val applier = currentComposer.applier

}

// Provided we have a tree with a node base type like the following
public abstract class Node {
    public val children: MutableList<Node> = mutableListOf<Node>()
}

// We would implement an Applier class like the following, which would teach compose how to
// manage a tree of Nodes.
public class NodeApplier<T: Node>(root: T) : AbstractApplier<Node>(root) {
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

// A function like the following could be created to create a composition provided a root Node.
public fun Node.setContent(
    parent: CompositionContext,
    content: @Composable () -> Unit
): Composition {
    return Composition(NodeApplier(this), parent).apply {
        setContent(content)
    }
}

// assuming we have Node sub-classes like "TextNode" and "GroupNode"
public class TextNode : Node() {
    public var text: String = ""
    public var onClick: () -> Unit = {}
}

public class GroupNode : Node()

// Composable equivalents could be created
@Composable
private fun MyText(text: String, onClick: () -> Unit = {}) {
    ComposeNode<TextNode, NodeApplier<Node>>(::TextNode) {
        set(text) { this.text = it }
        set(onClick) { this.onClick = it }
    }
}

@Composable
public fun Group(content: @Composable () -> Unit) {
    ComposeNode<GroupNode, NodeApplier<Node>>(::GroupNode, {}, content)
}

// and then a sample tree could be composed:
public fun runApp(root: GroupNode = GroupNode(), parent: CompositionContext): Composition {
    return root.setContent(parent) {
        var count by remember { mutableIntStateOf(0) }
        Group {
            println("displaying CONTENT")
            Text("FOO")
            MyText("Count: $count")
            MyText("Increment") { count++ }
        }
    }
}

