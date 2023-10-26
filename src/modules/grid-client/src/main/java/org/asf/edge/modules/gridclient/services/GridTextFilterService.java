package org.asf.edge.modules.gridclient.services;

import org.asf.edge.common.services.textfilter.PhraseFilterSet;
import org.asf.edge.common.services.textfilter.TextFilterService;
import org.asf.edge.common.services.textfilter.result.FilterResult;

public class GridTextFilterService extends TextFilterService {

	private TextFilterService delegate;
	private PhraseFilterSet phoenixDynamicFilterSet = new PhraseFilterSet("SYS//GRIDFILTERSET",
			"Grid-controlled filter", "Grid-controlled filter");

	public GridTextFilterService(TextFilterService delegate) {
		this.delegate = delegate;
	}

	@Override
	public void initService() {
	}

	@Override
	public void reload() {
		delegate.reload();
	}

	@Override
	public boolean isFiltered(String text, boolean strictMode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean shouldFilterMute(String text) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public FilterResult filter(String text, boolean strictMode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PhraseFilterSet[] getFilterSets() {
		PhraseFilterSet[] arr = delegate.getFilterSets();
		PhraseFilterSet[] newA = new PhraseFilterSet[arr.length + 1];
		for (int i = 0; i < arr.length; i++)
			newA[i] = arr[i];
		newA[arr.length] = phoenixDynamicFilterSet;
		return newA;
	}

	@Override
	public PhraseFilterSet getFilterSet(String name) {
		if (name.equals(phoenixDynamicFilterSet.getSetName()))
			return phoenixDynamicFilterSet;
		return delegate.getFilterSet(name);
	}

	@Override
	public void addFilterSet(PhraseFilterSet set) {
		delegate.addFilterSet(set);
	}

}
