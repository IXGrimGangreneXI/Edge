package org.asf.edge.common.services.commondata;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * 
 * Common data container
 * 
 * @author Sky Swimmer
 *
 */
public abstract class CommonKvDataContainer {

	protected CommonKvDataContainer parent;
	protected String name = "";
	protected String path = "";

	public class ContainerUnsafe {
		public JsonElement get(String key) throws IOException {
			return CommonKvDataContainer.this.get(key);
		}

		public void set(String key, JsonElement value) throws IOException {
			CommonKvDataContainer.this.set(key, value);
		}

		public void create(String key, JsonElement value) throws IOException {
			CommonKvDataContainer.this.create(key, value);
		}

		public boolean exists(String key) throws IOException {
			return CommonKvDataContainer.this.exists(key);
		}

		public void delete(String key) throws IOException {
			CommonKvDataContainer.this.delete(key);
		}

		public void deleteContainer(String root) throws IOException {
			CommonKvDataContainer.this.deleteContainer(root);
		}

		public String[] getEntryKeys(String key) throws IOException {
			return CommonKvDataContainer.this.getEntryKeys(key);
		}

		public String[] getChildContainers(String key) throws IOException {
			return CommonKvDataContainer.this.getChildContainers(key);
		}

		public JsonElement find(BiFunction<String, JsonElement, Boolean> function, String root) throws IOException {
			return CommonKvDataContainer.this.find(function, root);
		}

		public void runFor(BiFunction<String, JsonElement, Boolean> function, String root) throws IOException {
			CommonKvDataContainer.this.runFor(function, root);
		}

		public void runForChildren(Function<String, Boolean> function, String root) throws IOException {
			CommonKvDataContainer.this.runForChildren(function, root);
		}
	}

	/**
	 * Utility to directly access the data container functions that are protected,
	 * not recommended except for remote data containers
	 */
	public ContainerUnsafe unsafeAccessor() {
		return new ContainerUnsafe();
	}

	/**
	 * Retrieves the parent data container
	 * 
	 * @return CommonDataContainer instance or null
	 */
	public CommonKvDataContainer getParent() {
		return parent;
	}

	/**
	 * Called to retrieve elements
	 * 
	 * @param key Element key
	 * @return JsonElement instance or null
	 * @throws IOException If the entry cannot be retrieved
	 */
	protected abstract JsonElement get(String key) throws IOException;

	/**
	 * Called to assign elements
	 * 
	 * @param key   Element key
	 * @param value Value to assign
	 * @throws IOException If the entry cannot be assigned
	 */
	protected abstract void set(String key, JsonElement value) throws IOException;

	/**
	 * Called to create elements
	 * 
	 * @param key   Element key
	 * @param value Value to assign
	 * @throws IOException If the entry cannot be assigned
	 */
	protected abstract void create(String key, JsonElement value) throws IOException;

	/**
	 * Called to check if keys exist
	 * 
	 * @param key Key to check
	 * @return True if present, false otherwise
	 * @throws IOException If the entry cannot be verified
	 */
	protected abstract boolean exists(String key) throws IOException;

	/**
	 * Called to delete elements
	 * 
	 * @param key Element key
	 * @throws IOException If the deletion errors
	 */
	protected abstract void delete(String key) throws IOException;

	/**
	 * Called to retrieve entry keys
	 * 
	 * @param key Parent container key
	 * @return Array of container strings
	 * @throws IOException If retrieval fails
	 */
	protected abstract String[] getEntryKeys(String key) throws IOException;

	/**
	 * Called to retrieve child containers
	 * 
	 * @param key Parent container key
	 * @return Array of container strings
	 * @throws IOException If retrieval fails
	 */
	protected abstract String[] getChildContainers(String key) throws IOException;

	/**
	 * Called to delete containers
	 * 
	 * @param root Root key
	 * @throws IOException If deletion fails
	 */
	protected abstract void deleteContainer(String root) throws IOException;

	/**
	 * Runs a function for all entries, returns the entry if the function returns
	 * true
	 * 
	 * @param function Function to run
	 * @param root     Root key
	 * @return JsonElement instance or null
	 * @throws IOException If processing fails
	 */
	protected abstract JsonElement find(BiFunction<String, JsonElement, Boolean> function, String root)
			throws IOException;

	/**
	 * Runs a function for all entries
	 * 
	 * @param function Function to run
	 * @param root     Root key
	 * @throws IOException If processing fails
	 */
	protected abstract void runFor(BiFunction<String, JsonElement, Boolean> function, String root) throws IOException;

	/**
	 * Runs a function for all child containers
	 * 
	 * @param function Function to run
	 * @param root     Root key
	 * @throws IOException If processing fails
	 */
	protected abstract void runForChildren(Function<String, Boolean> function, String root) throws IOException;

	private boolean validName(String key) {
		// Check if internal
		if (key.contains("/") || key.equals("chholder"))
			return false;

		// Check validity
		if (key.replace(" ", "").trim().isEmpty())
			return false;

		// Check length
		if (key.length() > 64)
			return false;

		// Valid
		return true;
	}

	/**
	 * Retrieves data entries
	 * 
	 * @param key Entry key to retrieve
	 * @return JsonElement instance or null
	 * @throws IOException
	 */
	public JsonElement getEntry(String key) throws IOException {
		if (!validName(key))
			return null;
		return get(key);
	}

