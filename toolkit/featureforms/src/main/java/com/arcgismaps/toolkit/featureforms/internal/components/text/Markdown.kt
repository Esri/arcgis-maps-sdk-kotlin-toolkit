/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms.internal.components.text

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Link
import org.commonmark.node.ListBlock
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.parser.Parser
import org.commonmark.node.Document

private const val URL_LINK = "URL_LINK"

/**
 * Renders a markdown formatted text.
 *
 * @param text The markdown formatted text.
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
internal fun Markdown(text: String, modifier: Modifier = Modifier) {
    val document = remember(text) {
        Parser.builder()
            .extensions(listOf(StrikethroughExtension.create()))
            .build()
            .parse(text)
    }
    Column(modifier = modifier) {
        MarkdownTree(document)
    }
}

/**
 * Entry point for parsing and rendering the markdown tree.
 *
 * @param root The root node of the markdown tree. This can be a [Document] node.
 */
@Composable
private fun MarkdownTree(root: Node) {
    root.forEachChild { node ->
        when (node) {
            is Paragraph -> Paragraph(
                paragraph = node,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            is Heading -> Heading(heading = node, modifier = Modifier.padding(vertical = 8.dp))
            is BulletList -> ListNode(
                node = node,
                level = 0,
                isOrdered = false,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            is OrderedList -> ListNode(
                node = node,
                level = 0,
                isOrdered = true,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            else -> Log.w("Markdown", "Unsupported node type: ${node.javaClass.simpleName}")
        }
    }
}

/**
 * Renders a markdown node and its children recursively. The tree is built as an [AnnotatedString]
 * and displayed using a [Text] that supports tap gestures on links.
 *
 * @param node The markdown node to render.
 * @param modifier The modifier to be applied to the composable.
 * @param textStyle The text style to be applied to the text.
 * @param prefix A prefix string that will be prepended to the text.
 */
@Composable
private fun MarkdownNode(
    node: Node,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    prefix: AnnotatedString? = null,
) {
    val uriHandler = LocalUriHandler.current
    var layoutResult by remember {
        mutableStateOf<TextLayoutResult?>(null)
    }
    val text = buildAnnotatedString {
        withStyle(textStyle.toSpanStyle()) {
            prefix?.let { append(it) }
            parseMarkdownTree(node)
        }
    }
    Text(
        text = text,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offsetPosition ->
                if (layoutResult != null) {
                    val position = layoutResult!!.getOffsetForPosition(offsetPosition)
                    text.getStringAnnotations(position, position).firstOrNull()?.let { annotation ->
                        if (annotation.tag == URL_LINK) {
                            uriHandler.openUri(annotation.item)
                        }
                    }
                }
            }
        },
        style = textStyle,
        onTextLayout = { layoutResult = it }
    )
}

/**
 * Renders a markdown [Paragraph].
 */
@Composable
private fun Paragraph(paragraph: Paragraph, modifier: Modifier = Modifier) {
    MarkdownNode(
        node = paragraph,
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

/**
 * Renders a markdown [Heading].
 */
@Composable
private fun Heading(heading: Heading, modifier: Modifier = Modifier) {
    val typography = MaterialTheme.typography
    val textStyle = when (heading.level) {
        in 1..3 -> typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        // Designer only supports 4, 5, and 6 levels of headings.
        4 -> typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        5 -> typography.titleMedium
        6 -> typography.titleSmall
        else -> typography.titleSmall
    }
    MarkdownNode(
        node = heading,
        textStyle = textStyle,
        modifier = modifier
    )
}

/**
 * Renders a markdown [BulletList] or [OrderedList] based on the [isOrdered] flag. This also
 * supports nested lists that are rendered recursively. For nested lists, the nesting level is
 * determined by the [level] parameter.
 *
 * @param node The list node to render.
 * @param level The nesting level of the list.
 * @param isOrdered Flag to indicate if the list is ordered.
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
private fun ListNode(
    node: ListBlock,
    level: Int,
    isOrdered: Boolean,
    modifier: Modifier = Modifier
) {
    var number = if (isOrdered) (node as OrderedList).markerStartNumber else 0
    Column(
        modifier = modifier
    ) {
        node.forEachChild { child ->
            val bullet = if (isOrdered) "${number++}. " else when (level % 3) {
                0 -> "• "
                1 -> "◦ "
                else -> "▪ "
            }
            child.forEachChild { nestedChild ->
                when (nestedChild) {
                    is OrderedList -> ListNode(nestedChild, level + 1, true)
                    is BulletList -> ListNode(nestedChild, level + 1, false)
                    else -> MarkdownNode(
                        node = nestedChild,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        prefix = buildAnnotatedString {
                            append("${" ".repeat((level + 1) * 4)}$bullet")
                        }
                    )
                }
            }
        }
    }
}

/**
 * Parses the markdown tree recursively and builds an [AnnotatedString] with the appropriate styles.
 *
 * @param parent The parent node to parse.
 */
@Composable
private fun AnnotatedString.Builder.parseMarkdownTree(
    parent: Node
): AnnotatedString.Builder {
    val colors = MaterialTheme.colorScheme
    parent.forEachChild { node ->
        when (node) {
            is Text -> append(node.literal)
            is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                parseMarkdownTree(node)
            }

            is StrongEmphasis -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                parseMarkdownTree(node)
            }

            is Strikethrough -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                parseMarkdownTree(node)
            }

            is Link -> withStyle(
                SpanStyle(
                    color = Color.Blue,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                pushStringAnnotation(URL_LINK, node.destination)
                parseMarkdownTree(node)
                pop()
            }


            is Code -> withStyle(
                SpanStyle(
                    color = colors.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    background = colors.surfaceVariant
                )
            ) {
                append(node.literal)
            }

            is SoftLineBreak -> append(" ")
            is HardLineBreak -> appendLine()
            else -> Log.w("Markdown", "Unsupported node type: ${node.javaClass.simpleName}")
        }
    }
    return this
}

/**
 * Iterates over the children of a node and applies the specified action to each child.
 */
private inline fun Node.forEachChild(action: (Node) -> Unit) {
    var node = firstChild
    while (node != null) {
        action(node)
        node = node.next
    }
}

@Composable
@Preview(showBackground = true)
private fun MarkdownPreview() {
    val markdownText = """
        # General formatting

        `Inline code formatting`  
        **Bold text**  
        _Italicized text_  
        ~~Strikethrough text~~  
        ***Bold and italicized text***
        
        [Link](https://www.arcgis.com)
        
        ## Lists

        ### Numbered lists

        Numbered lists using number + period format. Text with right paren `1)` will be converted to number + period format.

        1.  one
        2.  two
        3.  three

        Bulleted lists

        1. Dog
            1) German Shepherd
            2) Belgian Shepherd
                1. Malinois
                2. Groenendael
                3. Tervuren
        2. Cat
            1. Siberian
            2. Siamese

        Bulleted lists - Start a line with `*` or `-` followed by a space.

        Lists using asterisks, including nested ones (authored by UI).  Dashes typed in will be converted to asterisks.

        *   One
            *   Won
            *   Uno
                * Hello
                * There
                    * Android
                    * iOS
        *   Two
        *   Three

        <DIV CLASS="foo">

        *Markdown*

        </DIV>
    """.trimIndent()
    LazyColumn {
        item {
            Markdown(text = markdownText, modifier = Modifier.padding(15.dp))
        }
    }
}
