package epilogue.powershell.shells;

import epilogue.Epilogue;
import epilogue.powershell.PowerShell;
import epilogue.module.Module;
import epilogue.util.ChatUtil;
import epilogue.util.KeyBindUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Bind extends PowerShell {
    public Bind() {
        super(new ArrayList<>(Arrays.asList("Bind")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 3) {
            if (args.size() == 2 && (args.get(1).equalsIgnoreCase("l") || args.get(1).equalsIgnoreCase("list"))) {
                List<Module> modules = Epilogue.moduleManager.modules.values().stream().filter(module -> module.getKey() != 0).collect(Collectors.toList());
                if (modules.isEmpty()) {
                    ChatUtil.sendFormatted(String.format("%sNo binds&r", Epilogue.clientName));
                } else {
                    ChatUtil.sendFormatted(String.format("%sBinds:&r", Epilogue.clientName));
                    for (Module module : modules) {
                        ChatUtil.sendFormatted(String.format("%s»&r %s&r", module.isHidden() ? "&8" : "&7", module.formatModule()));
                    }
                }
            } else {
                ChatUtil.sendFormatted(
                        String.format(
                                "%sUsage: .%s <&omodule&r> <&okey&r>&r | .%s <&omodule&r> &onone&r | .%s &olist&r",
                                Epilogue.clientName,
                                args.get(0).toLowerCase(Locale.ROOT),
                                args.get(0).toLowerCase(Locale.ROOT),
                                args.get(0).toLowerCase(Locale.ROOT)
                        )
                );
            }
        } else {
            String keyInput = args.get(2).toUpperCase();
            int keyIndex = 0;

            if (keyInput.equalsIgnoreCase("NONE") || keyInput.equalsIgnoreCase("NULL") || keyInput.equalsIgnoreCase("0")) {
                keyIndex = 0;
            } else {
                keyIndex = Keyboard.getKeyIndex(keyInput);

                if (keyIndex == 0) {
                    int buttonIndex = Mouse.getButtonIndex(keyInput);
                    if (buttonIndex != -1) {
                        keyIndex = buttonIndex - 100;
                    }
                }
            }
                if (!args.get(1).equals("*")) {
                    Module module = Epilogue.moduleManager.getModule(args.get(1));
                    if (module == null) {
                        ChatUtil.sendFormatted(String.format("%sModule not found (&o%s&r)&r", Epilogue.clientName, args.get(1)));
                    } else {
                        module.setKey(keyIndex);
                        if (keyIndex == 0) {
                            ChatUtil.sendFormatted(
                                    String.format("%sUnbind &o%s&r", Epilogue.clientName, module.getName())
                            );
                        } else {
                            ChatUtil.sendFormatted(
                                    String.format("%sBound &o%s&r to &l[%s]&r", Epilogue.clientName, module.getName(), KeyBindUtil.getKeyName(keyIndex))
                            );
                        }
                    }
                } else {
                    for (Module module : Epilogue.moduleManager.modules.values()) {
                        module.setKey(keyIndex);
                    }
                    if (keyIndex == 0) {
                        ChatUtil.sendFormatted(
                                String.format("%sUnbind all modules&r", Epilogue.clientName)
                        );
                    } else {
                        ChatUtil.sendFormatted(
                                String.format("%sBind all modules to &l[%s]&r", Epilogue.clientName, KeyBindUtil.getKeyName(keyIndex))
                    );
                }
            }
        }
    }
}