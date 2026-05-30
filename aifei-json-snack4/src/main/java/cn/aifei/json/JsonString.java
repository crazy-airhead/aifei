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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.noear.snack4.ONode;
import org.noear.snack4.codec.TypeRef;

/**
 * JsonString 将 json 字符串转换为 Java 对象。
 */
public class JsonString {

	static final ThreadLocal<JsonString> JSON_STRING_LOCAL = new ThreadLocal<>();

	static boolean defaultCamelToSnake = true;
	static boolean defaultModelAsRow = false;
	static String defaultDateFormat = null;

	Boolean camelToSnake;
	Boolean modelAsRow;

	final String text;

	public JsonString(String text) {
		this.text = text;
	}

	/**
	 * json 字符串转换为 Class type 参数指定的 Java 对象
	 */
	@SuppressWarnings("unchecked")
	public <T> T to(Class<T> type) {
		try {
			JSON_STRING_LOCAL.set(this);

			// Row 类型直接处理
			if (type == Row.class && JsonKit.aifeiDbPresent) {
				ONode node = ONode.ofJson(text);
				Map<String, Object> map = (Map<String, Object>) node.toBean(Map.class);
				boolean cs = Cpc.getCamelToSnake(this);
				return (T) RowReader.toRow(map, cs);
			}

			// Model + modelAsRow 处理
			if (JsonKit.aifeiDbPresent && Cpc.getModelAsRow(this)) {
				ONode node = ONode.ofJson(text);
				Map<String, Object> map = (Map<String, Object>) node.toBean(Map.class);
				Object model = JsonKit.readPreprocessor.apply(type, map);
				if (model != null) {
					return (T) model;
				}
			}

			// 默认使用 snack4 反序列化
			return ONode.deserialize(text, type);
		} finally {
			JSON_STRING_LOCAL.remove();
		}
	}

	/**
	 * json 字符串转换为 Java List 对象
	 */
	@SuppressWarnings("unchecked")
	public <E> List<E> toList(Class<E> elemType) {
		try {
			JSON_STRING_LOCAL.set(this);
			ONode node = ONode.ofJson(text);
			List<E> list = new ArrayList<>();

			int size = node.size();
			for (int i = 0; i < size; i++) {
				ONode item = node.get(i);

				// Row 类型直接处理
				if (elemType == Row.class && JsonKit.aifeiDbPresent) {
					Map<String, Object> map = (Map<String, Object>) item.toBean(Map.class);
					boolean cs = Cpc.getCamelToSnake(this);
					list.add((E) RowReader.toRow(map, cs));
					continue;
				}

				// Model + modelAsRow 处理
				if (JsonKit.aifeiDbPresent && Cpc.getModelAsRow(this)) {
					Map<String, Object> map = (Map<String, Object>) item.toBean(Map.class);
					Object model = JsonKit.readPreprocessor.apply(elemType, map);
					if (model != null) {
						list.add((E) model);
						continue;
					}
				}

				// 默认使用 snack4 转换
				list.add(item.toBean(elemType));
			}
			return list;
		} finally {
			JSON_STRING_LOCAL.remove();
		}
	}

	/**
	 * json 字符串转换为 Java Map 对象
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> toMap() {
		try {
			JSON_STRING_LOCAL.set(this);
			ONode node = ONode.ofJson(text);
			return (Map<String, Object>) node.toBean(Map.class);
		} finally {
			JSON_STRING_LOCAL.remove();
		}
	}

	/**
	 * json 字符串按 TypeRef 参数指定类型转换为 Java 对象，支持复杂泛型结构。
	 *
	 * <pre>
	 * 例子：
	 *   String json = "{\"u1\": {\"name\": \"James\", \"age\": 28}, \"u2\": {\"name\": \"Alice\", \"age\": 18}}";
	 *   Map<String, User> map = Json.of(json).to(new TypeRef<Map<String, User>>() {});
	 *   map.forEach((k, v) -> {
	 *       System.out.println(k + " -> " + v.getName() + ", " + v.getAge());
	 *   });
	 *
	 * 注：Java 的泛型在运行时会被擦除，使用 TypeRef 保留泛型信息转换为指定的类型。
	 * </pre>
	 */
	public <T> T to(TypeRef<T> typeRef) {
		try {
			JSON_STRING_LOCAL.set(this);
			return ONode.deserialize(text, typeRef, JsonKit.baseOptions);
		} finally {
			JSON_STRING_LOCAL.remove();
		}
	}

	/**
	 * 通过函数充分利用 snack4 API 定制转换逻辑，并可在 RowReader 中用上存放在
	 * ThreadLocal 内的 JsonString.camelToSnake/modelAsRow 等参数。
	 *
	 * <pre>
	 * 例子：
	 *    String json = "{\"name\": \"James\"}";
	 *    User user = Json.of(json).to(text -> {
	 *        return ONode.deserialize(text, User.class);
	 *    });
	 *    System.out.println(user.getName());
	 * </pre>
	 */
	public <T> T to(Function<String, T> fun) {
		try {
			JSON_STRING_LOCAL.set(this);
			return fun.apply(text);
		} finally {
			JSON_STRING_LOCAL.remove();
		}
	}

	@Override
  public String toString() {
		return text;
	}

	/**
	 * 设置 camelToSnake 为 true。仅针对 aifei-db 内的 Row 有效。
	 */
	public JsonString camelToSnake() {
		camelToSnake = true;
		return this;
	}

	/**
	 * 设置 camelToSnake。仅针对 aifei-db 内的 Row 有效。
	 */
	public JsonString camelToSnake(boolean enabled) {
		camelToSnake = enabled;
		return this;
	}

	/**
	 * 设置 modelAsRow 为 true，即将 aifei-db 中的 Model 当成 Row 转换，而非调用 setter 方法转换。
	 */
	public JsonString modelAsRow() {
		modelAsRow = true;
		return this;
	}

	/**
	 * 设置 modelAsRow，即将 aifei-db 中的 Model 当成 Row 转换，而非调用 setter 方法转换。
	 */
	public JsonString modelAsRow(boolean enabled) {
		modelAsRow = enabled;
		return this;
	}
}
