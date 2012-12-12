jpurexml
========

Simple all-in-memory XML parser in pure Java code so tools like Google's PlayN can transcompile it to all its target platforms

About XML compliance, it should support any regular XML document including comments and CDATA blocks. For more details about what is supported take a look at its unit tests.

This library has one main function XmlParse.parseXml(String) which returns an XmlDoc object. See the [javadocs](http://asilvestre.github.com/jpurexml/apidocs/index.html) for more details about its structure.
