package io.github.nakib16k.tunnelvision;


import java.io.PrintStream;
import java.util.Objects;

public class ConsoleMessages
{
    private final PrintStream out;

    public ConsoleMessages() {
        this(System.out);
    }

    public ConsoleMessages(PrintStream out) {
        this.out = Objects.requireNonNull(out);
    }
	public void showWelcomeMessage()
	{
		out.println("\nTunnel Vision - SwiftBot Tunnel & Gap Measurement\n");
		out.println("\nBefore Starting, the Speed needs to be calibrated.");
		out.println("\nPlease place the Swiftbot greater than 15cm from an object.");
		out.println("\nType ‘y’ and press ‘Enter’ from the keyboard to start the speed calibration");
	}

	public void showTunnelSetupInstructions()
	{
		out.println("\n\nPlace at least three consecutive tunnels with sufficient gaps (at least 20 cm) between them on the ground.");
		out.println("The minimum length of each tunnel needs to be atleast 30 cm.");
		out.println("The length of consecutive tunnels should vary between 10-15 cm from one another.");
		out.println("Please place the Swiftbot in front of the opening of the first tunnel before executing the task.");
		out.println("The tunnels should be wide enough for the Swiftbot to pass through and all the tunnels should have the same height and width (eg. Height = 15 cm and Width = 15 cm).");
		out.println("\nType ‘y’ and press ‘Enter’ from the keyboard to start the task");
	}


	public void showRunStartedMessage()
	{
		out.println("\nThe Swiftbot is now performing the Task");
	}

	public void showInvalidInputMessage()
	{
		out.println("\nIncorrect Input");
	}

	public void showCalibrationPrompt()
	{
		out.println("\nType ‘y’ and press ‘Enter’ from the keyboard to start the speed calibration");
	}

	public void showStartPrompt()
	{
		out.println("Type ‘y’ and press ‘Enter’ from the keyboard to start the task");
	}

	public void showLogPrompt()
	{
		out.println("\nDo you want a log? (Type and Enter ‘y’ or ‘n’)");
	}

	public void showStopInstructions()
	{
		out.println("\nTo stop the program, Press and Hold Button 'X' in Swiftbot.");
	}

	public void showTunnelVariationError()
	{
		out.println("\nThe difference in length between two consecutive tunnels is out of the specified range of 10-15 cm.");
	}

	public void showMinimumTunnelLengthError()
	{
		out.println("\nThe length of the tunnel needs to be atleast 30 cm.");
	}

	public void showInsideTunnelStatus()
	{
		out.println("\nThe Swiftbot is in the Tunnel");
	}

	public void showOutsideTunnelStatus()
	{
		out.println("\nThe Swiftbot is Outside");
	}


}
