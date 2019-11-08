package com.github.juupje.plotter;

import com.github.juupje.calculator.helpers.ErrorHandler;
import com.github.juupje.calculator.helpers.IOHandler;
import com.github.juupje.calculator.helpers.Shape;
import com.github.juupje.calculator.main.Calculator;
import com.github.juupje.calculator.main.Command;
import com.github.juupje.calculator.main.Parser;
import com.github.juupje.calculator.main.Variable;
import com.github.juupje.calculator.main.Variables;
import com.github.juupje.calculator.main.plugins.Plugin;
import com.github.juupje.calculator.mathobjects.MFunction;
import com.github.juupje.calculator.mathobjects.MReal;
import com.github.juupje.calculator.mathobjects.MathObject;

import javafx.application.Platform;

public class Plotter implements Plugin {

	private static PlotScreen plotScreen;
	
	@Override
	public void run() {
		Command.insertCommand("plot", new PlotCommand(false, false));
		Command.insertCommand("addplot", new PlotCommand(true, false));
		Command.insertCommand("delplot", new Command() {
			@Override
			public void process(String s) {
				if(!plotScreen.controller.getSelected().remove(s))
					throw new IllegalArgumentException("No known plot in selected pane with name '" + s + "'");
			}
		});
		Command.insertCommand("dplot", new PlotCommand(false, true));
		Command.insertCommand("adddplot", new PlotCommand(true, true));
		Command.insertCommand("selectplot", new Command() {
			@Override
			public void process(String s) {
				int index = 0;
				try {
					index = Integer.valueOf(s);
				} catch(NumberFormatException e) {
					MathObject obj = new Parser(s).evaluate();
					if(obj instanceof MReal && ((MReal) obj).isInteger()) {
						index = (int)((MReal) obj).getValue();
					} else {
						throw new IllegalArgumentException("Expected integer argument, got " + obj);
					}
				}
				index = index<0 ? plotScreen.controller.getTabCount()+index : (int) index;
				if(index > plotScreen.controller.getTabCount())
					throw new IllegalArgumentException("Can't select plot " + index + ": there are only " + plotScreen.controller.getTabCount() + " plots.");
				plotScreen.controller.select(index);
			}
		});
	}
	
	@Override
	public void exit() {
		Platform.exit();
	}

	class PlotCommand extends Command {
		
		boolean append = false;
		boolean dynamic = false;
		public PlotCommand(boolean append, boolean dynamic) {
			this.append = append;
			this.dynamic = dynamic;
		}

		@Override
		public void process(String s) {
			String[] stringArgs = Parser.getArguments(s);
			MathObject[] args = new MathObject[stringArgs.length];
			Variable var = null;
			if (Variables.exists(stringArgs[0])) {
				var = new Variable(stringArgs[0]);
				args[0] = var.evaluate();
			} else {
				throw new IllegalArgumentException("No known variable '" + stringArgs[0] + "' found.");
			}
			if (!(args[0] instanceof MFunction)) {
				throw new IllegalArgumentException(
						"Expected first argument to be a function, got " + args[0].getClass().getSimpleName());
			} else if (!(((MFunction) args[0]).getParameters().length == 1 && args[0].shape().equals(Shape.SCALAR))) {
				throw new IllegalArgumentException("Can't plot multivariate functions. Parameter count: "
						+ ((MFunction) args[0]).getParameters().length + ", shape: " + args[0].shape());
			}
			if (!append && stringArgs.length == 3) {
				args[1] = new Parser(stringArgs[1]).evaluate();
				args[2] = new Parser(stringArgs[2]).evaluate();
				if (!(args[1] instanceof MReal && args[2] instanceof MReal)) {
					throw new IllegalArgumentException("Expected arguments 2 and 3 to be real numbers, got "
							+ args[1].toString() + " and " + args[2].toString());
				}
				plot(var, ((MReal) args[1]).getValue(), ((MReal) args[2]).getValue(), append, dynamic);
			} else if (args.length == 1) {
				plot(var, -8, 8, append, dynamic);
			} else {
				if(append)
					throw new IllegalArgumentException("addplot() expected 1 argument, got " + stringArgs.length);
				else
					throw new IllegalArgumentException("plot() expected 1 or 3 argument(s), got " + stringArgs.length);				
			}
		}
	}
	
	public static void setPlotScreen(PlotScreen screen) {
		plotScreen = screen;
	}

	private void startPlotScreen() {
		PlotScreen.create();
	}

	private void plot(Variable var, double begin, double end, boolean append, boolean dynamic) {
		if(plotScreen==null)
			startPlotScreen();
		if(append) //adds plot to already existing pane
			plotScreen.plot(var, dynamic);
		else {//adds new pane
			plotScreen.plot(var, begin, end, dynamic);
		}
	}

	public static void main(String[] args) {
		Calculator.setIOHandler(new IOHandler());
		Calculator.setErrorHandler(new ErrorHandler());
		new Plotter().run();
		Calculator.start(args);
		new Calculator();
	}

	@Override
	public String getName() {
		return "Plotter";
	}

}
