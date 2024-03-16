package com.arcgismaps.toolkit.featureforms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult

fun SemanticsNodeInteraction.getAnnotatedTextString(): AnnotatedString {
    val textList = fetchSemanticsNode().config.first {
        it.key.name == "Text"
    }.value as List<*>
    return textList.first() as AnnotatedString
}

fun SemanticsNodeInteraction.getTextString(): String {
    return getAnnotatedTextString().text
}

fun SemanticsNodeInteraction.assertTextColor(
    color: Color
): SemanticsNodeInteraction = assert(isOfColor(color))

private fun isOfColor(color: Color): SemanticsMatcher = SemanticsMatcher(
    "${SemanticsProperties.Text.name} is of color '$color'"
) {
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    it.config.getOrNull(SemanticsActions.GetTextLayoutResult)
        ?.action
        ?.invoke(textLayoutResults)
    return@SemanticsMatcher if (textLayoutResults.isEmpty()) {
        false
    } else {
        textLayoutResults.first().layoutInput.style.color == color
    }
}

private fun SemanticsNodeInteractionCollection.onChildWithText(value: String, recurse: Boolean = false): SemanticsNodeInteraction? {
    val count = fetchSemanticsNodes().count()
    
    for (i in 0 until count) {
        var node: SemanticsNodeInteraction? = null
        val semanticsNodeInteraction = get(i)
        val semanticsNode = semanticsNodeInteraction.fetchSemanticsNode()
        if (semanticsNode.config.getOrNull(SemanticsProperties.Text)?.toList()?.map
            {
                it.text
            }?.contains(value) == true
        ) {
            node =  semanticsNodeInteraction
        } else if (semanticsNode.config.getOrNull(SemanticsProperties.EditableText)?.contains(value) == true) {
            node = semanticsNodeInteraction
        } else if (recurse) {
            node = semanticsNodeInteraction.onChildren().onChildWithText(value, true)
        }
        
        if (node != null) {
            return node
        }
    }
    
    return null
}
/**
 * Returns the child node with the given text [value].
 *
 * @param value the text string for which to search.
 * @param recurse if true will recurse through the whole semantic node hierarchy.
 * @throws AssertionError if the child with the given text does not exist.
 */
@Throws(AssertionError::class)
internal fun SemanticsNodeInteraction.onChildWithText(value: String, recurse: Boolean = false): SemanticsNodeInteraction {
    return onChildren().onChildWithText(value, recurse)
        ?:  throw AssertionError("No node exists with the given text : $value")
}

/**
 * Returns the child node with the given content description [value].
 *
 * @param value the content description string for which to search.
 * @param recurse if true will recurse through the whole semantic node hierarchy.
 * @throws AssertionError if the child with the content description does not exist.
 */
@Throws(AssertionError::class)
internal fun SemanticsNodeInteraction.onChildWithContentDescription(
    value: String,
    recurse: Boolean = false
): SemanticsNodeInteraction = onChildren().onChildWithContentDescription(value, recurse)
    ?: throw AssertionError("No node exists with the given content description : $value")

private fun SemanticsNodeInteractionCollection.onChildWithContentDescription(value: String, recurse: Boolean = false): SemanticsNodeInteraction? {
    val count = fetchSemanticsNodes().count()
    
    for (i in 0 until count) {
        var node: SemanticsNodeInteraction? = null
        val semanticsNodeInteraction = get(i)
        val semanticsNode = semanticsNodeInteraction.fetchSemanticsNode()
        if (semanticsNode.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(value) == true) {
            node =  semanticsNodeInteraction
        } else if (recurse) {
            node = semanticsNodeInteraction.onChildren().onChildWithContentDescription(value, true)
        }
        
        if (node != null) {
            return node
        }
    }
    
    return null
}

/**
 * Asserts equality with the editable text
 * @param value the expected editable text
 */
internal fun SemanticsNodeInteraction.assertEditableTextEquals(
    value: String
): SemanticsNodeInteraction =
    assert(hasEditableText(value))

/**
 * Creates a semantic matcher to match equality with the editable text
 *
 * @param textValue the text to match
 * @return a SemanticsMatcher to match the editable text of a SemanticsNodeInteraction.
 */
internal fun hasEditableText(
    textValue: String
): SemanticsMatcher {
    val propertyName = "${SemanticsProperties.Text.name} + ${SemanticsProperties.EditableText.name}"

    return SemanticsMatcher(
        "$propertyName = [${SemanticsProperties.EditableText.name}]"
    ) { node ->
        var actual = ""
        node.config.getOrNull(SemanticsProperties.EditableText) ?.let { actual = it.text }
            ?: throw IllegalStateException("expected an editable text in the semantics node")

        actual == textValue
    }
}
