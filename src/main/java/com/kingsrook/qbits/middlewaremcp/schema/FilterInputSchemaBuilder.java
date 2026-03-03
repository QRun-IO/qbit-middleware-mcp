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
import java.util.List;
import java.util.Map;


/*******************************************************************************
 ** Builds the JSON Schema for the filter/query input used by query_* and count_*
 ** MCP tools. Produces a schema that maps to QQueryFilter / QFilterCriteria.
 *******************************************************************************/
public class FilterInputSchemaBuilder
{

   /*******************************************************************************
    ** Build the complete input schema for a query or count tool.
    *******************************************************************************/
   public static Map<String, Object> buildQueryInputSchema()
   {
      Map<String, Object> schema = new LinkedHashMap<>();
      schema.put("type", "object");

      Map<String, Object> properties = new LinkedHashMap<>();

      // criteria array
      Map<String, Object> criteriaSchema   = buildCriteriaSchema();
      properties.put("criteria", criteriaSchema);

      // booleanOperator
      Map<String, Object> booleanOperator = new LinkedHashMap<>();
      booleanOperator.put("type", "string");
      booleanOperator.put("enum", List.of("AND", "OR"));
      booleanOperator.put("default", "AND");
      booleanOperator.put("description", "How to combine multiple criteria (default AND)");
      properties.put("booleanOperator", booleanOperator);

      // orderBys
      Map<String, Object> orderBysSchema = buildOrderBysSchema();
      properties.put("orderBys", orderBysSchema);

      // limit
      Map<String, Object> limitSchema = new LinkedHashMap<>();
      limitSchema.put("type", "integer");
      limitSchema.put("default", 50);
      limitSchema.put("maximum", 1000);
      limitSchema.put("description", "Maximum number of records to return");
      properties.put("limit", limitSchema);

      // skip
      Map<String, Object> skipSchema = new LinkedHashMap<>();
      skipSchema.put("type", "integer");
      skipSchema.put("default", 0);
      skipSchema.put("description", "Number of records to skip (for pagination)");
      properties.put("skip", skipSchema);

      schema.put("properties", properties);
      return schema;
   }



   /*******************************************************************************
    ** Build the schema for the criteria array.
    *******************************************************************************/
   private static Map<String, Object> buildCriteriaSchema()
   {
      Map<String, Object> criteriaSchema = new LinkedHashMap<>();
      criteriaSchema.put("type", "array");
      criteriaSchema.put("description", "List of filter conditions (combined via booleanOperator)");

      Map<String, Object> itemSchema = new LinkedHashMap<>();
      itemSchema.put("type", "object");

      Map<String, Object> itemProperties = new LinkedHashMap<>();

      Map<String, Object> fieldNameProp = new LinkedHashMap<>();
      fieldNameProp.put("type", "string");
      fieldNameProp.put("description", "Name of the field to filter on");
      itemProperties.put("fieldName", fieldNameProp);

      Map<String, Object> operatorProp = new LinkedHashMap<>();
      operatorProp.put("type", "string");
      operatorProp.put("enum", List.of(
         "EQUALS", "NOT_EQUALS",
         "IN", "NOT_IN",
         "STARTS_WITH", "ENDS_WITH", "CONTAINS",
         "LESS_THAN", "GREATER_THAN",
         "LESS_THAN_OR_EQUALS", "GREATER_THAN_OR_EQUALS",
         "IS_BLANK", "IS_NOT_BLANK",
         "BETWEEN"
      ));
      operatorProp.put("description", "Comparison operator");
      itemProperties.put("operator", operatorProp);

      Map<String, Object> valuesProp = new LinkedHashMap<>();
      valuesProp.put("type", "array");
      valuesProp.put("description", "Values to compare against (strings are coerced to the field's type). Use 2 values for BETWEEN.");
      Map<String, Object> valuesItems = new LinkedHashMap<>();
      valuesItems.put("type", "string");
      valuesProp.put("items", valuesItems);
      itemProperties.put("values", valuesProp);

      itemSchema.put("properties", itemProperties);
      itemSchema.put("required", List.of("fieldName", "operator"));
      criteriaSchema.put("items", itemSchema);

      return criteriaSchema;
   }



   /*******************************************************************************
    ** Build the schema for the orderBys array.
    *******************************************************************************/
   private static Map<String, Object> buildOrderBysSchema()
   {
      Map<String, Object> orderBysSchema = new LinkedHashMap<>();
      orderBysSchema.put("type", "array");
      orderBysSchema.put("description", "List of sort order specifications");

      Map<String, Object> itemSchema = new LinkedHashMap<>();
      itemSchema.put("type", "object");

      Map<String, Object> itemProperties = new LinkedHashMap<>();

      Map<String, Object> fieldNameProp = new LinkedHashMap<>();
      fieldNameProp.put("type", "string");
      fieldNameProp.put("description", "Field name to sort by");
      itemProperties.put("fieldName", fieldNameProp);

      Map<String, Object> isAscendingProp = new LinkedHashMap<>();
      isAscendingProp.put("type", "boolean");
      isAscendingProp.put("default", true);
      isAscendingProp.put("description", "true for ascending (A→Z, 1→9), false for descending");
      itemProperties.put("isAscending", isAscendingProp);

      itemSchema.put("properties", itemProperties);
      itemSchema.put("required", List.of("fieldName"));
      orderBysSchema.put("items", itemSchema);

      return orderBysSchema;
   }

}
