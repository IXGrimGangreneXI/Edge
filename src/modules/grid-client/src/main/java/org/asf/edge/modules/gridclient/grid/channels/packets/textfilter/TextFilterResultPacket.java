package org.asf.edge.modules.gridclient.grid.channels.packets.textfilter;

import java.io.IOException;

import org.asf.edge.common.services.textfilter.FilterSeverity;
import org.asf.edge.modules.gridclient.phoenix.networking.DataReader;
import org.asf.edge.modules.gridclient.phoenix.networking.DataWriter;
import org.asf.edge.modules.gridclient.phoenix.networking.packets.IPhoenixPacket;

public class TextFilterResultPacket implements IPhoenixPacket {

	public boolean success;

	public FilterResult result;

	public static class WordMatch {
		public String phrase;
		public String matchedPhrase;
		public String reason;
		public FilterSeverity severity;
	}

	public static class FilterResult {
		public boolean isFiltered;
		public String filteredResult;
		public WordMatch[] matches;
		public FilterSeverity severity;
	}

	@Override
	public IPhoenixPacket instantiate() {
		return new TextFilterResultPacket();
	}

	@Override
	public void parse(DataReader reader) throws IOException {
		success = reader.readBoolean();
		if (success) {
			// Read result
			boolean wasFiltered = reader.readBoolean();
			String message = reader.readString();
			WordMatch[] matches = new WordMatch[reader.readInt()];
			for (int i = 0; i < matches.length; i++) {
				// Read match
				String matchedPhrase = reader.readString();
				String phrase = reader.readString();
				String reason = reader.readString();
				FilterSeverity severity = FilterSeverity.values()[reader.readRawByte()];
				WordMatch match = new WordMatch();
				match.matchedPhrase = matchedPhrase;
				match.phrase = phrase;
				match.reason = reason;
				match.severity = severity;
				matches[i] = match;
			}
			FilterSeverity severity = FilterSeverity.values()[reader.readRawByte()];

			// Assign
			result = new FilterResult();
			result.isFiltered = wasFiltered;
			result.filteredResult = message;
			result.matches = matches;
			result.severity = severity;
		}
	}

	@Override
	public void build(DataWriter writer) throws IOException {
	}

}
