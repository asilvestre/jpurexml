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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

/**
 * Class to hold static methods to parse an XML string.
 */
public class XmlParser {

	/**
	 * Parse a String with an XML into an XmlDoc object
	 * 
	 * @param xml
	 * @throws XmlParseException
	 */
	public static XmlDoc parseXml(String xml) throws XmlParseException {
		XmlDoc res = new XmlDoc();

		// First of all removing all the comments from the XML
		String procXml = removeComments(xml);

		// Parsing XML prologue
		int prologEnd = parsePrologue(procXml, res.prologue);

		// Parsing the XML body
		parseTag(procXml, prologEnd, res.root);

		return res;
	}

	/**
	 * Remove XML comments
	 * 
	 * @param xml
	 * @return input XML without comments
	 */
	private static String removeComments(String xml) throws XmlParseException {
		String res = xml;

		int commentPos = res.indexOf("<!--");
		while (commentPos != -1) {
			int commentEnd = res.indexOf("-->");

			if (commentEnd == -1) {
				throw new XmlParseException("Missing comment ending '-->'", commentPos);
			}

			res = String.format("%s%s", res.substring(0, commentPos), res.substring(commentEnd + 3));

			commentPos = res.indexOf("<!--");
		}

		return res;
	}

	/**
	 * Parses the first line of each XML which states its version and encoding
	 * 
	 * @param xml
	 * @param prologue
	 * @return position where the encoding finishes
	 */
	private static int parsePrologue(String xml, XmlPrologue prologue) {
		prologue.version = "1.0";
		prologue.encoding = "UTF-8";

		int prologueStart = xml.indexOf("<?xml");
		int prologueEnd = xml.indexOf("?>", prologueStart);

		if (prologueStart != -1 && prologueEnd != -1) {
			String prologueString = xml.substring(prologueStart + 5, prologueEnd);

			HashMap<String, String> prologueAttrs = new HashMap<String, String>();
			parseAttributeList(prologueString, 0, prologueAttrs);

			if (prologueAttrs.containsKey("version")) {
				prologue.version = prologueAttrs.get("version");
			}

			if (prologueAttrs.containsKey("encoding")) {
				prologue.encoding = prologueAttrs.get("encoding");
			}
		}

		return prologueEnd != -1 ? prologueEnd + "?>".length() : 0;
	}

	/**
	 * Parse a tag and its children
	 * 
	 * @param xml
	 * @param pointer
	 *            position from where to start parsing
	 * @param tag
	 *            output parameter where the tag information will be put
	 * @return position where it has stopped parsing
	 * @throws XmlParseException
	 */
	private static int parseTag(String xml, int pointer, XmlTag tag) throws XmlParseException {
		int res;

		// Parsing the name and attributes of the tag
		int headerEnd = parseTagHeader(xml, pointer, tag);

		// If the tag wasn't an empty tag (finishes right away with a '/>') look
		// for children content
		if (!tag.empty) {
			// First looking for tag content which is not a children XML tag
			int childrenPos = parseTagContent(xml, headerEnd, tag);

			while (hasChildren(xml, childrenPos, tag.name)) {
				XmlTag child = new XmlTag();
				childrenPos = parseTag(xml, childrenPos, child);

				tag.children.add(child);

				// As far as I know there could be child tags and content mixed,
				// TODO: I am not planning on using this, so I'll just append
				// all the content in one string
				// but I'm not preserving the order between tags and chunks of
				// content
				childrenPos = parseTagContent(xml, childrenPos, tag);
			}

			res = parseEndTag(xml, childrenPos, tag.name);
		} else {
			res = headerEnd;
		}

		return res;
	}

	/**
	 * All the different states the tag header parser can be in.
	 */
	private enum TagHeaderStates {
		Init, TagStart, Name, AttrList, EmptyTagEnd, End, Invalid
	}

