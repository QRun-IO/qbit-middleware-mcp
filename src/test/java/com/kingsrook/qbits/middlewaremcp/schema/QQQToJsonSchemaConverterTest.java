/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qbits.middlewaremcp.schema;


import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for QQQToJsonSchemaConverter — verifies field type mappings.
 *******************************************************************************/
class QQQToJsonSchemaConverterTest
{
   private final QQQToJsonSchemaConverter converter = new QQQToJsonSchemaConverter();



   /*******************************************************************************
    ** Test STRING type maps to JSON Schema "string".
    *******************************************************************************/
   @Test
   void testStringType()
   {
      Map<String, Object> property = converter.fieldToJsonSchemaProperty(new QFieldMetaData("name", QFieldType.STRING));
      assertThat(property.get("type")).isEqualTo("string");
   }



   /*******************************************************************************
    ** Test INTEGER type maps to JSON Schema "integer".
    *******************************************************************************/
   @Test
   void testIntegerType()
   {
      Map<String, Object> property = converter.fieldToJsonSchemaProperty(new QFieldMetaData("age", QFieldType.INTEGER));
      assertThat(property.get("type")).isEqualTo("integer");
   }



   /*******************************************************************************
    ** Test LONG type maps to JSON Schema "integer" with format "int64".
    *******************************************************************************/
   @Test
   void testLongType()
   {
      Map<String, Object> property = converter.fieldToJsonSchemaProperty(new QFieldMetaData("bigNum", QFieldType.LONG));
      assertThat(property.get("type")).isEqualTo("integer");
      assertThat(property.get("format")).isEqualTo("int64");
   }



   /*******************************************************************************
    ** Test DECIMAL type maps to JSON Schema "number".
    *******************************************************************************/
   @Test
   void testDecimalType()
   {
      Map<String, Object> property = converter.fieldToJsonSchemaProperty(new QFieldMetaData("price", QFieldType.DECIMAL));
      assertThat(property.get("type")).isEqualTo("number");
   }



   /*******************************************************************************
    ** Test BOOLEAN type maps to JSON Schema "boolean".
    *******************************************************************************/
   @Test
   void testBooleanType()
   {
      Map<String, Object> property = converter.fieldToJsonSchemaProperty(new QFieldMetaData("active", QFieldType.BOOLEAN));
      assertThat(property.get("type")).isEqualTo("boolean");
   }



   /*******************************************************************************
    ** Test DATE type maps to JSON Schema "string" with format "date".
    *******************************************************************************/
   @Test
   void testDateType()
   {
      Map<String, Object> property = converter.fieldToJsonSchemaProperty(new QFieldMetaData("birthDate", QFieldType.DATE));
      assertThat(property.get("type")).isEqualTo("string");
      assertThat(property.get("format")).isEqualTo("date");
   }



   /*******************************************************************************
    ** Test DATE_TIME type maps to JSON Schema "string" with format "date-time".
    *******************************************************************************/
   @Test
   void testDateTimeType()
   {
      Map<String, Object> property = converter.fieldToJsonSchemaProperty(new QFieldMetaData("createdAt", QFieldType.DATE_TIME));
      assertThat(property.get("type")).isEqualTo("string");
      assertThat(property.get("format")).isEqualTo("date-time");
   }



   /*******************************************************************************
    ** Test BLOB type maps to JSON Schema "string" with contentEncoding "base64".
    *******************************************************************************/
   @Test
   void testBlobType()
   {
      Map<String, Object> property = converter.fieldToJsonSchemaProperty(new QFieldMetaData("data", QFieldType.BLOB));
      assertThat(property.get("type")).isEqualTo("string");
      assertThat(property.get("contentEncoding")).isEqualTo("base64");
   }



   /*******************************************************************************
    ** Test TEXT type maps to JSON Schema "string".
    *******************************************************************************/
   @Test
   void testTextType()
   {
      Map<String, Object> property = converter.fieldToJsonSchemaProperty(new QFieldMetaData("notes", QFieldType.TEXT));
      assertThat(property.get("type")).isEqualTo("string");
   }



   /*******************************************************************************
    ** Test that a field label is included as "title".
    *******************************************************************************/
   @Test
   void testFieldLabel()
   {
      QFieldMetaData field = new QFieldMetaData("firstName", QFieldType.STRING).withLabel("First Name");
      Map<String, Object> property = converter.fieldToJsonSchemaProperty(field);
      assertThat(property.get("title")).isEqualTo("First Name");
   }



   /*******************************************************************************
    ** Test that possible value source name is included in description.
    *******************************************************************************/
   @Test
   void testPossibleValueSource()
   {
      QFieldMetaData field = new QFieldMetaData("statusId", QFieldType.INTEGER);
      field.setPossibleValueSourceName("status");
      Map<String, Object> property = converter.fieldToJsonSchemaProperty(field);
      assertThat((String) property.get("description")).contains("status");
   }



   /*******************************************************************************
    ** Test that hidden fields are excluded from the properties map.
    *******************************************************************************/
   @Test
   void testHiddenFieldExcluded()
   {
      QFieldMetaData visibleField = new QFieldMetaData("name", QFieldType.STRING);
      QFieldMetaData hiddenField  = new QFieldMetaData("secret", QFieldType.STRING).withIsHidden(true);

      Map<String, Object> properties = converter.fieldsToJsonSchemaProperties(java.util.List.of(visibleField, hiddenField));
      assertThat(properties).containsKey("name");
      assertThat(properties).doesNotContainKey("secret");
   }

}
