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


import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;


/*******************************************************************************
 ** Converts QFieldMetaData to JSON Schema property objects for use in MCP
 ** tool input schemas (insert/update tools).
 *******************************************************************************/
public class QQQToJsonSchemaConverter
{

   /*******************************************************************************
    ** Convert a single QFieldMetaData to a JSON Schema property map.
    *******************************************************************************/
   public Map<String, Object> fieldToJsonSchemaProperty(QFieldMetaData field)
   {
      Map<String, Object> property = new LinkedHashMap<>();
      QFieldType          type     = field.getType();

      if(type == null)
      {
         property.put("type", "string");
         return property;
      }

      switch(type)
      {
         case STRING, TEXT, HTML ->
         {
            property.put("type", "string");
         }
         case PASSWORD ->
         {
            property.put("type", "string");
            property.put("format", "password");
         }
         case INTEGER ->
         {
            property.put("type", "integer");
         }
         case LONG ->
         {
            property.put("type", "integer");
            property.put("format", "int64");
         }
         case DECIMAL ->
         {
            property.put("type", "number");
         }
         case BOOLEAN ->
         {
            property.put("type", "boolean");
         }
         case DATE ->
         {
            property.put("type", "string");
            property.put("format", "date");
         }
         case DATE_TIME ->
         {
            property.put("type", "string");
            property.put("format", "date-time");
         }
         case TIME ->
         {
            property.put("type", "string");
            property.put("format", "time");
         }
         case BLOB ->
         {
            property.put("type", "string");
            property.put("contentEncoding", "base64");
         }
         default ->
         {
            property.put("type", "string");
         }
      }

      if(field.getLabel() != null)
      {
         property.put("title", field.getLabel());
      }

      if(field.getPossibleValueSourceName() != null)
      {
         property.put("description", "Possible values from source: " + field.getPossibleValueSourceName());
      }

      return property;
   }



   /*******************************************************************************
    ** Build a JSON Schema "properties" map for a collection of fields.
    *******************************************************************************/
   public Map<String, Object> fieldsToJsonSchemaProperties(Iterable<QFieldMetaData> fields)
   {
      Map<String, Object> properties = new LinkedHashMap<>();
      for(QFieldMetaData field : fields)
      {
         if(!field.getIsHidden())
         {
            properties.put(field.getName(), fieldToJsonSchemaProperty(field));
         }
      }
      return properties;
   }

}
