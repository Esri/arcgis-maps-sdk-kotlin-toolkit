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
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
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

private const val URL_LINK = "URL_LINK"

@Composable
internal fun MarkdownV2(text: String, modifier: Modifier = Modifier) {
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

@Composable
private fun MarkdownNode(
    node: Node,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    prefix: AnnotatedString? = null,
) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    val text = buildAnnotatedString {
        prefix?.let { append(it) }
        parseMarkdownTree(node, typography, colors)
    }
    ClickableText(text = text, style = textStyle, modifier = modifier)
}

@Composable
private fun Paragraph(paragraph: Paragraph, modifier: Modifier = Modifier) {
    MarkdownNode(
        node = paragraph,
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

@Composable
private fun Heading(heading: Heading, modifier: Modifier = Modifier) {
    val typography = MaterialTheme.typography
    val textStyle = when (heading.level) {
        in 1..3 -> typography.titleLarge
        4 -> typography.titleMedium
        5 -> typography.titleSmall
        else -> typography.titleSmall
    }.copy(fontWeight = FontWeight.Bold)
    MarkdownNode(
        node = heading,
        textStyle = textStyle,
        modifier = modifier
    )
}

@Composable
private fun ListNode(
    node: ListBlock,
    level: Int,
    isOrdered: Boolean,
    modifier: Modifier = Modifier
) {
    var number = if (isOrdered) (node as OrderedList).startNumber else 0
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

@Composable
private fun ClickableText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    var layoutResult by remember {
        mutableStateOf<TextLayoutResult?>(null)
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
        style = style,
        onTextLayout = { layoutResult = it }
    )
}

private fun AnnotatedString.Builder.parseMarkdownTree(
    parent: Node,
    typography: Typography,
    colors: ColorScheme
) {
    parent.forEachChild { node ->
        when (node) {
            is Text -> append(node.literal)
            is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                parseMarkdownTree(node, typography, colors)
            }

            is StrongEmphasis -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                parseMarkdownTree(node, typography, colors)
            }

            is Strikethrough -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                parseMarkdownTree(node, typography, colors)
            }

            is Link -> withStyle(
                SpanStyle(
                    color = Color.Blue,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                pushStringAnnotation(URL_LINK, node.destination)
                parseMarkdownTree(node, typography, colors)
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
}

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
            MarkdownV2(text = markdownText, modifier = Modifier.padding(15.dp))
        }
    }
}
