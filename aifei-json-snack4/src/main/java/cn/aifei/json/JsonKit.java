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
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.noear.snack4.ONode;
import org.noear.snack4.Options;
import org.noear.snack4.Feature;

/**
 * JsonKit.
 *
 * <pre>
 * JsonKit 使用说明：
 *  1: 以 "setRead" 打头的方法配置 json 字符串转换为 Java 对象的全局默认参数。
 *
 *  2: 以 "setWrite" 打头的方法配置 Java 对象转换为 json 字符串的全局默认参数。
 *
 *  3: json 模块针对 aifei-db 的 Row 以及 Model(继承自 AifeiRow) 的 json 转换进行增强，
 *     以便适用于各种应用场景。
 * </pre>
 */
public class JsonKit {

	static JsonFactory jsonFactory = new JsonFactory();
	static Options baseOptions;
	static boolean aifeiDbPresent = false;

	// Model 预处理器，仅在 aifei-db 存在时设置
	static Function<Object, Object> writePreprocessor = Function.identity();
	static BiFunction<Class<?>, Map<String, Object>, Object> readPreprocessor = (type, map) -> null;

	private JsonKit() {
		registerAifeiDbRowAndModel();
	}

	static JsonKit kit = new JsonKit();

	public static JsonKit get() {
		return kit;
	}

	/**
	 * 注册 aifei-db 的 Row 与 Model(继承自 AifeiRow)。
	 */
	void registerAifeiDbRowAndModel() {
		try {
			Class.forName("cn.aifei.db.core.Row");
			aifeiDbPresent = true;

			baseOptions = Options.of();

			// Row encoder：将 Row 数据转为 Map，可选下划线转驼峰
			baseOptions.addEncoder(Row.class, (ctx, row, target) -> {
				JsonObject jo = Cpc.getThreadLocalJsonObject();
				boolean sc = jo != null && Cpc.getSnakeToCamel(jo);
				boolean lc = jo != null && Cpc.getLowerBeforeCamel(jo);
				Map<String, Object> map = RowWriter.toMap(row, sc, lc);
				return ONode.ofBean(map, ctx.getOptions());
			});

			// Row decoder：将 ONode 转为 Row，可选驼峰转下划线
			baseOptions.addDecoder(Row.class, (ctx, node) -> {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) node.toBean(Map.class);
				JsonString js = Cpc.getThreadLocalJsonString();
				boolean cs = js != null && Cpc.getCamelToSnake(js);
				return RowReader.toRow(map, cs);
			});

			// Model write 预处理器：将 AifeiRow 转为 Row
			writePreprocessor = obj -> AifeiDbSupport.toRowIfNeeded(obj, Cpc.getThreadLocalJsonObject());

			// Model read 预处理器：将 Map 转为 AifeiRow 子类
			readPreprocessor = (type, map) -> {
				JsonString js = Cpc.getThreadLocalJsonString();
				return AifeiDbSupport.toModelIfNeeded(type, map, js);
			};

		} catch (Exception ignored) {
			baseOptions = Options.of();
		}
	}

	/**
	 * 注册 Row encoder/decoder 到指定的 Options。
	 */
	static void registerRowCodec(Options opts) {
		opts.addEncoder(Row.class, (ctx, row, target) -> {
			JsonObject jo = Cpc.getThreadLocalJsonObject();
			boolean sc = jo != null && Cpc.getSnakeToCamel(jo);
			boolean lc = jo != null && Cpc.getLowerBeforeCamel(jo);
			Map<String, Object> map = RowWriter.toMap(row, sc, lc);
			return ONode.ofBean(map, ctx.getOptions());
		});

		opts.addDecoder(Row.class, (ctx, node) -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) node.toBean(Map.class);
			JsonString js = Cpc.getThreadLocalJsonString();
			boolean cs = js != null && Cpc.getCamelToSnake(js);
			return RowReader.toRow(map, cs);
		});
	}

	/**
	 * 构建 Options，可选包含 dateFormat 和 prettyFormat。
	 * 当两者都不需要时返回共享的 baseOptions。
	 */
	static Options getOptions(String dateFormat, boolean prettyFormat) {
		if (dateFormat == null && !prettyFormat) {
			return baseOptions;
		}

		Options opts = Options.of();
		if (dateFormat != null) {
			opts.addFeatures(Feature.Write_UseDateFormat).dateFormat(dateFormat);
		}
		if (prettyFormat) {
			opts.writeIndent("  ");
		}
		if (aifeiDbPresent) {
			registerRowCodec(opts);
		}
		return opts;
	}

	// -------- 配置方法 --------

	/**
	 * 设置 json 字符串转换为 aifei-db 的 Row 时，是否将字段名由驼峰转换为下划线。
	 * 默认值为 true。
	 */
	public JsonKit setReadRowFieldCamelToSnake(boolean enabled) {
		JsonString.defaultCamelToSnake = enabled;
		return this;
	}

	/**
	 * 设置 json 字符串转换为 aifei-db 的 Model 时，是否当成 Row 去转换，而非调用 setter 方法转换。
	 * 默认值为 false。
	 */
	public JsonKit setReadModelAsRow(boolean enabled) {
		JsonString.defaultModelAsRow = enabled;
		return this;
	}

	/**
	 * 设置 aifei-db 的 Row 转换为 json 字符串时，是否将字段名由下划线转换为驼峰。
	 * 默认值为 true。
	 */
	public JsonKit setWriteRowFieldSnakeToCamel(boolean enabled) {
		JsonObject.defaultSnakeToCamel = enabled;
		return this;
	}

	/**
	 * 设置 json 字符串转换为 Java 对象时，Row 字段由下划线转驼峰之前是否将字段转成小写，
	 * 适用数据表字段大写的数据库，例如 Oracle。
	 */
	public JsonKit setWriteRowFieldLowerBeforeCamel(boolean enabled) {
		JsonObject.defaultLowerBeforeCamel = enabled;
		return this;
	}

	/**
	 * 设置 aifei-db 的 Model 转换为 json 字符串时，是否当成 Row 去转换，也即取出内部的 Map data
	 * 数据转成 json，而非调用 getter 方法取值转换。用于 SQL 关联查询将关联表字段一同转换为 json。
	 * 默认值为 false。
	 */
	public JsonKit setWriteModelAsRow(boolean enabled) {
		JsonObject.defaultModelAsRow = enabled;
		return this;
	}

	/**
	 * 设置 Java 对象转换为 json 字符串时的日期格式。
	 */
	public JsonKit setWriteDateFormat(String dateFormat) {
		JsonObject.defaultDateFormat = dateFormat;
		return this;
	}

	/**
	 * 设置 json 字符串转换为 Java 对象时的日期格式。
	 * 注意：一般不建议配置，而是利用 snack4 自动探测。
	 */
	public JsonKit setReadDateFormat(String dateFormat) {
		JsonString.defaultDateFormat = dateFormat;
		return this;
	}

	/**
	 * 设置格式化、缩进 json 字符串，提升可读性。
	 * 注意：有较大性能损耗，仅用于开发调试，生产环境请勿开启。
	 */
	public JsonKit setWritePrettyFormat(boolean enabled) {
		JsonObject.defaultPrettyFormat = enabled;
		return this;
	}

	/**
	 * 配置 JsonFactory，用于定制 JsonObject、JsonString 实现。
	 */
	public JsonKit setJsonFactory(JsonFactory jsonFactory) {
		Objects.requireNonNull(jsonFactory, "jsonFactory can not be null.");
		JsonKit.jsonFactory = jsonFactory;
		return this;
	}
}
