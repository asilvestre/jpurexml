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

import junit.framework.TestCase;

/**
 * Parses the main XML parsing functionality
 */
public class XmlParserTest extends TestCase {

	/**
	 * Test we parse the prologue correctly
	 */
	public void testParsePrologue() {
		String[] inputs = new String[] { "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root/>",
				"<?xml version='1.0' encoding=\"UTF-8\"?><root/>",
				"<?xml version='2.0' encoding=\"UTF-8\"?><   root     />",
				"<?xml encoding = \"UTF-8\"     version='1.0' ?><root/>",
				"<?xml a='r'   encoding = \"UTF-8\"     version='1.0' ?><root/>", };

		String[] outputs = new String[] { "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root />",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root />",
				"<?xml version=\"2.0\" encoding=\"UTF-8\"?><root />",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root />",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root />", };

		for (int i = 0; i < inputs.length; i++) {
			try {
				XmlDoc res = XmlParser.parseXml(inputs[i]);
				assertEquals(outputs[i], res.toString());
			} catch (XmlParseException e) {
				fail("Error parsing " + e.toString());
			}
		}
	}

	/**
	 * Test parsing different attribute lists
	 */
	public void testParseAttributes() {
		String[] inputs = new String[] {
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<   root \t\n  a='&ampa\"b'  c='/' b =   \"jj'j\"    />",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><   root   a='a\"b'   b =   \"j/j'j\"    >\n\t</root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>   <root    ></  root  >", };

		String[] outputs = new String[] {
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root a='&ampa\"b' b=\"jj'j\" c=\"/\" />",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root a='a\"b' b=\"j/j'j\" ></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root ></root>", };

		for (int i = 0; i < inputs.length; i++) {
			try {
				XmlDoc res = XmlParser.parseXml(inputs[i]);
				assertEquals(outputs[i], res.toString());
			} catch (XmlParseException e) {
				fail("Error parsing " + e.toString());
			}
		}
	}

