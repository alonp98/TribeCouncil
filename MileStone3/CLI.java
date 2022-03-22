package test;

import java.util.ArrayList;

import test.Commands.Command;
import test.Commands.DefaultIO;

public class CLI {

	ArrayList<Command> commands;
	DefaultIO dio;
	Commands c;
	
	public CLI(DefaultIO dio) {
		this.dio=dio;
		c=new Commands(dio); 
		commands=new ArrayList<>();

		// implement
		commands.add(c.new UploadCommand());
		commands.add(c.new CorrelationSetting());
		commands.add(c.new AnomalyDetection());
		commands.add(c.new DisplayResults());
		commands.add(c.new UploadAnomaliesAndAnalyzeResults());
		commands.add(c.new Exit());
	}
	
	public void start() {
		// implement
		printCommands(commands);

		while (true) {
			int userChoice = (int) dio.readVal();
			if (userChoice > 0 && userChoice < 6) {
				commands.get(userChoice - 1).execute();
				printCommands(commands);
			}
			else if (userChoice == 6) { /* Exit */
				commands.get(userChoice - 1).execute();
				break;
			}
			else {
				dio.write("Please enter a valid number 1 - 6\n");
			}
		}

	}

	private void printCommands(ArrayList<Command> commands) {
		this.dio.write("Welcome to the Anomaly Detection Server.\n");
		this.dio.write("Please choose an option:\n");

		int i = 1;
		for (Command command : commands) {
			this.dio.write(i + ". " + command.description);
			i++;
		}
	}
}
