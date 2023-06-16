package com.arcgismaps.toolkit.featureforms.api

import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.mapping.layers.FeatureLayer

public class TestData {
    public companion object {
        public val formInfo: String = """
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
    }
}

public val ArcGISFeatureTable.formInfoJson: String
    get() {
        return unsupportedJson["formInfo"].toString()
    }

public val FeatureLayer.formInfoJson: String
    get() {
        return unsupportedJson["formInfo"].toString()
    }
