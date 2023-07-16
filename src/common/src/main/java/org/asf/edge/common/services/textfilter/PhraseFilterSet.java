package org.asf.edge.common.services.textfilter;

import java.util.ArrayList;

/**
 * 
 * Set of filtered phrases
 * 
 * @author Sky Swimmer
 *
 */
public class PhraseFilterSet {

	private String setName;
	private String setDescription;
	private String filterReason;

	private ArrayList<PhraseFilter> phrases = new ArrayList<PhraseFilter>();

	public PhraseFilterSet(String name, String description, String reason) {
		this.setName = name;
		this.setDescription = description;
		this.filterReason = reason;
	}

	/**
	 * Adds phrase filters
	 * 
	 * @param severity Filter severity
	 * @param modes    Filter modes
	 * @param phrase   Phrase to filter
	 * @return PhraseFilter instance
	 */
	public PhraseFilter addPhraseFilter(FilterSeverity severity, FilterMode[] modes, String phrase) {
		return addPhraseFilter(severity, modes, filterReason, phrase);
	}

	/**
	 * Adds phrase filters
	 * 
	 * @param severity Filter severity
	 * @param modes    Filter modes
	 * @param reason   Filter reason
	 * @param phrase   Phrase to filter
	 * @param variants Variants
	 * @return PhraseFilter instance
	 */
	public PhraseFilter addPhraseFilter(FilterSeverity severity, FilterMode[] modes, String reason, String phrase,
			String... variants) {
		PhraseFilter filter = new PhraseFilter(this, modes, phrase, variants, reason, severity);
		phrases.add(filter);
		return filter;
	}

	/**
	 * Retrieves filtered phrases
	 * 
	 * @return Array of PhraseFilter instances
	 */
	public PhraseFilter[] getFilteredPhrases() {
		return phrases.toArray(t -> new PhraseFilter[t]);
	}

	/**
	 * Retrieves the set name
	 * 
	 * @return Set name string
	 */
	public String getSetName() {
		return setName;
	}

	/**
	 * Retrieves the set description
	 * 
	 * @return Set description string
	 */
	public String getSetDescription() {
		return setDescription;
	}

	/**
	 * Retrieves the reason string
	 * 
	 * @return Filter reason string
	 */
	public String getFilteringReason() {
		return filterReason;
	}

}