	/**
	 * Enumeration with all the possible events we can receive when parsing an
	 * tag header.
	 */
	private enum TagHeaderActions {
		Space, TagInit, NameChar, Slash, TagEnd, Invalid
	}

	/**
	 * Structure that describes the tag header parser state machine. There is an
	 * array entry for each header parser state, and for each of these there's
	 * an array with an entry for each tag header parser action that describes
	 * to which state should go when receiving that action. For instance in the
	 * tag header name state if we receive another character we stay in the tag
	 * header name state, but if we receive a ' ' we go the attribute list
	 * state.
	 */
	private static final TagHeaderStates[][] TagHeaderStateMachine = new TagHeaderStates[][] {
			// Init state
			{ TagHeaderStates.Init, TagHeaderStates.TagStart, TagHeaderStates.Invalid, TagHeaderStates.Invalid,
					TagHeaderStates.Invalid, TagHeaderStates.Invalid },
			// Tag start state '<'
			{ TagHeaderStates.TagStart, TagHeaderStates.Invalid, TagHeaderStates.Name, TagHeaderStates.Invalid,
					TagHeaderStates.Invalid, TagHeaderStates.Invalid },
			// Tag name state '<' + ' tagname '
			{ TagHeaderStates.AttrList, TagHeaderStates.Invalid, TagHeaderStates.Name, TagHeaderStates.EmptyTagEnd,
					TagHeaderStates.End, TagHeaderStates.Invalid },
			// Attribute list state 'key='val' key2='val'' (this will be
			// processed in its own state machine)
			{ TagHeaderStates.AttrList, TagHeaderStates.Invalid, TagHeaderStates.Invalid, TagHeaderStates.EmptyTagEnd,
					TagHeaderStates.End, TagHeaderStates.Invalid },
			// Empty tag end state, '/' + '>'
			{ TagHeaderStates.Invalid, TagHeaderStates.Invalid, TagHeaderStates.Invalid, TagHeaderStates.Invalid,
					TagHeaderStates.End, TagHeaderStates.Invalid }, };

	/**
	 * Attribute parser data such as where is the position where the attribute
	 * name starts and so forth
	 */
	private static class TagHeaderParserData {
		public int nameStart = 0;
		public int nameEnd = 0;
		public HashMap<String, String> attributes = new HashMap<String, String>();
		public boolean empty = false;
	}

	/**
	 * Parse the tag name and attribute list
	 * 
	 * @param xml
	 * @param pointer
	 * @param tag
	 * @return position from where to continue parsing
	 * @throws XmlParseException
	 */
	private static int parseTagHeader(String xml, int pointer, XmlTag tag) throws XmlParseException {
		TagHeaderStates state = TagHeaderStates.Init;
		TagHeaderParserData parserData = new TagHeaderParserData();

		int i = pointer;
		boolean done = i >= xml.length();
		while (!done) {
			// From the current character determine its corresponding action in
			// the state machine
			char nextChar = xml.charAt(i);
			TagHeaderActions action = parseCharIntoTagHeaderAction(nextChar);

			// Apply the action to the current state of the state machine and
			// obtain its resulting new state
			TagHeaderStates newState = TagHeaderStateMachine[state.ordinal()][action.ordinal()];

			// Process this state transition
			if (state != newState) // In this parser interesting stuff only
									// happens when we change state
			{
				i = processTagHeaderStateTransition(xml, i, state, newState, parserData);
			} else {
				i++;
			}

			state = newState;

			done = i >= xml.length() || state == TagHeaderStates.End || state == TagHeaderStates.Invalid;
		}

		// If the tag header parsing was successful store the name and
		// attributes in the XML tag object
		if (state == TagHeaderStates.End) {
			String name = xml.substring(parserData.nameStart, parserData.nameEnd);

			tag.name = name;
			tag.attributes = parserData.attributes;
			tag.empty = parserData.empty;
		} else {
			throw new XmlParseException("Error parsing tag header", i);
		}

		return i;
	}

