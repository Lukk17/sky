package com.lukk.sky.common.config;

import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MissingCredentialException extends RuntimeException {

    private final transient List<MissingCredential> missing;

    public MissingCredentialException(List<MissingCredential> missing) {
        super(describe(missing) + " " + act(missing));
        this.missing = List.copyOf(missing);
    }

    public String getDescription() {
        return describe(missing);
    }

    public String getAction() {
        return act(missing);
    }

    private static String describe(List<MissingCredential> missing) {
        String heading = missing.size() == 1
                ? "1 required credential is not configured:"
                : missing.size() + " required credentials are not configured:";

        String detail = missing.stream()
                .map(credential -> System.lineSeparator() + "    " + credential.describe() + ".")
                .collect(Collectors.joining());

        return heading + detail + System.lineSeparator()
                + "Spring Boot leaves an unresolvable ${...} placeholder in the value instead of failing, "
                + "so without this check the service would start and the placeholder text would be used "
                + "as if it were the credential, failing later as an authentication error that names "
                + "neither the property nor the variable.";
    }

    private static String act(List<MissingCredential> missing) {
        List<String> variables = missing.stream()
                .map(MissingCredential::environmentVariable)
                .distinct()
                .toList();

        return "Set " + joinWithAnd(variables) + " in this service's environment, then start it again.";
    }

    private static String joinWithAnd(List<String> variables) {
        if (variables.size() == 1) {
            return variables.getFirst();
        }

        return String.join(", ", variables.subList(0, variables.size() - 1)) + " and " + variables.getLast();
    }
}
