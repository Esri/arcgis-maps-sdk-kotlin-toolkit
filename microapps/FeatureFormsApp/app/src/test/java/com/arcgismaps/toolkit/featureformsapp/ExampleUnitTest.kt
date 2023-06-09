package com.arcgismaps.toolkit.featureformsapp

import com.arcgismaps.toolkit.featureformsapp.featureform.FeatureFormDefinition
import com.arcgismaps.toolkit.featureformsapp.featureform.FieldFeatureFormElement
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test



/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    
    val formInfo = """
        {
                "formElements": [
                    {
                        "description": "Description TextBox",
                        "label": "TextBox - Single Line",
                        "type": "field",
                        "editableExpression": "expr/system/true",
                        "fieldName": "TextBox_Single_Line",
                        "hint": "Placeholder Text",
                        "inputType": {
                            "type": "text-box",
                            "maxLength": 256,
                            "minLength": 0
                        }
                    },
                    {
                        "description": "Description for TextArea",
                        "label": "TextArea - Multiline",
                        "type": "field",
                        "editableExpression": "expr/system/true",
                        "fieldName": "TextArea_Multiline",
                        "hint": "Placeholder Text",
                        "inputType": {
                            "type": "text-area",
                            "maxLength": 1000,
                            "minLength": 0
                        }
                    }
                ],
                "expressionInfos": [
                    {
                        "expression": "false",
                        "name": "expr/system/false",
                        "returnType": "boolean",
                        "title": "False"
                    },
                    {
                        "expression": "true",
                        "name": "expr/system/true",
                        "returnType": "boolean",
                        "title": "True"
                    }
                ],
                "title": "PointFeature"
            }
    """.trimIndent()
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testFormInfoFromJson() = runTest {
        val featureFormInfo = FeatureFormDefinition.fromJsonOrNull(formInfo)
        assertThat(featureFormInfo).isNotNull()
        
        assertThat(featureFormInfo?.formElements).isNotNull()
        val formElements = featureFormInfo?.let {
            it.formElements
        } ?: run {
            fail("formInfo should not be null")
            listOf()
        }
        assertThat(formElements.isNotEmpty()).isTrue()
        assertThat(formElements.size).isEqualTo(2)
        val element = formElements.first()
        assertThat(element).isInstanceOf(FieldFeatureFormElement::class.java)
        
    }
    
}