	/**
	 * Convert a char to its corresponding TagHeaderAction for the tag header
	 * parser state machine
	 * 
	 * @param c
	 *            Character to parse
	 * @return The corresponding TagHeaderAction for the input character
	 */
	private static TagHeaderActions parseCharIntoTagHeaderAction(char c) {
		// By default we mark it as a valid value character
		TagHeaderActions res = TagHeaderActions.NameChar;

		// Checking if it's some form of whitespace
		if (Character.isWhitespace(c)) {
			res = TagHeaderActions.Space;
		} else if (c == '<') {
			res = TagHeaderActions.TagInit;
		} else if (c == '>') {
			res = TagHeaderActions.TagEnd;
		} else if (c == '/') {
			res = TagHeaderActions.Slash;
		} else if (c == '\'' || c == '"') {
			res = TagHeaderActions.Invalid;
		}

		return res;
	}

	/**
	 * Process a state transition
	 * 
	 * @param pos
	 *            Current parsing position
	 * @param from
	 *            Old state
	 * @param to
	 *            New state
	 * @param parserData
	 *            Here it will be stored name and value positions as they are
	 *            found
	 * @return the position from where to continue parsing
	 */
	private static int processTagHeaderStateTransition(String xml, int pos, TagHeaderStates from, TagHeaderStates to,
			TagHeaderParserData parserData) {
		// By default we continue parsing from the next character
		int res = pos + 1;

		// Transition from a non-name state to a name state, we store the
		// initial position of the name
		if (from != TagHeaderStates.Name && to == TagHeaderStates.Name) {
			parserData.nameStart = pos;
		}
		// Transition from a name state to a non-name state, we store the final
		// position of the name
		else if (from == TagHeaderStates.Name && to != TagHeaderStates.Name) {
			parserData.nameEnd = pos;
		}

		// Parse the attribute list, it has its own parser, it will return the
		// position from where to continue parsing
		if (from != TagHeaderStates.AttrList && to == TagHeaderStates.AttrList) {
			res = parseAttributeList(xml, pos, parserData.attributes);
		}

		// If we find a '/' it means this tag has no body
		if (to == TagHeaderStates.EmptyTagEnd) {
			parserData.empty = true;
		}

		return res;
	}

/**
	 * Checks if the next tag is an ending tag with the name of the parent
	 * @param xml
	 * @param pointer position starting with a '<' in xml
	 * @param parentName name of the parent tag to check if it has children
	 * @return if there are tags before the ending tag of the parent
	 */
	private static boolean hasChildren(String xml, int pointer, String parentName) {
		boolean res = false;

		try {
			parseEndTag(xml, pointer, parentName);
		} catch (XmlParseException e) {
			res = true;
		}

		return res;
	}

	/**
	 * Check this tag is the end tag for a specific parent tag
	 * 
	 * @param xml
	 * @param pointer
	 *            points to a tag that should be the end tag for tagName
	 * @param tagName
	 * @return position from where to continue parsing
	 */
	private static int parseEndTag(String xml, int pointer, String tagName) throws XmlParseException {
		int res;

		boolean correct = xml.startsWith("</", pointer);

		// Getting everything between the initial '</' and a '>'
		int endPos = xml.indexOf(">", pointer);
		if (correct) {
			correct = correct && endPos != -1;
		}

		if (correct) {
			String potentialParentEndTag = xml.substring(pointer + "</".length(), endPos);

			// Trimming any spaces before and after the string we have generated
			potentialParentEndTag = potentialParentEndTag.trim();

			// Here we should have the name of the parent tag
			correct = tagName.equals(potentialParentEndTag);
		}

		if (correct) {
			res = endPos + 1;
		} else {
			throw new XmlParseException(String.format("Expecting end tag <%s/>", tagName), pointer);
		}

		return res;
	}

	/**
	 * All the different states the attribute parser can be in.
	 */
	private enum AttrStates {
		Init, Name, PreSeparator, Separator, PostSeparator, SingleQuotedContent, DoubleQuotedContent, End, Invalid
	}

