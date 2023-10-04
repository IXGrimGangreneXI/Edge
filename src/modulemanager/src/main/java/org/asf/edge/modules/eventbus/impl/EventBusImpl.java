package org.asf.edge.modules.eventbus.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.edge.modules.eventbus.EventBus;
import org.asf.edge.modules.eventbus.EventListener;
import org.asf.edge.modules.eventbus.EventObject;
import org.asf.edge.modules.eventbus.EventPath;
import org.asf.edge.modules.eventbus.IEventReceiver;

public class EventBusImpl extends EventBus {

	private EventBus parent;
	private HashMap<String, ArrayList<Consumer<?>>> listeners = new HashMap<String, ArrayList<Consumer<?>>>();
	private Logger eventLog = LogManager.getLogger("EVENTBUS");
	private ArrayList<IEventReceiver> boundReceivers = new ArrayList<IEventReceiver>();

	@SuppressWarnings("rawtypes")
	private static class EventContainerListener implements Consumer {

		public IEventReceiver owner;
		public Consumer delegate;

		@Override
		@SuppressWarnings("unchecked")
		public void accept(Object t) {
			delegate.accept(t);
		}

	}

	@Override
	public void addAllEventsFromReceiver(IEventReceiver receiver) {
		// Check
		if (boundReceivers.contains(receiver))
			return;
		boundReceivers.add(receiver);

		// Log subscription
		eventLog.info("Registering all events in " + receiver.getClass().getTypeName() + "...");

		// Loop through the class and register events
		for (Method meth : receiver.getClass().getMethods()) {
			if (meth.isAnnotationPresent(EventListener.class) && Modifier.isPublic(meth.getModifiers())
					&& !Modifier.isAbstract(meth.getModifiers())) {
				// Find the event object
				if (meth.getParameterCount() == 1 && EventObject.class.isAssignableFrom(meth.getParameterTypes()[0])) {
					// Find event path
					Class<?> eventType = meth.getParameterTypes()[0];
					if (eventType.isAnnotationPresent(EventPath.class)) {
						EventPath info = eventType.getAnnotation(EventPath.class);

						// Add listener
						meth.setAccessible(true);
						String path = info.value();
						if (!listeners.containsKey(path)) {
							synchronized (listeners) {
								if (!listeners.containsKey(path))
									listeners.put(path, new ArrayList<Consumer<?>>());
							}
						}
						ArrayList<Consumer<?>> events = listeners.get(path);
						synchronized (events) {
							EventContainerListener l = new EventContainerListener();
							l.owner = receiver;
							l.delegate = t -> {
								try {
									meth.invoke(receiver, t);
								} catch (IllegalAccessException | IllegalArgumentException
										| InvocationTargetException e) {
									throw new RuntimeException(e);
								}
							};
							eventLog.debug("Attaching event handler " + receiver.getClass().getTypeName() + ":"
									+ meth.getName() + " to event " + info.value());
							events.add(l);
						}
					}

				}
			}
		}
	}

	@Override
	public void removeAllEventsFromReceiver(IEventReceiver receiver) {
		// Check
		if (!boundReceivers.contains(receiver))
			return;
		boundReceivers.remove(receiver);

		// Log subscription
		eventLog.info("De-registering all events in " + receiver.getClass().getTypeName() + "...");

		// Loop through the class and de-register events
		for (Method meth : receiver.getClass().getMethods()) {
			if (meth.isAnnotationPresent(EventListener.class) && Modifier.isPublic(meth.getModifiers())
					&& !Modifier.isAbstract(meth.getModifiers())) {
				// Find the event object
				if (meth.getParameterCount() == 1 && EventObject.class.isAssignableFrom(meth.getParameterTypes()[0])) {
					// Find event path
					Class<?> eventType = meth.getParameterTypes()[0];
					if (eventType.isAnnotationPresent(EventPath.class)) {
						EventPath info = eventType.getAnnotation(EventPath.class);

						// Find listeners
						meth.setAccessible(true);
						String path = info.value();
						if (listeners.containsKey(path)) {
							ArrayList<Consumer<?>> events = listeners.get(path);
							synchronized (events) {
								// Remove
								Consumer<?>[] evs = events.toArray(t -> new Consumer<?>[t]);
								for (Consumer<?> ev : evs) {
									if (ev instanceof EventContainerListener) {
										EventContainerListener l = (EventContainerListener) ev;
										if (l.owner == receiver) {
											eventLog.debug(
													"Detaching event handler " + receiver.getClass().getTypeName() + ":"
															+ meth.getName() + " from event " + info.value());
											events.remove(l);
										}
									}
								}
							}
						}
					}

				}
			}
		}
	}

	@Override
	public <T extends EventObject> void addEventHandler(Class<T> eventClass, Consumer<T> eventHandler) {
		EventPath info = eventClass.getAnnotation(EventPath.class);

		// Add listener
		String path = info.value();
		if (!listeners.containsKey(path)) {
			synchronized (listeners) {
				if (!listeners.containsKey(path))
					listeners.put(path, new ArrayList<Consumer<?>>());
			}
		}
		ArrayList<Consumer<?>> events = listeners.get(path);
		synchronized (events) {
			events.add(eventHandler);
			eventLog.debug("Attaching event handler " + eventHandler + " to event " + info.value());
		}
	}

	@Override
	public <T extends EventObject> void removeEventHandler(Class<T> eventClass, Consumer<T> eventHandler) {
		EventPath info = eventClass.getAnnotation(EventPath.class);

		// Add listener
		String path = info.value();
		if (!listeners.containsKey(path))
			return;
		ArrayList<Consumer<?>> events = listeners.get(path);
		synchronized (events) {
			events.remove(eventHandler);
			eventLog.debug("Detaching event handler " + eventHandler + " from event " + info.value());
		}
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void dispatchEvent(EventObject event) {
		if (parent != null)
			parent.dispatchEvent(event);
		if (listeners.containsKey(event.eventPath())) {
			// Dispatch event
			ArrayList<Consumer<?>> events = this.listeners.get(event.eventPath());
			Consumer<?>[] evs;
			synchronized (events) {
				evs = events.toArray(t -> new Consumer<?>[t]);
			}
			for (Consumer ev : evs) {
				ev.accept(event);
			}
		}
	}

	@Override
	public EventBus createBus() {
		EventBusImpl ev = new EventBusImpl();
		ev.parent = this;
		return ev;
	}
}
