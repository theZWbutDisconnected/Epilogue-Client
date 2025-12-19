package epilogue.powershell.shells;

import epilogue.Epilogue;
import epilogue.powershell.PowerShell;
import epilogue.enums.ChatColors;
import epilogue.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class Friend extends PowerShell {
    public Friend() {
        super(new ArrayList<>(Arrays.asList("Friend")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() >= 2) {
            String subCommand = args.get(1).toLowerCase(Locale.ROOT);
            switch (subCommand) {
                case "add":
                    if (args.size() < 3) {
                        ChatUtil.sendFormatted(
                                String.format("%sUsage: .%s add <&oname&r>&r", Epilogue.clientName, args.get(0).toLowerCase(Locale.ROOT))
                        );
                        return;
                    }
                    String added = Epilogue.friendManager.add(args.get(2));
                    if (added == null) {
                        ChatUtil.sendFormatted(String.format("%s&o%s&r is already in your friend list&r", Epilogue.clientName, args.get(2)));
                        return;
                    }
                    ChatUtil.sendFormatted(String.format("%sAdded &o%s&r to your friend list&r", Epilogue.clientName, added));
                    return;
                case "remove":
                    if (args.size() < 3) {
                        ChatUtil.sendFormatted(
                                String.format("%sUsage: .%s remove <&oname&r>&r", Epilogue.clientName, args.get(0).toLowerCase(Locale.ROOT))
                        );
                        return;
                    }
                    String removed = Epilogue.friendManager.remove(args.get(2));
                    if (removed == null) {
                        ChatUtil.sendFormatted(String.format("%s&o%s&r is not in your friend list&r", Epilogue.clientName, args.get(2)));
                        return;
                    }
                    ChatUtil.sendFormatted(String.format("%sRemoved &o%s&r from your friend list&r", Epilogue.clientName, removed));
                    return;
                case "list":
                    ArrayList<String> list = Epilogue.friendManager.getPlayers();
                    if (list.isEmpty()) {
                        ChatUtil.sendFormatted(String.format("%sNo friends&r", Epilogue.clientName));
                        return;
                    }
                    ChatUtil.sendFormatted(String.format("%sFriends:&r", Epilogue.clientName));
                    for (String friend : list) {
                        ChatUtil.sendRaw(String.format(ChatColors.formatColor("   &o%s&r"), friend));
                    }
                    return;
                case "clear":
                    Epilogue.friendManager.clear();
                    ChatUtil.sendFormatted(String.format("%sCleared your friend list&r", Epilogue.clientName));
                    return;
            }
        }
        ChatUtil.sendFormatted(
                String.format("%sUsage: .%s <&oadd&r/&oremove&r/&olist&r/&oclear&r>&r", Epilogue.clientName, args.get(0).toLowerCase(Locale.ROOT))
        );
    }
}
