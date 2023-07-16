package org.asf.edge.contentserver.http.postprocessors;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.asf.connective.RemoteClient;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.edge.contentserver.config.ContentServerConfig;
import org.asf.edge.contentserver.http.ContentServerRequestHandler.IPreProcessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

public class ServerDownPreprocessor implements IPreProcessor {

	private ContentServerConfig config;

	public ServerDownPreprocessor(ContentServerConfig config) {
		this.config = config;
	}

	@Override
	public boolean match(String path, String method, RemoteClient client, String contentType, HttpRequest request,
			HttpResponse response, File sourceDir) {
		return path.endsWith("/ServerDown.xml");
	}

	@Override
	public InputStream preProcess(String path, String method, RemoteClient client, String contentType,
			HttpRequest request, HttpResponse response, InputStream source, File sourceDir) throws IOException {
		// Check
		if (config.serverTestEndpoint != null) {
			// Read data
			byte[] docData = source.readAllBytes();
			source.close();

			// Decode
			String data = new String(docData, "UTF-8");

			// Parse XML
			XmlMapper mapper = new XmlMapper();
			mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
			ObjectNode node = mapper.readValue(docData, ObjectNode.class);

			// Test connection
			try {
				// Open URL connection
				HttpURLConnection conn = (HttpURLConnection) new URL(config.serverTestEndpoint).openConnection();
				int code = conn.getResponseCode();
				if (code != 200 && code != 404)
					throw new IOException(); // Down
			} catch (Exception e) {
				// Modify
				node.set("Down", BooleanNode.TRUE);
				data = mapper.writer().withDefaultPrettyPrinter()
						.withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL).withRootName("ServerDown")
						.writeValueAsString(node);
			}

			// Set result
			source = new ByteArrayInputStream(data.getBytes("UTF-8"));
		}

		try {
			// Read data
			byte[] docData = source.readAllBytes();
			source.close();

			// Decode
			String data = new String(docData, "UTF-8");

			// Parse XML
			XmlMapper mapper = new XmlMapper();
			mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
			ObjectNode node = mapper.readValue(docData, ObjectNode.class);

			// Check XML scheduled maintenance
			if (node.has("Scheduled")) {
				// Pull start and end
				JsonNode scheduled = node.get("Scheduled");
				if (scheduled.has("EdgeStartTime")) {
					String startTime = scheduled.get("EdgeStartTime").asText();
					String endTime = null;
					if (scheduled.has("EdgeEndTime"))
						endTime = scheduled.get("EdgeEndTime").asText();

					// Parse times
					long end = -1;
					long start = -1;
					SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy HH:mm");
					fmt.setTimeZone(TimeZone.getTimeZone("PST"));
					start = fmt.parse(startTime).getTime();
					if (fmt.getTimeZone().inDaylightTime(new Date(start)))
						start -= 1 * 60 * 60 * 1000;
					if (endTime != null) {
						end = fmt.parse(endTime).getTime();
						if (fmt.getTimeZone().inDaylightTime(new Date(end)))
							end -= 1 * 60 * 60 * 1000;
					}
					if (System.currentTimeMillis() >= start && (end == -1 || System.currentTimeMillis() < end)) {
						// Modify
						node.set("Down", BooleanNode.TRUE);
						data = mapper.writer().withDefaultPrettyPrinter()
								.withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL).withRootName("ServerDown")
								.writeValueAsString(node);
					}
				}
			}

			// Set result
			source = new ByteArrayInputStream(data.getBytes("UTF-8"));
		} catch (Exception e) {
		}

		// Return
		return source;
	}

}
