package epilogue.powershell.shells;

import epilogue.Epilogue;
import epilogue.powershell.PowerShell;
import epilogue.module.Module;
import epilogue.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class Toggle extends PowerShell {
    public Toggle() {
        super(new ArrayList<>(Arrays.asList("Toggle")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            ChatUtil.sendFormatted(
                    String.format("%sUsage: .%s <&omodule&r>&r", Epilogue.clientName, args.get(0).toLowerCase(Locale.ROOT))
            );
        } else {
            Module module = Epilogue.moduleManager.getModule(args.get(1));
            if (module == null) {
                ChatUtil.sendFormatted(String.format("%sModule not found (&o%s&r)&r", Epilogue.clientName, args.get(1)));
            } else {
                boolean changed = true;
                if (args.size() >= 3) {
                    if (args.get(2).equalsIgnoreCase("true")
                            || args.get(2).equalsIgnoreCase("on")
                            || args.get(2).equalsIgnoreCase("1")) {
                        changed = !module.isEnabled();
                    } else if (args.get(2).equalsIgnoreCase("false")
                            || args.get(2).equalsIgnoreCase("off")
                            || args.get(2).equalsIgnoreCase("0")) {
                        changed = module.isEnabled();
                    }
                }
                if (changed) {
                    module.toggle();
                }
            }
        }
    }
}