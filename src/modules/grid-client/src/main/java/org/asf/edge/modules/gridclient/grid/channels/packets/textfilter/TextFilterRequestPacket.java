package org.asf.edge.modules.gridclient.grid.channels.packets.textfilter;

import java.io.IOException;

import org.asf.edge.modules.gridclient.phoenix.networking.DataReader;
import org.asf.edge.modules.gridclient.phoenix.networking.DataWriter;
import org.asf.edge.modules.gridclient.phoenix.networking.packets.IPhoenixPacket;

public class TextFilterRequestPacket implements IPhoenixPacket {

	public String message;
	public boolean strictChat;

	@Override
	public IPhoenixPacket instantiate() {
		return new TextFilterRequestPacket();
	}

	@Override
	public void parse(DataReader reader) throws IOException {
	}

	@Override
	public void build(DataWriter writer) throws IOException {
		writer.writeString(message);
		writer.writeBoolean(strictChat);
	}

}
