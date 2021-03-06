package net.cassite.vproxy.app.cmd.handle.param;

import net.cassite.vproxy.app.cmd.Command;
import net.cassite.vproxy.app.cmd.Param;
import net.cassite.vproxy.component.check.HealthCheckConfig;

public class HealthCheckHandle {
    private HealthCheckHandle() {
    }

    public static HealthCheckConfig getHealthCheckConfig(Command cmd) throws Exception {
        int timeout = Integer.parseInt(cmd.args.get(Param.timeout));
        int period = Integer.parseInt(cmd.args.get(Param.period));
        int up = Integer.parseInt(cmd.args.get(Param.up));
        int down = Integer.parseInt(cmd.args.get(Param.down));

        if (timeout < 0 || period < 0 || up < 0 || down < 0)
            throw new Exception("invalid health check config");
        return new HealthCheckConfig(timeout, period, up, down);
    }
}
