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

package com.kingsrook.qbits.middlewaremcp.tools;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qbits.middlewaremcp.QMCPMetaData;
import com.kingsrook.qbits.middlewaremcp.session.MCPSessionProviderInterface;


/*******************************************************************************
 ** Tool handler for "query_{tableName}" — searches/filters records in a table.
 *******************************************************************************/
public class QueryToolHandler extends AbstractQQQToolHandler
{
   private final String tableName;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QueryToolHandler(QInstance qInstance, QMCPMetaData mcpMetaData, MCPSessionProviderInterface sessionProvider, String tableName)
   {
      super(qInstance, mcpMetaData, sessionProvider);
      this.tableName = tableName;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected Object doExecute(Map<String, Object> arguments) throws QException
   {
      int limit = getIntArg(arguments, "limit", mcpMetaData.getDefaultQueryLimit());
      int skip  = getIntArg(arguments, "skip", 0);

      // Enforce max limit
      limit = Math.min(limit, mcpMetaData.getMaxQueryLimit());

      QQueryFilter filter = buildFilter(arguments);
      filter.setLimit(limit);
      filter.setSkip(skip);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      queryInput.setFilter(filter);
      queryInput.setShouldGenerateDisplayValues(true);
      queryInput.setShouldTranslatePossibleValues(true);

      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("records", recordsToList(queryOutput.getRecords()));
      result.put("count", queryOutput.getRecords().size());
      result.put("pageInfo", Map.of("limit", limit, "skip", skip));
      return result;
   }



   /*******************************************************************************
    ** Build a QQueryFilter from MCP arguments.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   static QQueryFilter buildFilter(Map<String, Object> arguments)
   {
      QQueryFilter filter = new QQueryFilter();

      // Parse booleanOperator
      String booleanOperatorStr = arguments.containsKey("booleanOperator") ? String.valueOf(arguments.get("booleanOperator")) : "AND";
      if("OR".equalsIgnoreCase(booleanOperatorStr))
      {
         filter.setBooleanOperator(QQueryFilter.BooleanOperator.OR);
      }
      else
      {
         filter.setBooleanOperator(QQueryFilter.BooleanOperator.AND);
      }

      // Parse criteria
      Object criteriaObj = arguments.get("criteria");
      if(criteriaObj instanceof List)
      {
         for(Object criterionObj : (List<?>) criteriaObj)
         {
            if(criterionObj instanceof Map)
            {
               Map<String, Object> criterion = (Map<String, Object>) criterionObj;
               String              fieldName = (String) criterion.get("fieldName");
               String              opStr     = (String) criterion.get("operator");
               List<Object>        rawValues = criterion.containsKey("values") ? (List<Object>) criterion.get("values") : List.of();

               if(fieldName != null && opStr != null)
               {
                  QCriteriaOperator operator = parseOperator(opStr);
                  if(operator != null)
                  {
                     List<Serializable> values = new ArrayList<>();
                     for(Object v : rawValues)
                     {
                        values.add(v == null ? null : String.valueOf(v));
                     }
                     filter.addCriteria(new QFilterCriteria(fieldName, operator, values));
                  }
               }
            }
         }
      }

      // Parse orderBys
      Object orderBysObj = arguments.get("orderBys");
      if(orderBysObj instanceof List)
      {
         for(Object orderByObj : (List<?>) orderBysObj)
         {
            if(orderByObj instanceof Map)
            {
               Map<String, Object> orderByMap  = (Map<String, Object>) orderByObj;
               String              fieldName   = (String) orderByMap.get("fieldName");
               Object              isAscending = orderByMap.get("isAscending");
               boolean             ascending   = isAscending == null || Boolean.TRUE.equals(isAscending) || "true".equals(String.valueOf(isAscending));
               if(fieldName != null)
               {
                  filter.addOrderBy(new QFilterOrderBy(fieldName, ascending));
               }
            }
         }
      }

      return filter;
   }



   /*******************************************************************************
    ** Map MCP operator strings to QCriteriaOperator enum values.
    *******************************************************************************/
   private static QCriteriaOperator parseOperator(String opStr)
   {
      return switch(opStr.toUpperCase())
      {
         case "EQUALS" -> QCriteriaOperator.EQUALS;
         case "NOT_EQUALS" -> QCriteriaOperator.NOT_EQUALS;
         case "IN" -> QCriteriaOperator.IN;
         case "NOT_IN" -> QCriteriaOperator.NOT_IN;
         case "STARTS_WITH" -> QCriteriaOperator.STARTS_WITH;
         case "ENDS_WITH" -> QCriteriaOperator.ENDS_WITH;
         case "CONTAINS" -> QCriteriaOperator.CONTAINS;
         case "LESS_THAN" -> QCriteriaOperator.LESS_THAN;
         case "GREATER_THAN" -> QCriteriaOperator.GREATER_THAN;
         case "LESS_THAN_OR_EQUALS" -> QCriteriaOperator.LESS_THAN_OR_EQUALS;
         case "GREATER_THAN_OR_EQUALS" -> QCriteriaOperator.GREATER_THAN_OR_EQUALS;
         case "IS_BLANK" -> QCriteriaOperator.IS_BLANK;
         case "IS_NOT_BLANK" -> QCriteriaOperator.IS_NOT_BLANK;
         case "BETWEEN" -> QCriteriaOperator.BETWEEN;
         default -> null;
      };
   }

}
