package org.apache.logging.log4j.jackson.xml.layout;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamWriter2;

import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;

/**
 * When &lt;Event&gt;s are written into a XML file; the "Event" object is not the root element, but an element named
 * &lt;Events&gt; created using {@link XmlLayout#getHeader()} and {@link XmlLayout#getFooter()} methods.
 * <p>
 * {@link com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter} is used to print the Event object into
 * XML; hence it assumes &lt;Event&gt; tag as the root element, so it prints the &lt;Event&gt; tag without any
 * indentation. To add an indentation to the &lt;Event&gt; tag; hence an additional indentation for any
 * sub-elements, this class is written. As an additional task, to avoid the blank line printed after the ending
 * &lt;/Event&gt; tag, {@link #writePrologLinefeed(XMLStreamWriter2)} method is also overridden.
 * </p>
 */
class Log4jXmlPrettyPrinter extends DefaultXmlPrettyPrinter {

    private static final long serialVersionUID = 1L;

    Log4jXmlPrettyPrinter(final int nesting) {
        _nesting = nesting;
    }

    /**
     * Sets the nesting level to 1 rather than 0, so the "Event" tag will get indentation of next level below root.
     */
    @Override
    public DefaultXmlPrettyPrinter createInstance() {
        return new Log4jXmlPrettyPrinter(XmlJacksonFactory.DEFAULT_INDENT);
    }

    @Override
    public void writePrologLinefeed(final XMLStreamWriter2 sw) throws XMLStreamException {
        // nothing
    }

}