	/**
	 * Test parsing different types of tag content
	 */
	public void testParseContent() {
		String[] inputs = new String[] {
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root> hola </root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>\r\n\tho\tla\n\r\n\t\t</root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>\n<![CDATA[ \r\n\tho\tla\n\r\n\t\t]]></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>&lta<![CDATA[\r\n\tho\tla\n\r\n\t\t]]>b</root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><![CDATA[<<]]<<]]>a<![CDATA[\r\n\tho\tla\n\r\n\t\t]]>b"
						+ "<![CDATA[]]></root>", };

		String[] outputs = new String[] {
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root >hola</root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root >hola</root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root ><![CDATA[ \r\n\tho\tla\n\r\n\t\t]]></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root ><![CDATA[<a\r\n\tho\tla\n\r\n\t\tb]]></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root ><![CDATA[<<]]<<a\r\n\tho\tla\n\r\n\t\tb]]>"
						+ "</root>", };

		for (int i = 0; i < inputs.length; i++) {
			try {
				XmlDoc res = XmlParser.parseXml(inputs[i]);
				assertEquals(outputs[i], res.toString());
			} catch (XmlParseException e) {
				fail("Error parsing " + e.toString());
			}
		}
	}

	/**
	 * Test parsing tags with children
	 */
	public void testParseChildren() {
		String[] inputs = new String[] {
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a><b/><b/>hola<c i='o'></c></a></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a a='j'></a><b/></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a><b/><b/><c i='o'></c></a></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a><b/><b/>hola<c i='o'></c></a></root>", };

		String[] outputs = new String[] {
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root ><a ><b /><b /><c i=\"o\" ></c>hola</a></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root ><a a=\"j\" ></a><b /></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root ><a ><b /><b /><c i=\"o\" ></c></a></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root ><a ><b /><b /><c i=\"o\" ></c>hola</a></root>", };

		for (int i = 0; i < inputs.length; i++) {
			try {
				XmlDoc res = XmlParser.parseXml(inputs[i]);
				assertEquals(outputs[i], res.toString());
			} catch (XmlParseException e) {
				fail("Error parsing " + e.toString());
			}
		}
	}

	/**
	 * Test removing comments
	 */
	public void testRemoveComments() {
		String[] inputs = new String[] {
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a<!-- I'm a comment-->><b/><b/>hola<c i='o'></c></a></root>",
				"<?xml version=\"1.0\" <!---->encoding=\"UTF-8\"?><root><a a='j'></a><b/></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><!--Hello--><a><b/><b/><c i='o'></c></a></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root<!--ooo-->><a><b/><b/>hola<c i='o'></c></a></root>", };

		String[] outputs = new String[] {
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root ><a ><b /><b /><c i=\"o\" ></c>hola</a></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root ><a a=\"j\" ></a><b /></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root ><a ><b /><b /><c i=\"o\" ></c></a></root>",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root ><a ><b /><b /><c i=\"o\" ></c>hola</a></root>", };

		for (int i = 0; i < inputs.length; i++) {
			try {
				XmlDoc res = XmlParser.parseXml(inputs[i]);
				assertEquals(outputs[i], res.toString());
			} catch (XmlParseException e) {
				fail("Error parsing " + e.toString());
			}
		}
	}

	/**
	 * Test parsing a Tiled XML, (Tiled XML's are the main reason I'm coding
	 * this...)
	 */
	public void testParseXml() {

		String input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<map version=\"1.0\" orientation=\"isometric\" width=\"100\" height=\"100\" tilewidth=\"32\" "
				+ "tileheight=\"32\">" + " <tileset firstgid=\"1\" name=\"a\" tilewidth=\"32\" tileheight=\"32\">"
				+ "  <properties>" + "   <property name=\"2\" value=\"\"/>"
				+ "   <property name=\"a\" value=\"2222\"/>" + "  </properties>"
				+ "  <image source=\"../Downloads/36805.jpg\" width=\"75\" height=\"75\"/>" + " </tileset>"
				+ " <layer name=\"Tile Layer 1\" width=\"100\" height=\"100\" opacity=\"0.81\">"
				+ "  <data encoding=\"base64\" compression=\"zlib\">"
				+ "   eJzt2ltugzAQBVA3Yf9rblFrZTryKwn0o5wjoQIxIM0NNiYt5Rjb13L/Wb9PlhLa1WM5zlYeeYxqew9ttsY274t1rdtZr"
				+ "Hvr2Loul/fE+sX+aCv9PioeW93Tfpk8r1e3UQaj455tcwW9Grb06tWrZeyPbovnv3omNY9bGddsS39n+/O+lTx657mS2s+M8oj"
				+ "f262xr5R5HeWxbiWPuN7qV1bquNpnXd3oHmndB7O+qWeWhyweGcyekXaj+YI8jpFrNHvvcVYesvjWqtFsHHl2LF85rzzGY3hrf"
				+ "28cOSIP5v15a2zP23lM6X3Xe9nL4iHX5yMssU1tNxo3Zu8GZ89tVxfrk3PoZdK7P1auFa8ni99ifXLto7g/voe6FVkcaSWL/Pn"
				+ "KnLB1HVnM7TWaZVH18li5Rhx3ZNG3mkVtu5s9P0WtLOhbzSK2Xf2Oy+JcOY8ROZxvz2NW39kcheO0xvPo2edfXpfnH1lvnsixW"
				+ "u9Ncs3dE+f6KO0cqpXfbHlNnA+OMoha73I5zuz/e1rt5XCuW1pGn/O3cjZyAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADgv/o"
				+ "E7UADtQ==" + "  </data>" + " </layer>" + " <objectgroup name=\"a\" width=\"100\" height=\"100\">"
				+ "  <object x=\"-55\" y=\"537\">"
				+ "   <polygon points=\"0,0 -128,302 -84,344 283,151 -141,-91 -445,323 -122,514 273,239 60,-80\"/>"
				+ "  </object>" + "  <object x=\"-505\" y=\"701\">"
				+ "   <polyline points=\"0,0 339,-331 474,-330 509,-183 172,96 122,-234\"/>" + "  </object>"
				+ "  <object x=\"-229\" y=\"409\"/>" + "  <object x=\"-253\" y=\"349\" width=\"290\" height=\"80\"/>"
				+ "  <object x=\"-631\" y=\"797\" width=\"669\"/>" + "  <object x=\"-399\" y=\"981\" width=\"258\"/>"
				+ "  <object x=\"-461\" y=\"635\" width=\"350\" height=\"530\"/>"
				+ "  <object x=\"-101\" y=\"565\" width=\"265\" height=\"477\"/>"
				+ "  <object x=\"-497\" y=\"679\" width=\"156\" height=\"180\"/>"
				+ "  <object gid=\"2\" x=\"-114\" y=\"554\"/>" + "  <object gid=\"2\" x=\"-109\" y=\"663\"/>"
				+ "  <object gid=\"2\" x=\"8\" y=\"738\"/>" + "  <object gid=\"2\" x=\"11\" y=\"849\"/>"
				+ "  <object gid=\"2\" x=\"-186\" y=\"1016\"/>" + "  <object gid=\"2\" x=\"-231\" y=\"847\"/>"
				+ "  <object gid=\"2\" x=\"-243\" y=\"803\"/>" + "  <object gid=\"2\" x=\"-267\" y=\"553\"/>"
				+ " </objectgroup>" + "</map>";

		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<map height=\"100\" orientation=\"isometric\" tileheight=\"32\" tilewidth=\"32\" version=\"1.0\""
				+ " width=\"100\" >"
				+ "<tileset firstgid=\"1\" name=\"a\" tileheight=\"32\" tilewidth=\"32\" >"
				+ "<properties >"
				+ "<property name=\"2\" value=\"\" />"
				+ "<property name=\"a\" value=\"2222\" />"
				+ "</properties>"
				+ "<image height=\"75\" source=\"../Downloads/36805.jpg\" width=\"75\" />"
				+ "</tileset>"
				+ "<layer height=\"100\" name=\"Tile Layer 1\" opacity=\"0.81\" width=\"100\" >"
				+ "<data compression=\"zlib\" encoding=\"base64\" >"
				+ "eJzt2ltugzAQBVA3Yf9rblFrZTryKwn"
				+ "0o5wjoQIxIM0NNiYt5Rjb13L/Wb9PlhLa1WM5zlYeeYxqew9ttsY274t1rdtZrHvr2Loul/fE+sX+aCv9PioeW93Tfpk8r1e3UQ"
				+ "aj455tcwW9Grb06tWrZeyPbovnv3omNY9bGddsS39n+/O+lTx657mS2s+M8ojf262xr5R5HeWxbiWPuN7qV1bquNpnXd3oHmndB"
				+ "7O+qWeWhyweGcyekXaj+YI8jpFrNHvvcVYesvjWqtFsHHl2LF85rzzGY3hrf28cOSIP5v15a2zP23lM6X3Xe9nL4iHX5yMssU1t"
				+ "Nxo3Zu8GZ89tVxfrk3PoZdK7P1auFa8ni99ifXLto7g/voe6FVkcaSWL/PnKnLB1HVnM7TWaZVH18li5Rhx3ZNG3mkVtu5s9P0W"
				+ "tLOhbzSK2Xf2Oy+JcOY8ROZxvz2NW39kcheO0xvPo2edfXpfnH1lvnsixWu9Ncs3dE+f6KO0cqpXfbHlNnA+OMoha73I5zuz/e1"
				+ "rt5XCuW1pGn/O3cjZyAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADgv/oE7UADtQ=="
				+ "</data>"
				+ "</layer>"
				+ "<objectgroup height=\"100\" name=\"a\" width=\"100\" >"
				+ "<object x=\"-55\" y=\"537\" >"
				+ "<polygon points=\"0,0 -128,302 -84,344 283,151 -141,-91 -445,323 -122,514 273,239 60,-80\" />"
				+ "</object>"
				+ "<object x=\"-505\" y=\"701\" ><polyline points=\"0,0 339,-331 474,-330 509,-183 172,96 122,-234\" />"
				+ "</object>"
				+ "<object x=\"-229\" y=\"409\" /><object height=\"80\" width=\"290\" x=\"-253\" y=\"349\" />"
				+ "<object width=\"669\" x=\"-631\" y=\"797\" /><object width=\"258\" x=\"-399\" y=\"981\" />"
				+ "<object height=\"530\" width=\"350\" x=\"-461\" y=\"635\" />"
				+ "<object height=\"477\" width=\"265\" x=\"-101\" y=\"565\" />"
				+ "<object height=\"180\" width=\"156\" x=\"-497\" y=\"679\" />"
				+ "<object gid=\"2\" x=\"-114\" y=\"554\" />" + "<object gid=\"2\" x=\"-109\" y=\"663\" />"
				+ "<object gid=\"2\" x=\"8\" y=\"738\" />" + "<object gid=\"2\" x=\"11\" y=\"849\" />"
				+ "<object gid=\"2\" x=\"-186\" y=\"1016\" />" + "<object gid=\"2\" x=\"-231\" y=\"847\" />"
				+ "<object gid=\"2\" x=\"-243\" y=\"803\" />" + "<object gid=\"2\" x=\"-267\" y=\"553\" />"
				+ "</objectgroup>" + "</map>";

		try {
			XmlDoc doc = XmlParser.parseXml(input);

			assertEquals(expected, doc.toString());
		} catch (XmlParseException e) {
			fail("Parsing XML error " + e.toString());
		}
	}

}
