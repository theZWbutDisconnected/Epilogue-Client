package epilogue.powershell.shells;

import epilogue.Epilogue;
import epilogue.powershell.PowerShell;
import epilogue.util.ChatUtil;
import epilogue.value.Value;
import epilogue.value.values.BooleanValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Module extends PowerShell {
    public Module() {
        super(new ArrayList<>(Epilogue.moduleManager.modules.values().stream().<String>map(epilogue.module.Module::getName).collect(Collectors.<String>toList())));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        epilogue.module.Module module = Epilogue.moduleManager.getModule(args.get(0));
        if (args.size() >= 2) {
            Value<?> value = Epilogue.valueHandler.getProperty(module, args.get(1));
            if (value == null) {
                ChatUtil.sendFormatted(String.format("%s%s has no value &o%s&r", Epilogue.clientName, module.getName(), args.get(1)));
            } else if (args.size() < 3 && !(value instanceof BooleanValue)) {
                ChatUtil.sendFormatted(
                        String.format(
                                "%s%s: &o%s&r is set to %s&r (%s)&r",
                                Epilogue.clientName,
                                module.getName(),
                                value.getName(),
                                value.formatValue(),
                                value.getValuePrompt()
                        )
                );
            } else {
                String newValue = args.size() < 3 ? null : String.join(" ", args.subList(2, args.size()));
                try {
                    if (value.parseString(newValue)) {
                        ChatUtil.sendFormatted(
                                String.format("%s%s: &o%s&r has been set to %s&r", Epilogue.clientName, module.getName(), value.getName(), value.formatValue())
                        );
                        return;
                    }
                } catch (Exception e) {
                }
                ChatUtil.sendFormatted(
                        String.format("%sInvalid value for value &o%s&r (%s)&r", Epilogue.clientName, value.getName(), value.getValuePrompt())
                );
            }
        } else {
            List<Value<?>> properties = Epilogue.valueHandler.properties.get(module.getClass());
            if (properties != null) {
                List<Value<?>> visible = properties.stream().filter(Value::isVisible).collect(Collectors.toList());
                if (!visible.isEmpty()) {
                    ChatUtil.sendFormatted(String.format("%s%s:&r", Epilogue.clientName, module.formatModule()));
                    for (Value<?> value : visible) {
                        ChatUtil.sendFormatted(String.format("&7»&r %s: %s&r", value.getName(), value.formatValue()));
                    }
                    return;
                }
            }
            ChatUtil.sendFormatted(String.format("%s%s has no values&r", Epilogue.clientName, module.formatModule()));
        }
    }
}