	/**
	 * Enumeration with all the possible events we can receive when parsing an
	 * attribute.
	 */
	private enum AttrActions {
		Space, NameChar, Separator, SingleQuote, DoubleQuote, Slash, Invalid
	}

	/**
	 * Structure that describes the attribute parser state machine. There is an
	 * array entry for each attribute parser state, and for each of these
	 * there's an array with an entry for each attribute parser action that
	 * describes to which state should go when receiving that action. For
	 * instance in the attribute name state if we receive another character we
	 * stay in the attribute name state, but if we receive an '=' we go the
	 * attribute separator state.
	 */
	private static final AttrStates[][] AttrStateMachine = new AttrStates[][] {
			// Init state
			{ AttrStates.Init, AttrStates.Name, AttrStates.Invalid, AttrStates.Invalid, AttrStates.Invalid,
					AttrStates.Invalid, AttrStates.Invalid },
			// Attribute name state
			{ AttrStates.PreSeparator, AttrStates.Name, AttrStates.Separator, AttrStates.Invalid, AttrStates.Invalid,
					AttrStates.Invalid, AttrStates.Invalid },
			// Attribute pre separator state
			{ AttrStates.PreSeparator, AttrStates.Invalid, AttrStates.Separator, AttrStates.Invalid,
					AttrStates.Invalid, AttrStates.Invalid, AttrStates.Invalid },
			// Attribute separator state
			{ AttrStates.PostSeparator, AttrStates.Invalid, AttrStates.Invalid, AttrStates.SingleQuotedContent,
					AttrStates.DoubleQuotedContent, AttrStates.Invalid, AttrStates.Invalid },
			// Attribute separator post state
			{ AttrStates.PostSeparator, AttrStates.Invalid, AttrStates.Invalid, AttrStates.SingleQuotedContent,
					AttrStates.DoubleQuotedContent, AttrStates.Invalid, AttrStates.Invalid },
			// Single quoted content state
			{ AttrStates.SingleQuotedContent, AttrStates.SingleQuotedContent, AttrStates.SingleQuotedContent,
					AttrStates.End, AttrStates.SingleQuotedContent, AttrStates.SingleQuotedContent, AttrStates.Invalid },
			// Double quoted content state
			{ AttrStates.DoubleQuotedContent, AttrStates.DoubleQuotedContent, AttrStates.DoubleQuotedContent,
					AttrStates.DoubleQuotedContent, AttrStates.End, AttrStates.DoubleQuotedContent, AttrStates.Invalid }, };

	/**
	 * Attribute parser data such as where is the position where the attribute
	 * name starts and so forth
	 */
	private static class AttrParserData {
		public int nameStart = 0;
		public int nameEnd = 0;
		public int valueStart = 0;
		public int valueEnd = 0;
	}

	/**
	 * Parse an attribute list, if it doesn't find anything or finds something
	 * not belonging to an attribute list returns with the position of the
	 * offending character, in the meantime it will have filled the attributes
	 * hashtable argument with all the attributes it has found.
	 * 
	 * @param xml
	 *            String to look for an attribute list
	 * @param pointer
	 *            Position from where to start parsing
	 * @param attributes
	 *            Output parameter to place all key-value entries with the
	 *            attributes found
	 * @return the position for the parser to continue on
	 */
	private static int parseAttributeList(String xml, int pointer, HashMap<String, String> attributes) {
		int i = pointer;
		boolean done = false;

		// Go parsing attributes until we find something it is not an XML tag
		// attribute
		do {
			AttrStates state = AttrStates.Init;
			AttrParserData parserData = new AttrParserData();
			boolean attrDone = i >= xml.length() || state == AttrStates.End || state == AttrStates.Invalid;
			while (!attrDone) {
				// From the current character determine its corresponding action
				// in the state machine
				char nextChar = xml.charAt(i);
				AttrActions action = parseCharIntoAttrAction(nextChar);

				// Apply the action to the current state of the state machine
				// and obtain its resulting new state
				AttrStates newState = AttrStateMachine[state.ordinal()][action.ordinal()];

				// Process this state transition
				if (state != newState) // In this parser interesting stuff only
										// happens when we change state
				{
					i = processAttrStateTransition(i, state, newState, parserData);
				} else {
					i++;
				}

				state = newState;

				attrDone = i >= xml.length() || state == AttrStates.End || state == AttrStates.Invalid;
			}

			// If the attribute parsing was successful store it in the hash
			// table
			if (state == AttrStates.End) {
				String name = xml.substring(parserData.nameStart, parserData.nameEnd);
				String value = xml.substring(parserData.valueStart, parserData.valueEnd);

				// Escaping value literal
				value = unescapeXmlLiteral(value);

				attributes.put(name, value);
			}

			done = i >= xml.length() || state == AttrStates.Invalid;
		} while (!done);

		return i;
	}

