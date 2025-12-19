package epilogue.powershell.shells;

import epilogue.Epilogue;
import epilogue.powershell.PowerShell;
import epilogue.module.Module;
import epilogue.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class List extends PowerShell {
    public List() {
        super(new ArrayList<>(Arrays.asList("List")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (!Epilogue.moduleManager.modules.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sModules:&r", Epilogue.clientName));
            for (Module module : Epilogue.moduleManager.modules.values()) {
                ChatUtil.sendFormatted(String.format("%s»&r %s&r", module.isHidden() ? "&8" : "&7", module.formatModule()));
            }
        }
    }
}
