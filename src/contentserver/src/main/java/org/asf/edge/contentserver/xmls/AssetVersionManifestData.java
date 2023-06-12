package org.asf.edge.contentserver.xmls;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class AssetVersionManifestData {

	@JsonProperty("A")
	@JacksonXmlElementWrapper(useWrapping = false)
	public AssetVersionBlock[] assets;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class AssetVersionBlock {

		@JacksonXmlProperty(localName = "N", isAttribute = true)
		public String name;

		@JsonProperty("V")
		@JacksonXmlElementWrapper(useWrapping = false)
		public AssetVariantBlock[] variants;

		@JsonIgnoreProperties(ignoreUnknown = true)
		@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
		public static class AssetVariantBlock {

			@JacksonXmlProperty(localName = "N", isAttribute = true)
			public long version;

			@JacksonXmlProperty(localName = "L", isAttribute = true)
			public String locale;

			@JacksonXmlProperty(localName = "S", isAttribute = true)
			public long size;

		}

	}

}