	/**
	 * Convert a char to its corresponding AttrAction for the attribute parser
	 * state machine
	 * 
	 * @param c
	 *            Character to parse
	 * @return The corresponding AttrAction for the intut character
	 */
	private static AttrActions parseCharIntoAttrAction(char c) {
		// By default we mark it as a valid value character
		AttrActions res = AttrActions.NameChar;

		// Checking if it's some form of whitespace
		if (Character.isWhitespace(c)) {
			res = AttrActions.Space;
		}
		// For this parser '<' and '>' are invalid
		else if (c == '<' || c == '>') {
			res = AttrActions.Invalid;
		}
		// '=' separates the name of the attribute and its value
		else if (c == '=') {
			res = AttrActions.Separator;
		}
		// Values can be enclosed in single and double quotes
		else if (c == '\'') {
			res = AttrActions.SingleQuote;
		} else if (c == '"') {
			res = AttrActions.DoubleQuote;
		} else if (c == '/') {
			res = AttrActions.Slash;
		}

		return res;
	}

	/**
	 * Process a state transition
	 * 
	 * @param pos
	 *            Current parsing position
	 * @param from
	 *            Old state
	 * @param to
	 *            New state
	 * @param parserData
	 *            Here it will be stored name and value positions as they are
	 *            found
	 */
	private static int processAttrStateTransition(int pos, AttrStates from, AttrStates to, AttrParserData parserData) {
		int res = pos + 1;

		// Transition from a non-name state to a name state, we store the
		// initial position of the name
		if (from != AttrStates.Name && to == AttrStates.Name) {
			parserData.nameStart = pos;
		}
		// Transition from a name state to a non-name state, we store the final
		// position of the name
		else if (from == AttrStates.Name && to != AttrStates.Name) {
			parserData.nameEnd = pos;
		}
		// Transition from a non-value state to a value state (single or double
		// quoted), store the initial value pos
		else if ((from != AttrStates.SingleQuotedContent && to == AttrStates.SingleQuotedContent)
				|| (from != AttrStates.DoubleQuotedContent && to == AttrStates.DoubleQuotedContent)) {
			parserData.valueStart = pos + 1;
		}
		// Transition from a value state (single or double quoted) to a non
		// value state
		else if ((from == AttrStates.SingleQuotedContent && to == AttrStates.End)
				|| (from == AttrStates.DoubleQuotedContent && to == AttrStates.End)) {
			parserData.valueEnd = pos;
		}

		// When we are in the invalid state here means this is not part of the
		// attribute list
		if (to == AttrStates.Invalid) {
			res = pos;
		}

		return res;
	}

	/**
	 * All the different states the tag header parser can be in.
	 */
	private enum TagContentStates {
		Content, Gt, CDATA, End, Invalid
	}

	/**
	 * Enumeration with all the possible events we can receive when parsing an
	 * tag header.
	 */
	private enum TagContentActions {
		Char, TagInit, Exclamation, Invalid
	}

