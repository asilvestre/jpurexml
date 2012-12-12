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

import java.util.AbstractMap;
import java.util.AbstractSequentialList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

/**
 * Class that represents an XML tag with its children, content and attributes.
 * It doesn't have getter or setter methods for its properties as it is only
 * intended to be a simple data container.
 */
public class XmlTag {
	/**
	 * Name for this tag
	 */
	public String name = "";

	/**
	 * Dictionary with all the attributes for this tag.
	 */
	public AbstractMap<String, String> attributes = new HashMap<String, String>();

	/**
	 * Ordered list with all the tag children for this tag.
	 */
	public AbstractSequentialList<XmlTag> children = new LinkedList<XmlTag>();

	/**
	 * Tag contents which are not child tags, i.e. <tag>content</tag>. If a tag
	 * has content and child tags mixed all the content will be aggregated here
	 * in one unit.
	 */
	public String content = "";

	/**
	 * If this tag is singleton. That is, it doesn't have a closing tag, for
	 * instance <emptytag />
	 */
	public boolean empty = false;

	@Override
	public String toString() {
		String res;

		// Printing tag header (name and attributes)
		String attrStr = "";
		Iterator<String> keyIter = attributes.keySet().iterator();

		// Sorting attributes (so its easier to writes tests)
		TreeSet<String> treeSet = new TreeSet<String>();
		while (keyIter.hasNext()) {
			treeSet.add(keyIter.next());
		}

		// Rendering the attributes
		Iterator<String> sortedKeyIter = treeSet.iterator();
		while (sortedKeyIter.hasNext()) {
			String key = XmlParser.escapeXmlLiteral(sortedKeyIter.next(), null);
			String value = XmlParser.escapeXmlLiteral(attributes.get(key), new String[] { "\"", "'" });

			boolean valueHasDoubleQuotes = value.indexOf("\"") != -1;
			String attrFormat = valueHasDoubleQuotes ? "%s='%s' " : "%s=\"%s\" ";
			attrStr += String.format(attrFormat, key, value);
		}

		String headerFormat = empty ? "<%s %s/>" : "<%s %s>";
		res = String.format(headerFormat, XmlParser.escapeXmlLiteral(name, null), attrStr);

		// if the header is not empty print its children and ending tag
		if (!empty) {
			String childrenStr = "";

			Iterator<XmlTag> childIter = children.iterator();

			while (childIter.hasNext()) {
				XmlTag child = childIter.next();

				childrenStr += child.toString();
			}

			// Checking if the content has any char that needs to be inside a
			// CDATA block
			String procContent = contentHasSpecialChars() ? String.format("<![CDATA[%s]]>", content) : content;

			// Adding the ending tag
			res = String.format("%s%s%s</%s>", res, childrenStr, procContent, XmlParser.escapeXmlLiteral(name, null));
		}

		return res;
	}

	/**
	 * This is the list of special characters, this list might not be exhausted,
	 * but my use of XML printing is mainly for testing purposes.
	 */
	private static char[] SpecialChars = new char[] { '\n', '\r', '\t', '<', '>', '&', '\'', '"', };

	/**
	 * @return The content of this tag has special characters and should be
	 *         printed enclosed in a CDATA block
	 */
	private boolean contentHasSpecialChars() {
		boolean res = false;
		for (int i = 0; i < SpecialChars.length && !res; i++) {
			res = content.indexOf(SpecialChars[i]) != -1;
		}

		// Check if it has initial or final spaces
		res = res || !content.trim().equals(content);

		return res;
	}
}
