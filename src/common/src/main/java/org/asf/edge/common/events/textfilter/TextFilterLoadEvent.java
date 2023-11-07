package org.asf.edge.common.events.textfilter;

import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.nexus.events.EventObject;
import org.asf.nexus.events.EventPath;

/**
 * 
 * Text filter load event - called after the filter has been loaded
 * 
 * @author Sky Swimmer
 *
 */
@EventPath("textfilter.load")
public class TextFilterLoadEvent extends EventObject {

	private TextFilterService service;

	@Override
	public String eventPath() {
		return "textfilter.load";
	}

	public TextFilterLoadEvent(TextFilterService service) {
		this.service = service;
	}

	/**
	 * Retrieves the text filter service
	 * 
	 * @return TextFilterService instance
	 */
	public TextFilterService getService() {
		return service;
	}

}
