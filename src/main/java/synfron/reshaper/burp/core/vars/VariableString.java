package synfron.reshaper.burp.core.vars;

import burp.BurpExtender;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import synfron.reshaper.burp.core.messages.IEventInfo;
import synfron.reshaper.burp.core.messages.MessageValue;
import synfron.reshaper.burp.core.messages.MessageValueHandler;
import synfron.reshaper.burp.core.utils.CollectionUtils;
import synfron.reshaper.burp.core.utils.GetItemPlacement;
import synfron.reshaper.burp.core.utils.Log;
import synfron.reshaper.burp.core.utils.TextUtils;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VariableString implements Serializable {
    private final String text;
    private final List<VariableSourceEntry> variables;

    private VariableString() {
        text = "";
        variables = Collections.emptyList();
    }

    public VariableString(String text, List<VariableSourceEntry> variables) {
        this.text = text;
        this.variables = variables;
    }

    public static boolean isValidVariableName(String name) {
        return StringUtils.isNotEmpty(name) && !Pattern.matches("\\{\\{|}}", name);
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(text);
    }

    public String toString()
    {
        return String.format(text, variables.stream().map(VariableSourceEntry::getTag).toArray());
    }

    public static String toString(VariableString variableString, String defaultValue) {
        return variableString != null ? variableString.toString() : defaultValue;
    }
    public static VariableString getAsVariableString(String str) {
        return getAsVariableString(str, true);
    }

    public static VariableString getAsVariableString(String str, boolean requiresParsing)
    {
        if (str == null) {
            return null;
        }
        str = str.replace("%", "%%");
        if (requiresParsing)
        {
            List<VariableSourceEntry> variableSourceEntries = new ArrayList<>();
            Pattern pattern = Pattern.compile(String.format("\\{\\{(%s):(.+?)\\}\\}", String.join("|", VariableSource.getSupportedNames())));
            str = pattern.matcher(str).replaceAll(match -> {
                VariableSource variableSource = VariableSource.get(match.group(1));
                String entryName = variableSource == VariableSource.Special ?
                        getSpecialChar(match.group(2)) :
                        match.group(2);
                variableSourceEntries.add(
                        new VariableSourceEntry(VariableSource.get(match.group(1)), entryName, match.group(0))
                );
                return "%s";
            });
            return new VariableString(str, variableSourceEntries);
        }
        else
        {
            return new VariableString(str, Collections.emptyList());
        }
    }

    public Integer getInt(IEventInfo eventInfo)
    {
        String text = getText(eventInfo);
        Integer nullableValue = null;
        try {
            nullableValue = Integer.parseInt(text);
        } catch (NumberFormatException ignored) {

        }
        return nullableValue;
    }

    public String getText(IEventInfo eventInfo)
    {
        List<String> variableVals = new ArrayList<>();
        for (VariableSourceEntry variable : variables)
        {
            VariableSource variableSource = variable.getVariableSource();
            if (variableSource != null) {
                if (variableSource.isAccessor()) {
                    String value = switch (variable.getVariableSource()) {
                        case Message -> getMessageVariable(eventInfo, variable.getName());
                        case File -> getFileText(eventInfo, variable.getName());
                        case Special -> variable.getName();
                        default -> null;
                    };
                    variableVals.add(value);
                } else {
                    Variable value = switch (variable.getVariableSource()) {
                        case Global -> GlobalVariables.get().getOrDefault(variable.getName());
                        case Event -> eventInfo.getVariables().getOrDefault(variable.getName());
                        default -> null;
                    };
                    variableVals.add(value != null ? TextUtils.toString(value.getValue()) : null);
                }
            }
        }
        return String.format(text, variableVals.toArray());
    }

    private String getFileText(IEventInfo eventInfo, String locator) {
        try {
            String[] variableNameParts = locator.split(":", 2);
            return FileUtils.readFileToString(new File(variableNameParts[1]), variableNameParts[0]);
        } catch (Exception e) {
            if (eventInfo.getDiagnostics().isEnabled()) {
                Log.get().withMessage(String.format("Error reading file with variable tag: %s", VariableSourceEntry.getTag(VariableSource.Special, locator))).withException(e).logErr();
            }
        }
        return null;
    }

    private static String getSpecialChar(String sequences) {
            try {
                return TextUtils.parseSpecialChars(sequences);
            } catch (Exception e) {
                if (BurpExtender.getGeneralSettings().isEnableEventDiagnostics()) {
                    Log.get().withMessage(String.format("Invalid use of special character variable tag: %s", VariableSourceEntry.getTag(VariableSource.Special, sequences))).withException(e).logErr();
                }
            }
            return null;
    }

    private String getMessageVariable(IEventInfo eventInfo, String locator) {
        String[] variableNameParts = locator.split(":", 2);
        MessageValue messageValue = EnumUtils.getEnumIgnoreCase(MessageValue.class, CollectionUtils.elementAtOrDefault(variableNameParts, 0, ""));
        String identifier = CollectionUtils.elementAtOrDefault(variableNameParts, 1, "");
        if (messageValue != null) {
            String value = StringUtils.defaultString(
                    MessageValueHandler.getValue(eventInfo, messageValue, VariableString.getAsVariableString(identifier, false), GetItemPlacement.Last)
            );
            return value;
        }
        return null;
    }

    public static String getTextOrDefault(IEventInfo eventInfo, VariableString variableString, String defaultValue) {
        return variableString != null && !variableString.isEmpty() ?
                StringUtils.defaultIfEmpty(variableString.getText(eventInfo), defaultValue) :
                defaultValue;
    }

    public static String getText(IEventInfo eventInfo, VariableString variableString) {
        return variableString != null ? variableString.getText(eventInfo) : null;
    }

    public static int getIntOrDefault(IEventInfo eventInfo, VariableString variableString, int defaultValue) {
        return variableString != null && !variableString.isEmpty() ?
                variableString.getInt(eventInfo) :
                defaultValue;
    }

    public static boolean isPotentialInt(String formattedString) {
        if (StringUtils.isEmpty(formattedString)) {
            return false;
        }
        String strippedText = formattedString.replaceAll(String.format("\\{\\{(%s):(.+?)\\}\\}", Arrays.stream(VariableSource.values())
                .map(value -> value.toString().toLowerCase())
                .collect(Collectors.joining("|"))
        ), "");
        return TextUtils.isInt(strippedText);
    }
}
