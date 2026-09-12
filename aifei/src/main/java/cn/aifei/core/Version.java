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

package cn.aifei.core;

/**
 * 版本号常量，由 templating-maven-plugin 在构建时用根 pom 的 revision 属性生成。
 * 请勿手工修改本文件；升级版本只需修改根 pom 中的 revision 属性。
 */
public final class Version {

    public static final String VERSION = "1.1.1";

    private Version() {}
}
