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

import cn.aifei.db.core.AifeiRow;
import cn.aifei.db.core.Row;
import cn.aifei.enjoy.util.InstanceUtil;
import java.util.Map;

/**
 * AifeiDbSupport 处理 aifei-db 的 Model 与 Row 之间的转换。
 * 仅在 aifei-db 和 aifei-enjoy 存在时由 JsonKit 加载。
 */
class AifeiDbSupport {

	/**
	 * 若对象为 AifeiRow 且 modelAsRow 为 true，则将其转为 Row。
	 */
	static Object toRowIfNeeded(Object obj, JsonObject jo) {
		if (obj instanceof AifeiRow && Cpc.getModelAsRow(jo)) {
			return new Row().data(((AifeiRow<?>) obj).data());
		}
		return obj;
	}

	/**
	 * 若目标类型为 AifeiRow 子类且 modelAsRow 为 true，则先转为 Row 再创建 Model。
	 *
	 * @return 转换后的 Model 对象，或 null 表示不需要特殊处理
	 */
	static Object toModelIfNeeded(Class<?> type, Map<String, Object> map, JsonString js) {
		if (AifeiRow.class.isAssignableFrom(type) && Cpc.getModelAsRow(js)) {
			boolean cs = Cpc.getCamelToSnake(js);
			Row row = RowReader.toRow(map, cs);
			if (row != null) {
				AifeiRow<?> model = (AifeiRow<?>) InstanceUtil.get(type);
				return model.data(row);
			}
		}
		return null;
	}

	/**
	 * 判断类型是否为 AifeiRow 子类
	 */
	static boolean isModel(Class<?> type) {
		return AifeiRow.class.isAssignableFrom(type);
	}
}
