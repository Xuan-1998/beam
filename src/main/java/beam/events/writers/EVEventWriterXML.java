package beam.events.writers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Map;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import beam.EVGlobalData;

public class EVEventWriterXML implements EventWriter, BasicEventHandler {

	protected EVEventWriterXML() {
		
	}
	
	public EVEventWriterXML(String outfilename) {
		this.out = IOUtils.getBufferedWriter(outfilename);
		try {
			this.out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<events version=\"1.0\">\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void handleEvent(final Event event) {
		if (EVGlobalData.data.eventLogger.getControlEventTypesWithLogger().contains(event.getClass())) {
			if (EVGlobalData.data.controler.getIterationNumber() % EVGlobalData.data.eventLogger.getWriteEVEventsInterval() == 0) {
				if (EVGlobalData.data.eventLogger.getLoggingLevel(event) > 0) {
					super_handleEvent(event);
				}
			}
		} else {
			super_handleEvent(event);
		}
	}
	
	
	
	
	
		protected BufferedWriter out;

		@Override
		public void closeFile() {
			try {
				this.out.write("</events>");
				// I added a "\n" to make it look nicer on the console.  Can't say if this may have unintended side
				// effects anywhere else.  kai, oct'12
				// fails signalsystems test (and presumably other tests in contrib/playground) since they compare
				// checksums of event files.  Removed that change again.  kai, oct'12
				this.out.close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Deprecated
		public void init(final String outfilename) {
			throw new RuntimeException("Please create a new instance.");
		}

		@Override
		public void reset(final int iter) {
		}

		protected void super_handleEvent(final Event event) {
			try {
				this.out.append("\t<event ");
				Map<String, String> attr = event.getAttributes();
				for (Map.Entry<String, String> entry : attr.entrySet()) {
					this.out.append(entry.getKey());
					this.out.append("=\"");
					this.out.append(encodeAttributeValue(entry.getValue()));
					this.out.append("\" ");
				}
				this.out.append(" />\n");
				
				if (EVGlobalData.data.IS_DEBUG_MODE){
					// TODO: this conditional statement can be removed, if this flush statement does not deteriorate performance 
					this.out.flush();
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// the following method was taken from MatsimXmlWriter in order to correctly encode attributes, but
		// to forego the overhead of using the full MatsimXmlWriter.
		/**
		 * Encodes the given string in such a way that it no longer contains
		 * characters that have a special meaning in xml.
		 * 
		 * @see <a href="http://www.w3.org/International/questions/qa-escapes#use">http://www.w3.org/International/questions/qa-escapes#use</a>
		 * @param attributeValue
		 * @return String with some characters replaced by their xml-encoding.
		 */
		private String encodeAttributeValue(final String attributeValue) {
			if (attributeValue == null) {
				return null;
			}
			int len = attributeValue.length();
			boolean encode = false;
			for (int pos = 0; pos < len; pos++) {
				char ch = attributeValue.charAt(pos);
				if (ch == '<') {
					encode = true;
					break;
				} else if (ch == '>') {
					encode = true;
					break;
				} else if (ch == '\"') {
					encode = true;
					break;
				} else if (ch == '&') {
					encode = true;
					break;
				}
			}
			if (encode) {
				StringBuffer bf = new StringBuffer();
				for (int pos = 0; pos < len; pos++) {
					char ch = attributeValue.charAt(pos);
					if (ch == '<') {
						bf.append("&lt;");
					} else if (ch == '>') {
						bf.append("&gt;");
					} else if (ch == '\"') {
						bf.append("&quot;");
					} else if (ch == '&') {
						bf.append("&amp;");
					} else {
						bf.append(ch);
					}
				}
				
				return bf.toString();
			}
			return attributeValue;

		}

}