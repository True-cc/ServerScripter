package uwu.smsgamer.serverscripter.spigot.utils;

import org.bukkit.event.*;
import org.bukkit.plugin.*;
import uwu.smsgamer.serverscripter.scripts.Script;
import uwu.smsgamer.serverscripter.senapi.utils.Pair;
import uwu.smsgamer.serverscripter.spigot.SpigotServerScripter;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

/**
 * Helper class to make listening to bukkit events much easier. Not necessary though.
 */
@SuppressWarnings("unused")
public class ScriptListenerHelper extends RegisteredListener {
    public static ScriptListenerHelper lowestListener;
    public static ScriptListenerHelper lowListener;
    public static ScriptListenerHelper listener;
    public static ScriptListenerHelper highListener;
    public static ScriptListenerHelper highestListener;
    public static ScriptListenerHelper monitorListener;

    public static void init() {
        lowestListener = new ScriptListenerHelper(EventPriority.LOWEST, SpigotServerScripter.getInstance());
        lowListener = new ScriptListenerHelper(EventPriority.LOW, SpigotServerScripter.getInstance());
        listener = new ScriptListenerHelper(EventPriority.NORMAL, SpigotServerScripter.getInstance());
        highListener = new ScriptListenerHelper(EventPriority.HIGH, SpigotServerScripter.getInstance());
        highestListener = new ScriptListenerHelper(EventPriority.HIGHEST, SpigotServerScripter.getInstance());
        monitorListener = new ScriptListenerHelper(EventPriority.MONITOR, SpigotServerScripter.getInstance());
    }

    public static void registerEvent(Class<? extends Event> type, EventPriority priority, Consumer<Event> function, Script script) {
        switch (priority) {
            case LOWEST:
                lowestListener.registerFunction(type, function, script);
                break;
            case LOW:
                lowListener.registerFunction(type, function, script);
                break;
            case NORMAL:
                listener.registerFunction(type, function, script);
                break;
            case HIGH:
                highListener.registerFunction(type, function, script);
                break;
            case HIGHEST:
                highestListener.registerFunction(type, function, script);
                break;
            case MONITOR:
                monitorListener.registerFunction(type, function, script);
                break;
        }
    }

    public static void unregisterEvent(Class<? extends Event> type, EventPriority priority, Consumer<Event> function) {
        switch (priority) {
            case LOWEST:
                lowestListener.unregisterFunction(type, function);
                break;
            case LOW:
                lowListener.unregisterFunction(type, function);
                break;
            case NORMAL:
                listener.unregisterFunction(type, function);
                break;
            case HIGH:
                highListener.unregisterFunction(type, function);
                break;
            case HIGHEST:
                highestListener.unregisterFunction(type, function);
                break;
            case MONITOR:
                monitorListener.unregisterFunction(type, function);
                break;
        }
    }

    private static HandlerList getEventListeners(Class<? extends Event> type) {
        try {
            Method method = getRegistrationClass(type).getDeclaredMethod("getHandlerList");
            method.setAccessible(true);
            return (HandlerList) method.invoke(null);
        } catch (Exception e) {
            throw new IllegalPluginAccessException(e.toString());
        }
    }

    private static Class<? extends Event> getRegistrationClass(Class<? extends Event> clazz) {
        try {
            clazz.getDeclaredMethod("getHandlerList");
            return clazz;
        } catch (NoSuchMethodException e) {
            if (clazz.getSuperclass() != null
              && !clazz.getSuperclass().equals(Event.class)
              && Event.class.isAssignableFrom(clazz.getSuperclass())) {
                return getRegistrationClass(clazz.getSuperclass().asSubclass(Event.class));
            } else {
                throw new IllegalPluginAccessException("Unable to find handler list for event " + clazz.getName() + ". Static getHandlerList method required!");
            }
        }
    }

    private final HashMap<Class<? extends Event>, Set<Pair<Consumer<Event>, Script>>> functions = new HashMap<>();

    public ScriptListenerHelper(EventPriority priority, Plugin plugin) {
        super(null, null, priority, plugin, false);
    }

    public void registerFunction(Class<? extends Event> event, Consumer<Event> fun, Script script) {
        functions.computeIfAbsent(event, k -> {
            getEventListeners(event).register(this);
            return new HashSet<>();
        }).add(new Pair<>(fun, script));
    }

    public void unregisterFunction(Class<? extends Event> event, Consumer<Event> fun) {
        functions.computeIfPresent(event, (k, v) -> {
            v.removeIf(p -> p.a.equals(fun));
            if (v.isEmpty()) {
                getEventListeners(event).unregister(this);
                return null;
            }
            return v;
        });
    }

    @Override
    public Listener getListener() {
        return new Listener(){
            @Override
            public boolean equals(Object o) {
                return false;
            }
        };
    }

    @Override
    public void callEvent(Event event) {
        Set<Pair<Consumer<Event>, Script>> funs = functions.get(event.getClass());
        if (funs != null) {
            Set<Pair<Consumer<Event>, Script>> toRemove = new HashSet<>();
            for (Pair<Consumer<Event>, Script> fun : funs) {
                if (fun.b.isLoaded()) {
                    fun.a.accept(event);
                } else {
                    toRemove.add(fun);
                }
            }
            toRemove.forEach(fun -> unregisterFunction(event.getClass(), fun.a)
            );
        }
    }
}
