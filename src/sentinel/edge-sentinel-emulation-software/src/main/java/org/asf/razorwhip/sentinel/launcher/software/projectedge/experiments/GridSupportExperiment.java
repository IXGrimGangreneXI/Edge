package org.asf.razorwhip.sentinel.launcher.software.projectedge.experiments;

import java.io.File;
import java.io.IOException;

import org.asf.razorwhip.sentinel.launcher.LauncherUtils;
import org.asf.razorwhip.sentinel.launcher.experiments.api.IExperiment;

public class GridSupportExperiment implements IExperiment {

	@Override
	public void onLoad() {
		// Copy module files
		File source = new File("multiplayergrid-experiment");
		if (source.exists()) {
			try {
				LauncherUtils.copyDirWithoutProgress(source, new File("server"));
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void onEnable() {
	}

	@Override
	public void onDisable() {
		// Delete module files
		File source = new File("multiplayergrid-experiment");
		if (source.exists()) {
			deleteBySource(source, new File("server"));
		}
	}

	private void deleteBySource(File source, File target) {
		if (!target.exists())
			return;
		for (File ch : source.listFiles(t -> t.isDirectory()))
			deleteBySource(ch, new File(target, ch.getName()));
		for (File ch : source.listFiles(t -> t.isFile())) {
			File tF = new File(target, ch.getName());
			if (tF.exists())
				tF.delete();
		}
		if (target.listFiles().length == 0)
			target.delete();
	}

}
