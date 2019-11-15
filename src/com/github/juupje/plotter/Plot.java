package com.github.juupje.plotter;

import java.util.function.Function;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

public class Plot extends Pane {

	Path path;
	double xMin, xMax, xInc;
	Function<Double, Double> func;
	double plotYRangeLength;
	double xUnit = 1, yUnit = 1;
	CartesianAxes axes;
	
	public Plot(Function<Double, Double> f, double xMin, double xMax, double xUnit, double yUnit, CartesianAxes axes, Color color) {
		this.xMin = xMin;
		this.xMax = xMax;
		this.func = f;
		this.xUnit = xUnit;
		this.yUnit = yUnit;
		this.axes = axes;
		path = new Path();
		path.setStroke(color);
		path.setStrokeWidth(2);
		setStyle("-fx-background-color: blue;");
		scaleXProperty().addListener((obj, oldVal, newVal) -> {
			path.setStrokeWidth(2/newVal.doubleValue());
		});
		scaleYProperty().addListener((obj, oldVal, newVal) -> path.setStrokeWidth(-2/newVal.doubleValue()));
		
		xInc = (xMax-xMin)/(axes.getWidth()/4);
		plotYRangeLength = axes.maxY()-axes.minY();
		drawPath(f, xMin, xMax, xInc);
		
		axes.getXAxis().upperBoundProperty().addListener((obj, oldVal, newVal) -> {
			if(newVal.doubleValue() > xMax) {
				drawPath(f, this.xMax, Math.max(newVal.doubleValue(), this.xMax+xInc), xInc);
				this.xMax = newVal.doubleValue();
			}
		});
		
		axes.getXAxis().lowerBoundProperty().addListener((obj, oldVal, newVal) -> {
			if(newVal.doubleValue() < xMin) {
				drawPath(f, Math.min(newVal.doubleValue(), this.xMin-xInc), this.xMin, xInc);
				this.xMin = newVal.doubleValue();
			}
		});
		
		setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
		setPrefSize(axes.getPrefWidth()/axes.xUnitDist, axes.getPrefHeight()/axes.yUnitDist);
		setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
		getChildren().setAll(path);
	}
	
	protected void redrawPath() {
		path.getElements().clear();
		drawPath(func, xMin, xMax, xInc);
	}
	
	private void drawPath(Function<Double, Double> f, double xMin, double xMax, double xInc) {
		if(f == null) return;
		double x = xMin;
		double y = Double.NaN, lastY = Double.NaN;
		while(x <= xMax) {
			try {
				if(Double.isNaN(y)) {
					y = f.apply(x);
					if(!Double.isNaN(y))
						path.getElements().add(new MoveTo(x*xUnit, -y*yUnit));
				} else {
					y = f.apply(x);
					if(Math.signum(lastY) != Math.signum(y) && Math.abs(lastY-y)>10*plotYRangeLength)
						path.getElements().add(new MoveTo(x*xUnit,-y*yUnit));
					else
						path.getElements().add(new LineTo(x*xUnit, -y*yUnit));
				}
				x += xInc;
				lastY = y;
			} catch(Exception e) {}
		}
	}
	
	public void setScales(double unitX, double unitY) {
		xInc = (xMax-xMin)/(axes.getWidth()/4);
		xUnit = unitX;
		yUnit = unitY;
		plotYRangeLength = axes.maxY()-axes.minY();
		redrawPath();
	}
	
	public Function<Double, Double> getFunction() {
		return func;
	}
	
	public void setColor(Color col) {
		path.setStroke(col);
	}
	
	public void setContentClip(double x, double y, double width, double height) {
		//path.setClip(new Rectangle(x, y ,width, height));
	}
	
	public void setPlotYRangeLength(double d) {
		plotYRangeLength = d;
	}

	public void updateFunction(Function<Double, Double> function) {
		func = function;
		redrawPath();
	}
}