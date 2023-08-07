/*
 *
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.featureforms

object TestData {
    val inputValidationFeatureFormJson: String = """
      {
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
      "formElements": [
          {
              "type": "field",
              "label": "No_Value Multiline",
              "editableExpression": "expr/system/true",
              "fieldName": "No_Value",
              "inputType": {
                  "type": "text-area",
                  "maxLength": 255.0
              }
          },
          {
              "type": "field",
              "label": "Populated Short String",
              "editableExpression": "expr/system/true",
              "fieldName": "Populated",
              "inputType": {
                  "type": "text-area",
                  "maxLength": 255.0
              }
          },
          {
              "type": "field",
              "label": "Populated Long String",
              "editableExpression": "expr/system/true",
              "fieldName": "Populated_Multiline",
              "inputType": {
                  "type": "text-area",
                  "maxLength": 255.0
              }
          },
          {
              "type": "field",
              "label": "Populated_Max_Error (100)",
              "editableExpression": "expr/system/true",
              "fieldName": "Populated_Max_Error",
              "inputType": {
                  "type": "text-area",
                  "maxLength": 100.0
              }
          },
          {
              "type": "field",
              "label": "Populated_Constrained 10 to 90",
              "editableExpression": "expr/system/true",
              "fieldName": "Populated_Constrained",
              "inputType": {
                  "type": "text-area",
                  "maxLength": 90.0,
                  "minLength": 10.0
              }
          },
          {
              "type": "field",
              "label": "No_Value_Placeholder",
              "editableExpression": "expr/system/true",
              "fieldName": "No_Value_Placeholder",
              "inputType": {
                  "type": "text-area",
                  "maxLength": 255.0
              },
              "hint": "Hello this is a Placeholder !!!"
          },
          {
              "type": "field",
              "description": "This is a Description",
              "label": "No_Value_Description",
              "editableExpression": "expr/system/true",
              "fieldName": "No_Value_Description",
              "inputType": {
                  "type": "text-area",
                  "maxLength": 255.0
              }
          },
          {
              "type": "field",
              "label": "Read_Only",
              "fieldName": "Read_Only",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 255.0
              }
          },
          {
              "type": "field",
              "label": "Single Line No Value, Placeholder or Description",
              "editableExpression": "expr/system/true",
              "fieldName": "Single_Line_No_Value_Placeholder_or_Description",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 256.0
              }
          },
          {
              "type": "field",
              "label": "Populated Single Line",
              "editableExpression": "expr/system/true",
              "fieldName": "Populated_Single_Line",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 256.0
              }
          },
          {
              "type": "field",
              "label": "Populated Max Error ",
              "editableExpression": "expr/system/true",
              "fieldName": "Populated_Max_Error_SingleLine",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 10.0
              }
          },
          {
              "type": "field",
              "label": "NoPlaceholderordescription_required",
              "editableExpression": "expr/system/true",
              "fieldName": "NoPlaceholderordescription_required",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 256.0
              },
              "requiredExpression": "expr/system/true"
          },
          {
              "type": "field",
              "label": "NoPlaceholderDescription_CharConstrained",
              "editableExpression": "expr/system/true",
              "fieldName": "NoPlaceholderDescription_CharConstrained",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 25.0,
                  "minLength": 10.0
              }
          },
          {
              "type": "field",
              "label": "noPlaceholderDescription_CharCons_required",
              "editableExpression": "expr/system/true",
              "fieldName": "noPlaceholderDescription_CharCons_required",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 25.0,
                  "minLength": 10.0
              },
              "requiredExpression": "expr/system/true"
          },
          {
              "type": "field",
              "label": "NoPlaceholderDescription_CharConstrainedError",
              "editableExpression": "expr/system/true",
              "fieldName": "NoPlaceholderDescription_CharConstrainedError",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 25.0,
                  "minLength": 10.0
              }
          },
          {
              "type": "field",
              "description": "This is a description that is  very long the maximume lenght in fact  just typing until I can type no more and on and on , will this thing every stop letting me type, I think it will just keep on typing typein more and more until it stops and it ends in Z",
              "label": "No Value with Long Placeholder and Description",
              "editableExpression": "expr/system/true",
              "fieldName": "NoValueWithLongPlaceholderandDescription",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 256.0
              },
              "hint": "This is a placeholder that is the longest placeholder that it can be, I don;t know how long it can be, but it seem that it can be  very long as I am typing this out it does not seem to be stopping the amount that I can type, very odd that field maps designers would llet a string get so long they are notorious for not letting user have long strings in stuff that they create, very strange indeed. that input form is not stopping oh I give up!"
          },
          {
              "type": "field",
              "description": "In error because value is less than 10                                                                                                                                                                                                 and long description mmm",
              "label": "Placeholder and Description Min Lengh Error",
              "editableExpression": "expr/system/true",
              "fieldName": "Placeholder_and_Description_Min_Lengh_Error",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 256.0,
                  "minLength": 10.0
              },
              "hint": "Placeholder for min length error"
          },
          {
              "type": "field",
              "description": "THis is in error because the length is more than 25",
              "label": "Placeholder and Description Max Length Error",
              "editableExpression": "expr/system/true",
              "fieldName": "Placeholder_and_Description_Max_Length_Error",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 25.0
              },
              "hint": "A placeholder for max length error"
          },
          {
              "type": "field",
              "description": "This is a really long description xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxZ",
              "label": "Placholder and Descripton Required",
              "editableExpression": "expr/system/true",
              "fieldName": "Placholder_and_Descripton_Required",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 256.0
              },
              "requiredExpression": "expr/system/true",
              "hint": "This is a placeholder"
          },
          {
              "type": "field",
              "label": "Read Only No Value",
              "fieldName": "ReadONlyNoValue",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 256.0
              }
          },
          {
              "type": "field",
              "label": "Read Only Populated",
              "fieldName": "Read_Only_Populated",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 256.0
              }
          },
          {
              "type": "field",
              "description": "Description for ready only single line with no value",
              "label": "Read Only No Value Description",
              "fieldName": "Read_Only_No_Value_Description",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 256.0
              }
          },
          {
              "type": "field",
              "description": "Not sure this one makes sense,",
              "label": "Read Only No Value and Required",
              "fieldName": "Read_Only_No_Value_and_Required",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 256.0
              },
              "requiredExpression": "expr/system/true"
          },
          {
              "type": "field",
              "description": "This is a description for a Read Only Populated form",
              "label": "Read Only Populated with Description",
              "fieldName": "Read_Only_Populated_with_Description",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 256.0
              }
          },
          {
              "type": "field",
              "description": "More that 10 char in length",
              "label": "Read Only Form with Max Length Error",
              "fieldName": "Read_Only_Form_with_Max_Length_Error",
              "inputType": {
                  "type": "text-box",
                  "maxLength": 10.0
              }
          }
      ],
      "title": "InputValidation"
  }

    """.trimIndent()
}