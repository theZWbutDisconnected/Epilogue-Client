package epilogue.powershell.shells;

import epilogue.Epilogue;
import epilogue.powershell.PowerShell;
import epilogue.module.Module;
import epilogue.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class Show extends PowerShell {
    public Show() {
        super(new ArrayList<>(Arrays.asList("Show")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            ChatUtil.sendFormatted(
                    String.format("%sUsage: .%s <&omodule&r>&r", Epilogue.clientName, args.get(0).toLowerCase(Locale.ROOT))
            );
        } else if (!args.get(1).equals("*")) {
            Module module = Epilogue.moduleManager.getModule(args.get(1));
            if (module == null) {
                ChatUtil.sendFormatted(String.format("%sModule &o%s&r not found&r", Epilogue.clientName, args.get(1)));
            } else if (!module.isHidden()) {
                ChatUtil.sendFormatted(String.format("%s&o%s&r is not hidden in HUD&r", Epilogue.clientName, module.getName()));
            } else {
                module.setHidden(false);
                ChatUtil.sendFormatted(String.format("%s&o%s&r is no longer hidden in HUD&r", Epilogue.clientName, module.getName()));
            }
        } else {
            for (Module module : Epilogue.moduleManager.modules.values()) {
                module.setHidden(false);
            }
            ChatUtil.sendFormatted(String.format("%sAll modules are no longer hidden in HUD&r", Epilogue.clientName));
        }
    }
}
