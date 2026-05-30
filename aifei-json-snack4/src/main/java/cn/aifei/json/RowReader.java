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
import java.util.Map;

/**
 * RowReader 用于实现 Map 转换为 aifei-db 的 Row 对象，支持可选的驼峰转下划线。
 */
public class RowReader {

	static final ComputeCache<String, String> KEY_CACHE = new ComputeCache<>(1024);

	/**
	 * 将 Map 转换为 Row，可选驼峰转下划线。
	 */
	@SuppressWarnings("unchecked")
	public static Row toRow(Object mapObj, boolean camelToSnake) {
		if (mapObj == null) {
			return null;
		}

		if (mapObj instanceof Row) {
			return (Row) mapObj;
		}

		if (!(mapObj instanceof Map)) {
			return null;
		}

		Map<String, Object> map = (Map<String, Object>) mapObj;
		Row row = new Row();
		if (camelToSnake) {
			for (Map.Entry<String, Object> e : map.entrySet()) {
				String snakeName = KEY_CACHE.computeIfAbsent(e.getKey(), RowReader::camelToSnake);
				row.setOrPut(snakeName, e.getValue());
			}
		} else {
			map.forEach((k, v) -> row.setOrPut(k, v));
		}
		return row;
	}

	// 驼峰转下划线风格
	static String camelToSnake(String fieldName) {
		if (fieldName == null || fieldName.isEmpty()) {
			return fieldName;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0, len = fieldName.length(); i < len; i++) {
			char c = fieldName.charAt(i);
			if (Character.isUpperCase(c)) {
				if (i > 0) {
					sb.append('_');
				}
				sb.append(Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
