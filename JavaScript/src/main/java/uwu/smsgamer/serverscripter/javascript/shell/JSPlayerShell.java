package uwu.smsgamer.serverscripter.javascript.shell;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.ShellContextFactory;
import uwu.smsgamer.serverscripter.shell.PlayerShell;
import uwu.smsgamer.serverscripter.shell.ShellManager;
import uwu.smsgamer.serverscripter.utils.KillingTimerTask;

import java.util.TimerTask;
import java.util.UUID;

public class JSPlayerShell extends PlayerShell {
    private Context cx;
    private Scriptable scope;

    protected JSPlayerShell(UUID uuid) {
        super(uuid, JSShell.getInstance());
        ShellContextFactory factory = new ShellContextFactory();
        factory.setLanguageVersion(Context.VERSION_ES6);
        shellTimer.schedule(() -> {
            cx = factory.enterContext();
            scope = cx.initStandardObjects();
        }, 0);
    }

    @Override
    public Result doExecute(String command) {
        Result r = super.doExecute(command);
        if (r.response == Result.Response.FINISHED) {
            print(r.output);
        }
        return r;
    }

    @Override
    public Result execute(String command) {
        Object result = cx.evaluateString(scope, command, "shell", 1, null);
        if (result == null) {
            return Result.UNFINISHED;
        }
        return new Result(Result.Response.FINISHED, result.toString());
    }

    @Override
    public void setObject(String name, Object object) {
        shellTimer.schedule(() -> scope.put(name, scope, object), 0);
    }
}
