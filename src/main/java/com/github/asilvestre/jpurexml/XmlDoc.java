/**
 * Copyright Antoni Silvestre
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.asilvestre.jpurexml;

/**
 * Main object that describes an XML document
 */
public class XmlDoc {
	/**
	 * Has the XML prologue, that is the initial '<?xml' tag with its version
	 * and encoding
	 */
	public XmlPrologue prologue = new XmlPrologue();

	/**
	 * Has the root tag for the XML document
	 */
	public XmlTag root = new XmlTag();

	@Override
	public String toString() {
		String res = prologue.toString() + root.toString();

		return res;
	}
}
