package io.github.nakib16k.tunnelvision;

import java.util.Objects;
import java.util.Scanner;

/** Shared case-sensitive yes/no validation, with an injectable input source. */
public final class ConsoleInputHandler {
    private final Scanner scanner;
    private final ConsoleMessages messages;

    public ConsoleInputHandler() {
        this(new Scanner(System.in), new ConsoleMessages());
    }

    public ConsoleInputHandler(Scanner scanner, ConsoleMessages messages) {
        this.scanner = Objects.requireNonNull(scanner);
        this.messages = Objects.requireNonNull(messages);
    }

    public void awaitCalibrationConfirmation() {
        awaitResponse(false, messages::showCalibrationPrompt);
    }

    public void awaitStartConfirmation() {
        awaitResponse(false, messages::showStartPrompt);
    }

    public boolean askWhetherToWriteLog() {
        return "y".equals(awaitResponse(true, messages::showLogPrompt));
    }

    private String awaitResponse(boolean allowNo, Runnable repeatPrompt) {
        while (scanner.hasNextLine()) {
            String answer = scanner.nextLine();
            if ("y".equals(answer) || (allowNo && "n".equals(answer))) {
                return answer;
            }
            messages.showInvalidInputMessage();
            repeatPrompt.run();
        }
        // Avoid the historical infinite loop when keyboard input ends.
        throw new IllegalStateException("Console input ended before a valid answer");
    }
}