	/**
	 * Structure that describes the tag content parser state machine.
	 */
	private static final TagContentStates[][] TagContentStateMachine = new TagContentStates[][] {
			// Content state
			{ TagContentStates.Content, TagContentStates.Gt, TagContentStates.Invalid, TagContentStates.Invalid },
			// GT state (a '<' has been found and we need to decide if it's a
			// new tag or a CDATA)
			{ TagContentStates.End, TagContentStates.Invalid, TagContentStates.CDATA, TagContentStates.Invalid },
			// CDATA state (it has it's own parser)
			{ TagContentStates.Content, TagContentStates.Gt, TagContentStates.Invalid, TagContentStates.Invalid }, };

	/**
	 * Tag content parser data It contains all the data fragments it has found
	 * along the way
	 */
	private static class TagContentParserData {
		public TagContentParserData(int pos) {
			lastContentStart = pos;
		}

		public LinkedList<String> contentBits = new LinkedList<String>();
		public int lastContentStart;
	}

	/**
	 * Parse the content of a tag
	 * 
	 * @param xml
	 * @param pointer
	 * @param tag
	 * @return position from where to continue parsing
	 * @throws XmlParseException
	 */
	private static int parseTagContent(String xml, int pointer, XmlTag tag) throws XmlParseException {
		TagContentStates state = TagContentStates.Content;
		TagContentParserData parserData = new TagContentParserData(pointer);

		int i = pointer;
		boolean done = i >= xml.length();
		while (!done) {
			// From the current character determine its corresponding action in
			// the state machine
			char nextChar = xml.charAt(i);
			TagContentActions action = parseCharIntoTagContentAction(nextChar);

			// Apply the action to the current state of the state machine and
			// obtain its resulting new state
			TagContentStates newState = TagContentStateMachine[state.ordinal()][action.ordinal()];

			// Process this state transition
			if (state != newState) // In this parser interesting stuff only
									// happens when we change state
			{
				i = processTagContentStateTransition(xml, i, state, newState, parserData);
			} else {
				i++;
			}

			state = newState;

			done = i >= xml.length() || state == TagContentStates.End || state == TagContentStates.Invalid;
		}

		// If the tag content parsing was successful combine all the string bits
		// we have found into one
		if (state == TagContentStates.End) {
			String contentBit = parserData.contentBits.poll();
			while (contentBit != null) {
				// TODO: Java seems to not have an efficient way of joining all
				// the strings into one using its
				// standard library, for now doing it like this
				tag.content += contentBit;

				contentBit = parserData.contentBits.poll();
			}

			// We have to return a position minus two, because we have parsed a
			// '<' plus something else
			i -= 2;
		} else {
			throw new XmlParseException("Error parsing tag content", i);
		}

		return i;
	}

	/**
	 * Convert a char to its corresponding TagContentAction for the tag content
	 * parser state machine
	 * 
	 * @param c
	 *            Character to parse
	 * @return The corresponding TagContentAction for the input character
	 */
	private static TagContentActions parseCharIntoTagContentAction(char c) {
		// By default we mark it as a valid value character
		TagContentActions res = TagContentActions.Char;

		if (c == '<') {
			res = TagContentActions.TagInit;
		} else if (c == '!') {
			res = TagContentActions.Exclamation;
		} else if (c == '>' || c == '\'' || c == '"') {
			res = TagContentActions.Invalid;
		}

		return res;
	}

