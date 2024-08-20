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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.parser.Parser

private const val URL_LINK = "URL_LINK"

@Composable
internal fun Markdown(text: String, modifier: Modifier = Modifier) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    val markdownText = remember {
        parseMarkdown(text, typography, colors)
    }
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = markdownText,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offsetPosition ->
                if (layoutResult.value != null) {
                    val position = layoutResult.value!!.getOffsetForPosition(offsetPosition)
                    markdownText.getStringAnnotations(position, position).firstOrNull()
                        ?.let { annotation ->
                            if (annotation.tag == URL_LINK) {
                                Log.e("TAG", "URL: ${annotation.item}")
                            }
                        }
                }
            }
        },
        lineHeight = 25.sp,
        onTextLayout = { layoutResult.value = it }
    )
}

private fun parseMarkdown(
    text: String,
    typography: Typography,
    colors: ColorScheme
): AnnotatedString {
    val parser = Parser.builder()
        .extensions(listOf(StrikethroughExtension.create()))
        .build()
    val document = parser.parse(text)
    return buildAnnotatedString {
        visitMarkdownNode(document, typography, colors)
    }
}

private fun AnnotatedString.Builder.visitMarkdownNode(
    node: Node,
    typography: Typography,
    colors: ColorScheme
) {
    when (node) {
        is Document -> {
            visitChildren(node, typography, colors)
        }

        is Paragraph -> {
            withStyle(typography.bodyMedium.toSpanStyle()) {
                visitChildren(node, typography, colors)
                appendLine()
            }
        }

        is Heading -> {
            val style = when (node.level) {
                in 1..3 -> typography.titleLarge
                4 -> typography.titleMedium
                5 -> typography.titleSmall
                else -> typography.titleSmall
            }.copy(fontWeight = FontWeight.Bold)
            withStyle(style = style.toSpanStyle()) {
                withStyle(style = style.toParagraphStyle()) {
                    visitChildren(node, typography, colors)
                    appendLine()
                }
            }
        }

        is Text -> {
            Log.e(
                "TAG",
                "visitMarkdownNode: \"${node.literal}\", parent: ${node.parent.javaClass.simpleName}"
            )
            append(node.literal)
        }

        is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            visitChildren(node, typography, colors)
        }

        is StrongEmphasis -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            visitChildren(node, typography, colors)
        }

        is Strikethrough -> {
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                visitChildren(node, typography, colors)
            }
        }

        is Link -> {
            withStyle(
                SpanStyle(
                    color = Color.Blue,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                pushStringAnnotation(URL_LINK, node.destination)
                visitChildren(node, typography, colors)
                pop()
            }
        }

        is Code -> {
            withStyle(
                SpanStyle(
                    color = colors.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    background = colors.surfaceVariant
                )
            ) {
                append(node.literal)
            }
        }

        is BulletList -> {
            withStyle(
                SpanStyle(
                    textGeometricTransform = TextGeometricTransform(),
                    baselineShift = BaselineShift.Subscript
                )
            ) {
                parseBulletList(node, typography, colors, 0)
                appendLine()
            }
        }

        is OrderedList -> {
            withStyle(
                SpanStyle(
                    textGeometricTransform = TextGeometricTransform(),
                    baselineShift = BaselineShift.Subscript
                )
            ) {
                parseOrderedList(node, typography, colors, 0)
                appendLine()
            }
        }

        is SoftLineBreak -> {
            append(" ")
        }

        is HardLineBreak -> {
            appendLine()
        }

        else -> {
            Log.w(
                "Markdown",
                "visitMarkdownNode: Unsupported node type: ${node.javaClass.simpleName}"
            )
        }
    }
}


private fun AnnotatedString.Builder.visitChildren(
    node: Node,
    typography: Typography,
    colors: ColorScheme
) {
    var child = node.firstChild
    while (child != null) {
        visitMarkdownNode(child, typography, colors)
        child = child.next
    }
}

private fun AnnotatedString.Builder.parseOrderedList(
    node: OrderedList,
    typography: Typography,
    colors: ColorScheme,
    level: Int
) {
    var number = 1
    var child = node.firstChild
    while (child != null) {
        if (child is ListItem) {
            append("${" ".repeat((level + 1) * 4)}${number++}. ")
            parseListItem(child, typography, colors, level)
        }
        child = child.next
    }
}

private fun AnnotatedString.Builder.parseBulletList(
    node: BulletList,
    typography: Typography,
    colors: ColorScheme,
    level: Int
) {
    val bullet = when (level % 3) {
        0 -> "•"
        1 -> "◦"
        else -> "▪"
    }
    var child = node.firstChild
    while (child != null) {
        if (child is ListItem) {
            append("${" ".repeat((level + 1) * 4)}$bullet ")
            parseListItem(child, typography, colors, level)
        }
        child = child.next
    }
}

private fun AnnotatedString.Builder.parseListItem(
    node: ListItem,
    typography: Typography,
    colors: ColorScheme,
    level: Int
) {
    var child = node.firstChild
    while (child != null) {
        when (child) {
            is OrderedList -> {
                parseOrderedList(child, typography, colors, level + 1)
            }

            is BulletList -> {
                parseBulletList(child, typography, colors, level + 1)
            }

            else -> visitMarkdownNode(child, typography, colors)
        }
        child = child.next
    }
}

@Composable
@Preview(showBackground = true)
private fun MarkdownPreviewV2() {
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
