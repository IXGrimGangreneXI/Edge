package org.asf.edge.commonapi.http.handlers.api.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

import org.asf.connective.RemoteClient;
import org.asf.connective.impl.http_1_1.RemoteClientHttp_1_1;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.http.EdgeWebService;
import org.asf.edge.common.http.functions.LegacyFunction;
import org.asf.edge.common.http.functions.LegacyFunctionInfo;
import org.asf.edge.commonapi.EdgeCommonApiServer;
import org.asf.edge.commonapi.util.MmoServerEntry;
import org.asf.edge.commonapi.xmls.servers.MMOServerInfoBlock;
import org.asf.edge.commonapi.xmls.servers.MMOServerInfoList;

public class ConfigurationWebServiceProcessor extends EdgeWebService<EdgeCommonApiServer> {

	public ConfigurationWebServiceProcessor(EdgeCommonApiServer server) {
		super(server);
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new ConfigurationWebServiceProcessor(getServerInstance());
	}

	@Override
	public String path() {
		return "/ConfigurationWebService.asmx";
	}

	@Override
	public void fallbackRequestProcessor(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		// Handle request
		path = path;
		setResponseStatus(404, "Not found");
	}

	@LegacyFunction(allowedMethods = { "POST" })
	public void getMMOServerInfoWithZone(LegacyFunctionInfo info) throws IOException {
		// Handle server list request
		ServiceRequestInfo req = getUtilities().getServiceRequestPayload(getServerInstance().getLogger());
		if (req == null)
			return;

		// Create list
		MmoServerEntry[] servers = getServerInstance().getMmoServers();
		MMOServerInfoList lst = new MMOServerInfoList();
		ArrayList<MMOServerInfoBlock> serverLs = new ArrayList<MMOServerInfoBlock>();
		for (MmoServerEntry server : servers) {
			// Create server
			for (String zone : server.zones) {
				// Determine address
				String addr = server.address;
				if (addr.equals("localhost")) {
					// Get local IP
					String host = "localhost";
					if (info.getClient() instanceof RemoteClientHttp_1_1) {
						RemoteClientHttp_1_1 cl = (RemoteClientHttp_1_1) info.getClient();
						Socket sock = cl.getSocket();

						// Get interface
						SocketAddress ad = sock.getLocalSocketAddress();
						if (ad instanceof InetSocketAddress) {
							InetSocketAddress iA = (InetSocketAddress) ad;
							host = iA.getAddress().getCanonicalHostName();
							addr = host;
						}
					}
					if (host.equals("localhost"))
						host = "127.0.0.1";
				}

				// Create block
				MMOServerInfoBlock srv = new MMOServerInfoBlock();
				srv.serverAddress = new MMOServerInfoBlock.StringWrapper(addr);
				srv.port = new MMOServerInfoBlock.IntWrapper(server.port);
				srv.isDefault = new MMOServerInfoBlock.BooleanWrapper(!server.isBackupServer);
				srv.rootZoneName = new MMOServerInfoBlock.StringWrapper(server.rootZone);
				srv.zoneName = new MMOServerInfoBlock.StringWrapper(zone);
				serverLs.add(srv);
			}
		}
		lst.servers = serverLs.toArray(t -> new MMOServerInfoBlock[t]);
		setResponseContent("text/xml", req.generateXmlValue("ArrayOfMMOServerInfo", lst));
	}
}