	/**
	 * Process changing from one content tag parsing state to another
	 * 
	 * @param xml
	 * @param pos
	 * @param from
	 * @param to
	 * @param parserData
	 * @return Position from where to continue parsing
	 * @throws XmlParseException
	 */
	private static int processTagContentStateTransition(String xml, int pos, TagContentStates from,
			TagContentStates to, TagContentParserData parserData) throws XmlParseException {
		int res = pos + 1;

		// Transition from a non-content state to a content state, we store the
		// initial position of this content bit
		if (from != TagContentStates.Content && to == TagContentStates.Content) {
			parserData.lastContentStart = pos;
		}
		// Transition from a content state to a non-content state, get the
		// substring for this content
		if (from == TagContentStates.Content && to != TagContentStates.Content) {
			if (pos != parserData.lastContentStart) {
				String contentBit = xml.substring(parserData.lastContentStart, pos);

				// Trimming initial and final spaces
				contentBit = contentBit.trim();

				// unescaping string bit
				contentBit = unescapeXmlLiteral(contentBit);

				// Removing linefeeds and tabs
				contentBit = removeTabsAndLinefeeds(contentBit);

				parserData.contentBits.add(contentBit);
			}
		}
		// Transition to a CDATA state
		else if (from != TagContentStates.CDATA && to == TagContentStates.CDATA) {
			// We take away one from pos because pos has already passed over the
			// '<!' of the '<![CDATA['
			res = parseCDATA(xml, pos - 1, parserData);
		}

		return res;
	}

	/**
	 * Parses a CDATA piece of content
	 * 
	 * @param xml
	 * @param pos
	 *            Position pointing at the very start of a CDATA block
	 *            "<![CDATA["
	 * @param parserData
	 * @return position from where to continue parsing
	 */
	private static int parseCDATA(String xml, int pos, TagContentParserData parserData) throws XmlParseException {
		boolean correct = xml.startsWith("<![CDATA[", pos);
		int res = pos;

		if (correct) {
			int cdataEnd = xml.indexOf("]]>", pos);

			correct = cdataEnd != -1;
			// We have a correct CDATA block
			if (correct) {
				String contentBit = xml.substring(pos + "<![CDATA[".length(), cdataEnd);

				parserData.contentBits.add(contentBit);

				res = cdataEnd + "]]>".length();
			}
		}

		if (!correct) {
			throw new XmlParseException("Error parsing CDATA block", pos);
		}

		return res;
	}

	private static class StringPair {
		public StringPair(String first, String second) {
			this.first = first;
			this.second = second;
		}

		public String first;
		public String second;
	}

	private static final StringPair[] EscapedEntities = new StringPair[] { new StringPair("&lt", "<"),
			new StringPair("&gt", ">"), new StringPair("&amp", "&"), new StringPair("&apos", "'"),
			new StringPair("&quot", "\""), };

	/**
	 * Unescape XML literal, that is &lt, &gt, &amp, &apos, &quot
	 * 
	 * @param literal
	 * @return
	 */
	public static String unescapeXmlLiteral(String literal) {
		String res = literal;

		// Look for any escaped entities
		for (int i = 0; i < EscapedEntities.length; i++) {
			// For each entity replace all of its occurrences
			res = Utils.ReplaceStr(res, EscapedEntities[i].first, EscapedEntities[i].second);
		}

		return res;
	}

	/**
	 * Escape XML literal, that is &lt, &gt, &amp, &apos, &quot
	 * 
	 * @param literal
	 * @param skip
	 *            List of tokens to skip escaping
	 * @return escaped literal
	 */
	public static String escapeXmlLiteral(String literal, String[] skip) {
		String res = literal;
		TreeSet<String> skipSet = new TreeSet<String>();
		if (skip != null) {
			for (int i = 0; i < skip.length; i++) {
				skipSet.add(skip[i]);
			}
		}

		// Look for any escaped entities
		for (int i = 0; i < EscapedEntities.length; i++) {
			if (!skipSet.contains(EscapedEntities[i].second)) {
				// For each entity replace all of its occurrences
				res = Utils.ReplaceStr(res, EscapedEntities[i].second, EscapedEntities[i].first);
			}
		}

		return res;
	}

	private static String removeTabsAndLinefeeds(String literal) {
		String res = literal;

		res = Utils.ReplaceStr(res, "\n", "");
		res = Utils.ReplaceStr(res, "\t", "");
		res = Utils.ReplaceStr(res, "\r", "");

		return res;
	}

}
