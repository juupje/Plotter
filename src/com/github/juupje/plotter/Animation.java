package com.github.juupje.plotter;

import java.util.function.Function;

import com.github.juupje.calculator.helpers.Shape;
import com.github.juupje.calculator.helpers.exceptions.TreeException;
import com.github.juupje.calculator.main.Calculator;
import com.github.juupje.calculator.mathobjects.MFunction;
import com.github.juupje.calculator.mathobjects.MReal;

import javafx.animation.AnimationTimer;
import javafx.scene.paint.Color;

public class Animation extends Plot {

	MReal param = new MReal(0);
	AnimationTimer timer;
	public Animation(MFunction func, String var, double min, double max, double time, double xMin, double xMax, double xUnit, double yUnit,
			CartesianAxes axes, Color color) {		
		super(null, xMin, xMax, xUnit, yUnit, axes, color);
		param.setValue(min);
		this.func = toFunction(func, var);
		timer = new AnimationTimer() {
			double delta = (max-min)/time, val = min;
			long oldTime= 0;
			@Override
			public void handle(long time) {
				if(oldTime==0) {
					oldTime  = time;
					return;
				}
				double dt = (time-oldTime)*1e-9;
				val += delta*dt;
				if(val>= max) {
					delta*=-1;
					val = max;
				} else if(val<= min) {
					delta *= -1;
					val = min;
				}
				param.setValue(val);
				redrawPath();
				oldTime = time;
			}
		};
	}
	
	public void start() {
		timer.start();
	}
	
	public void stop() {
		timer.stop();
	}

	private Function<Double, Double> toFunction(MFunction func, String var) {
			if (func.getParameters().length == 1 && func.shape().equals(Shape.SCALAR)) {
				final MFunction f = new MFunction(new String[] {func.getParameters()[0], var}, func.getTree(), true);
				MReal xVal = new MReal(0);
				return (x -> {
					try {
						xVal.setValue(x);
						return ((MReal) (f.evaluateAt(xVal, param))).getValue();
					} catch (TreeException e) {
						Calculator.errorHandler.handle("Something went wrong while evaluating this function.", e);
						return null;
					}
				});
			} else
				throw new IllegalArgumentException("Can't plot multivariate or multi-dimensional functions.");
	}
}