	/**
	 * Verifies if data entries exist
	 * 
	 * @param key Key to check
	 * @return True if present, false otherwise
	 * @throws IOException If the entry cannot be verified
	 */
	public boolean entryExists(String key) throws IOException {
		if (!validName(key))
			return false;
		return exists(key);
	}

	/**
	 * Assigns data entries
	 * 
	 * @param key   Element key
	 * @param value Value to assign
	 * @throws IOException If the entry cannot be assigned
	 */
	public void setEntry(String key, JsonElement value) throws IOException {
		if (!validName(key))
			throw new IOException("Invalid key");

		// Check if it previously existed
		boolean existed = exists(key);

		// Set
		if (existed)
			set(key, value);
		else
			create(key, value);
	}

	/**
	 * Deletes data entries
	 * 
	 * @param key Element key
	 * @throws IOException If the entry cannot be assigned
	 */
	public void deleteEntry(String key) throws IOException {
		if (!validName(key))
			throw new IOException("Invalid key");

		// Check if it exists
		if (exists(key)) {
			// Delete
			delete(key);
		}
	}

	/**
	 * Retrieves child data containers
	 * 
	 * @param key Container key
	 * @return CommonDataContainer instance
	 * @throws IOException If loading the container fails
	 */
	public CommonKvDataContainer getChildContainer(String key) throws IOException {
		// Verify name
		if (!validName(key))
			throw new IOException("Invalid key");

		// Verify path length limit
		if ((path + key + "/").length() + 64 > 256)
			throw new IOException("Invalid key: key name too long, path length limit would be hit");

		// Return container
		return new ChildDataContainer(path + key + "/", key, this);
	}

	/**
	 * Retrieves entry keys
	 * 
	 * @return Array of entry key strings
	 * @throws IOException If retrieval fails
	 */
	public String[] getEntryKeys() throws IOException {
		return Stream.of(getEntryKeys("")).filter(t -> !t.equalsIgnoreCase("chholder")).toArray(t -> new String[t]);
	}

	/**
	 * Retrieves child containers
	 * 
	 * @return Array of child container name strings
	 * @throws IOException If retrieval fails
	 */
	public String[] getChildContainers() throws IOException {
		return getChildContainers("");
	}

	/**
	 * Runs a function for all entries, returns the entry if the function returns
	 * true
	 * 
	 * @param function Function to run (if it returns true the entry is a match and
	 *                 will be returned, iterates until it returns true)
	 * @return JsonElement instance or null
	 * @throws IOException If processing fails
	 */
	public JsonElement findEntry(BiFunction<String, JsonElement, Boolean> function) throws IOException {
		return find(function, "");
	}

	/**
	 * Runs a function for all entries
	 * 
	 * @param function Function to run (iterates until it returns false)
	 * @throws IOException If processing fails
	 */
	public void runForEntries(BiFunction<String, JsonElement, Boolean> function) throws IOException {
		runFor(function, "");
	}

	/**
	 * Runs a function for all child containers
	 * 
	 * @param function Function to run (iterates until it returns false)
	 * @throws IOException If processing fails
	 */
	public void runForChildContainers(Function<String, Boolean> function) throws IOException {
		runForChildren(function, "");
	}

	/**
	 * Deletes the data container
	 * 
	 * @throws IOException If deletion fails
	 */
	public void deleteContainer() throws IOException {
		deleteContainer("");
	}

	protected void initIfNeeded() throws IOException {
	}

	private class ChildDataContainer extends CommonKvDataContainer {

		private boolean inited = false;

		public ChildDataContainer(String path, String name, CommonKvDataContainer parent) {
			this.path = path;
			this.parent = parent;
			this.name = name;
		}

		@Override
		protected void initIfNeeded() throws IOException {
			// Check
			if (inited)
				return;
			inited = true;

			// Create holder
			if (!exists("chholder"))
				create("chholder", new JsonPrimitive(true));

			// Call parent
			parent.initIfNeeded();
		}

		@Override
		protected JsonElement get(String key) throws IOException {
			return parent.get(name + "/" + key);
		}

		@Override
		protected boolean exists(String key) throws IOException {
			return parent.exists(name + "/" + key);
		}

		@Override
		protected void set(String key, JsonElement value) throws IOException {
			parent.set(name + "/" + key, value);
			initIfNeeded();
		}

		@Override
		protected void create(String key, JsonElement value) throws IOException {
			parent.create(name + "/" + key, value);
			initIfNeeded();
		}

		@Override
		protected void delete(String key) throws IOException {
			parent.delete(name + "/" + key);
		}

		@Override
		protected String[] getEntryKeys(String key) throws IOException {
			return parent.getEntryKeys(name + (key.isEmpty() ? "" : "/" + key));
		}

		@Override
		protected String[] getChildContainers(String key) throws IOException {
			return parent.getChildContainers(name + (key.isEmpty() ? "" : "/" + key));
		}

		@Override
		protected void deleteContainer(String root) throws IOException {
			parent.deleteContainer(name + (root.isEmpty() ? "" : "/" + root));
		}

		@Override
		protected JsonElement find(BiFunction<String, JsonElement, Boolean> function, String root) throws IOException {
			return parent.find(function, name + (root.isEmpty() ? "" : "/" + root));
		}

		@Override
		protected void runFor(BiFunction<String, JsonElement, Boolean> function, String root) throws IOException {
			parent.runFor(function, name + (root.isEmpty() ? "" : "/" + root));
		}

		@Override
		protected void runForChildren(Function<String, Boolean> function, String root) throws IOException {
			parent.runForChildren(function, name + (root.isEmpty() ? "" : "/" + root));
		}

	}

}
