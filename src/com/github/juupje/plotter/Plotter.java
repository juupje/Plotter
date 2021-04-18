package com.github.juupje.plotter;

import java.io.IOException;

import org.json.JSONObject;

import com.github.juupje.calculator.commands.Command;
import com.github.juupje.calculator.commands.Commands;
import com.github.juupje.calculator.helpers.ErrorHandler;
import com.github.juupje.calculator.helpers.Tools;
import com.github.juupje.calculator.helpers.io.IOHandler;
import com.github.juupje.calculator.helpers.io.JSONReader;
import com.github.juupje.calculator.main.Calculator;
import com.github.juupje.calculator.main.Parser;
import com.github.juupje.calculator.main.Variable;
import com.github.juupje.calculator.main.Variables;
import com.github.juupje.calculator.main.VectorParser;
import com.github.juupje.calculator.main.plugins.Plugin;
import com.github.juupje.calculator.mathobjects.MFunction;
import com.github.juupje.calculator.mathobjects.MReal;
import com.github.juupje.calculator.mathobjects.MVector;
import com.github.juupje.calculator.mathobjects.MathObject;
import com.github.juupje.calculator.mathobjects.Shape;
import com.github.juupje.calculator.settings.Setting;
import com.github.juupje.calculator.settings.Settings;
import com.github.juupje.calculator.settings.SettingsHandler;

import javafx.application.Platform;

public class Plotter implements Plugin {

	private static PlotScreen plotScreen;
	public static Setting SETTING_ANIM_TIME;
	private static final int VERSION_ID = 5;
	
	@Override
	public void run() {
		Commands.insertCommand("plt_plot", new PlotCommand(false, false));
		Commands.insertCommand("plt_add", new PlotCommand(true, false));
		Commands.insertCommand("plt_dplot", new PlotCommand(false, true));
		Commands.insertCommand("plt_addd", new PlotCommand(true, true));
		
		Commands.insertCommand("plt_del", arg -> {
				if(!plotScreen.controller.getSelected().remove(arg))
					throw new IllegalArgumentException("No known plot in selected pane with name '" + arg + "'");
			});
		Commands.insertCommand("plt_select", arg -> {
				int index = 0;
				try {
					index = Integer.valueOf(arg);
				} catch(NumberFormatException e) {
					MathObject obj = new Parser(arg).evaluate();
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
			});
		Commands.insertCommand("plt_anim", arg -> {
			String[] args = Parser.getArguments(arg);
			//check argument length
			if(!(args.length == 2 || args.length==3))
				throw new IllegalArgumentException("Expected 2 or 3 arguments, got " + args.length);
			//check if plot already exists (it should)
			if(plotScreen.controller.getSelected().getPlot(args[0])==null)
				throw new IllegalArgumentException("No plot with name '" + args[0] + "' was found.");
	
			MFunction func = (MFunction) Variables.get(args[0]);
			
			//Find the variable to be animated, and check whether the function depends on it, and whether it is a real scalar.
			int index = args[1].indexOf("=");
			if(index==-1)
				throw new IllegalArgumentException("Expected '=' in second argument. See help(anim).");
			String varname  = args[1].substring(0, index);
			Variable var = new Variable(varname);
			if(!func.getDependencies().contains(var))
				throw new IllegalArgumentException("Function " + args[0] + " does not depend on " + varname + ".");
			if(!(var.get() instanceof MReal))
				throw new IllegalArgumentException("Variable " + var.getName() + " is not a real scalar.");
			
			//find the range of the variable to be animated
			MathObject mo = new VectorParser(args[1].substring(index+1)).parse(false).evaluate();
			if(!(mo instanceof MVector) || ((MVector) mo).size()!=2)
				throw new IllegalArgumentException("Expected vector of length 2 after '=' in second argument, got " + Tools.type(mo));
			MVector v = (MVector) mo;
			if(!(v.get(0) instanceof MReal) || !(v.get(1) instanceof MReal))
				throw new IllegalArgumentException("Animation bounds should be real numbers, got " + v.get(0) + " and " + v.get(1));
			double begin = ((MReal) ((MVector) mo).get(0)).getValue();
			double end = ((MReal) ((MVector) mo).get(1)).getValue();
			
			//find the duration of one animation cycle
			double time = Settings.getDouble(SETTING_ANIM_TIME);
			if(args.length==3) {
				try {
					time = Double.valueOf(args[2]);
				} catch(NumberFormatException e) {
					Calculator.ioHandler.err("Expected numeric value in 3rd argument. Got " + args[2] + ", using default time.");
				}
			}
			plotScreen.anim(new Variable(args[0]), varname, begin, end, time);
		});
		Commands.insertCommand("plt_stop", arg -> {
			try {
				Plot plt = plotScreen.controller.getSelected().getPlot(arg);
				((Animation) plt).stop();
			} catch(NullPointerException | ClassCastException e) {
				throw new IllegalArgumentException("Animated plot '" + arg + "' was not found.");
			}
		});
		
		SETTING_ANIM_TIME = Settings.insertSetting("plt.anim_time", 2d);
	}
	
	@Override
	public int version() {
		return VERSION_ID;
	}
	
	@Override
	public JSONObject initHelp() {
		try {
			return JSONReader.parse(Plotter.class.getResourceAsStream("/com/github/juupje/plotter/files/help.json"));
		} catch (IOException e) {
			Calculator.errorHandler.handle("Could not load help file of Plotter.", e);
		}
		return null;
	}
	
	@Override
	public void exit() {
		Platform.exit();
	}

	class PlotCommand implements Command {
		
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
					throw new IllegalArgumentException("plt_add()/plt_addd() expected 1 argument, got " + stringArgs.length);
				else
					throw new IllegalArgumentException("plt_plot()/plt_plotd() expected 1 or 3 argument(s), got " + stringArgs.length);				
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
		Calculator.setSettingsHandler(new SettingsHandler());
		Calculator.parseArgs(args);
		new Plotter().run();
		Calculator.start();
		new Calculator();
	}

	@Override
	public String getName() {
		return "Plotter";
	}

}
