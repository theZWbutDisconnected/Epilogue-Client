package epilogue.powershell.shells;

import epilogue.Epilogue;
import epilogue.powershell.PowerShell;
import epilogue.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class Help extends PowerShell {
    public Help() {
        super(new ArrayList<>(Arrays.asList("Help")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (!Epilogue.moduleManager.modules.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sCommands:&r", Epilogue.clientName));
            for (PowerShell powerShell : Epilogue.handler.powerShells) {
                if (!(powerShell instanceof Module)) {
                    ChatUtil.sendFormatted(String.format("&7»&r .%s&r", String.join(" &7/&r .", powerShell.names)));
                }
            }
        }
    }
}
