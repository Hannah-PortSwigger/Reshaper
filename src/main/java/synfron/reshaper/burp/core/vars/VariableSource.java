package synfron.reshaper.burp.core.vars;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum VariableSource {
    Event("e"),
    Global("g"),
    Message("m"),
    File("f"),
    Special("s");

    @Getter
    private final String shortName;

    VariableSource(String shortName) {
        this.shortName = shortName;
    }

    public static List<String> getSupportedNames() {
        return Stream.concat(
                Arrays.stream(VariableSource.values()).map(source -> source.name().toLowerCase()),
                Arrays.stream(VariableSource.values()).map(VariableSource::getShortName)
        ).collect(Collectors.toList());
    }

    public static VariableSource get(String name) {
        return Arrays.stream(VariableSource.values())
                .filter(source -> source.name().equalsIgnoreCase(name) || source.shortName.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
