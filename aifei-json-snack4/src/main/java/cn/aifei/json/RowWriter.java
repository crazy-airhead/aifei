/*
 * Copyright 2011-2035 詹波 (aifei.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.aifei.json;

import cn.aifei.db.core.Row;
import cn.aifei.util.ComputeCache;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RowWriter 用于实现 aifei-db 的 Row 对象转换为 Map，支持可选的下划线转驼峰。
 *
 * @author airhead
 */
public class RowWriter {

  static final ComputeCache<String, String> KEY_CACHE = new ComputeCache<>(1024);

  /** 将 Row 的数据转换为 Map，可选下划线转驼峰。 */
  public static Map<String, Object> toMap(Row row, boolean snakeToCamel, boolean lowerBeforeCamel) {
    Map<String, Object> data = row.data();
    if (data == null || data.isEmpty()) {
      return new LinkedHashMap<>();
    }

    Map<String, Object> result = new LinkedHashMap<>(data.size());
    if (snakeToCamel) {
      for (Map.Entry<String, Object> e : data.entrySet()) {
        String key = lowerBeforeCamel ? e.getKey() + '1' : e.getKey();
        String camelName =
            KEY_CACHE.computeIfAbsent(key, doNotUse -> snakeToCamel(e.getKey(), lowerBeforeCamel));
        result.put(camelName, e.getValue());
      }
    } else {
      result.putAll(data);
    }
    return result;
  }

  // 下划线转驼峰风格
  static String snakeToCamel(String fieldName, boolean lowerCase) {
    if (fieldName == null) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    boolean toUpperCase = false;

    for (int i = 0, len = fieldName.length(); i < len; i++) {
      char c = fieldName.charAt(i);
      if (c == '_') {
        toUpperCase = true;
      } else {
        if (toUpperCase) {
          sb.append(Character.toUpperCase(c));
          toUpperCase = false;
        } else {
          if (lowerCase) {
            sb.append(Character.toLowerCase(c));
          } else {
            sb.append(c);
          }
        }
      }
    }

    return sb.toString();
  }
}
