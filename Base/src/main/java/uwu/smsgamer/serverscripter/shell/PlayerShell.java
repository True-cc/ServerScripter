package uwu.smsgamer.serverscripter.shell;

import uwu.smsgamer.serverscripter.utils.KillingTimer;
import uwu.smsgamer.serverscripter.utils.KillingTimerTask;

import java.util.*;

/**
 * A shell for a player. This runs commands in a separate thread.
 * If a command takes longer than the specified timeout, it will
 * be terminated and the command will return an error.
 */
public abstract class PlayerShell {
    public boolean isEnabled = false;
    public final UUID uuid;
    public final List<KillingTimerTask> tasks = new java.util.ArrayList<>();
    public final KillingTimer shellTimer;
    public final StringBuffer buffer = new StringBuffer();
    public final Shell<?> shell;

    /**
     * Creates a new player shell.
     * @param uuid The UUID of the player.
     * @param shell The shell associated with the player.
     */
    protected PlayerShell(UUID uuid, Shell<?> shell) {
        this.uuid = uuid;
        this.shell = shell;
        this.shellTimer = new KillingTimer(uuid.toString(), true, getTimeout());
    }

    /**
     * Called when the shell is enabled.
     */
    public void onEnable() {
        isEnabled = true;
    }

    /**
     * Called when the shell is disabled.
     */
    public void onDisable() {
        isEnabled = false;
    }

    /**
     * Returns the timeout in milliseconds.
     *
     * @return The timeout in milliseconds.
     */
    public long getTimeout() {
        return 10000L;
    }

    /**
     * Called when a command is executed.
     *
     * If the command takes longer than the timeout, it will be terminated.
     *
     * @param command The command that was executed.
     */
    public void onCommand(String command) {
        synchronized (tasks) {
            if (tasks.size() > 0) {
                announce("Please wait for the previous command" + (tasks.size() > 1 ? "s" : "") + " to finish.");
                return;
            }
            command = command.replace("\\ ", " ");
            if (command.equals("\\")) {
                print("Finished command.");
                command = buffer.toString();
                buffer.setLength(0);
            } else {
                print("> " + command);
                if (buffer.length() > 0) {
                    buffer.append("\n").append(command);
                    announce("Unfinished command.");
                    return;
                }
            }
            KillingTimerTask task;
            String finalCommand = command;
            task = new KillingTimerTask() {
                @Override
                public void run() {
                    try {
                        doExecute(finalCommand);
                    } catch (ThreadDeath e) {
                        throw e;
                    } catch (Throwable e) {
                        ShellManager.onError.accept(uuid, e);
                    }
                    tasks.remove(this);
                }

                @Override
                public void killed(boolean suspended) {
                    if (suspended) {
                        printError("Command timed out. (Suspended)");
                    } else {
                        printError("Command timed out.");
                    }
                    tasks.remove(this);
                }
            };
            tasks.add(task);
            shellTimer.schedule(task, 0L);
        }
    }

    public Result doExecute(String command) {
        Result result = execute(command);
        switch (result.response) {
            case UNFINISHED:
                buffer.append(result.output);
                announce("Unfinished command.");
                break;
            case EXIT:
                announce("Exit shell.");
                shell.removeShell(uuid);
        }
        return result;
    }

    public void print(String message) {
        ShellManager.onPrint.accept(uuid, message);
    }

    public void printError(String message) {
        ShellManager.onPrintError.accept(uuid, message);
    }

    public void error(Throwable e) {
        ShellManager.onError.accept(uuid, e);
    }

    public void announce(String message) {
        ShellManager.onAnnounce.accept(uuid, message);
    }

    /**
     * Executes the command. This method is called in a separate thread.
     *
     * @param command The command to execute.
     * @return The response.
     */
    public abstract Result execute(String command);

    /**
     * Sets an object to be used by the shell.
     *
     * @param name The name of the object.
     * @param object The object.
     */
    public abstract void setObject(String name, Object object);

    public static class Result {
        public enum Response {
            FINISHED,
            UNFINISHED,
            EXIT,
        }

        public static final Result UNFINISHED = new Result(Response.UNFINISHED);
        public static final Result EXIT = new Result(Response.EXIT);
        public static final Result EMPTY = new Result(Response.FINISHED, "");

        public final Response response;
        public final String output;

        public Result(Response response, String output) {
            this.response = response;
            this.output = output;
        }

        public Result(Response response) {
            this.response = response;
            this.output = null;
        }

        public Result(String output) {
            this(Response.FINISHED, output);
        }
    }
}
