package com.github.juupje.plotter;

import java.util.function.Function;

import com.github.juupje.calculator.helpers.Tools;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class CartesianAxes extends Pane {
	private Axis xAxis;
	private Axis yAxis;
	
	public double xUnitDist, yUnitDist;
	private double originX, originY;
	private int width, height;
	private final int overflow = 0;
	
	public CartesianAxes(int w, int h, double xMin, double xMax, double xTick, double yMin, double yMax, double yTick) {
		setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
		setPrefSize(width, height);
		setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
		this.width = w;
		this.height = h;
		originX = width/2;
		originY = height/2;
		xUnitDist = width/(xMax-xMin);
		yUnitDist = height/(yMax-yMin);
		double length = xMax-xMin;
		xAxis = new Axis(xMin-length*.05, xMax+length*.05, xTick);
		xAxis.setTickLabelFill(Color.DARKGRAY);
		xAxis.setSide(Side.BOTTOM);
		xAxis.setMinorTickVisible(false);
		xAxis.setLayoutX(-overflow/2);
		xAxis.setPrefWidth(width+overflow);
		xAxis.setLayoutY(originY);
		
		widthProperty().addListener((obj, oldVal, newVal) -> {
			//System.out.println("Changed width: " + oldVal + " -> " + newVal);
			//double old = originX;
			originX += (newVal.doubleValue()-width)*originX/width;
			width = (int) newVal.doubleValue()+overflow;
			xAxis.setPrefWidth(width);
			updateAxes();
		});
		
		heightProperty().addListener((obj, oldVal, newVal) -> {
			//System.out.println("Changed height: " + oldVal + " -> " + newVal);
			//double old = originY;
			originY += (newVal.doubleValue()-height)*originY/height;
			//System.out.println("Origin: " + old + " -> " + originY);
			height = (int) newVal.doubleValue()+overflow;
			yAxis.setPrefHeight(height);
			updateAxes();
		});
		
		yAxis = new Axis(yMin, yMax, yTick);
		yAxis.setSide(Side.LEFT);
		yAxis.setTickLabelFill(Color.DARKGRAY);
		yAxis.setMinorTickVisible(false);
		yAxis.setPrefHeight(height);
		yAxis.setLayoutY(-overflow/2);
		
		yAxis.widthProperty().addListener((obj, oldVal, newVal) -> yAxis.setLayoutX(Tools.clamp(originX-newVal.doubleValue()+1, newVal.doubleValue(), width-newVal.doubleValue())));
		xAxis.heightProperty().addListener((obj, oldVal, newVal) -> xAxis.setLayoutY(Tools.clamp(originY, newVal.doubleValue(), height-newVal.doubleValue())));
		getChildren().setAll(xAxis,yAxis);
	}
	
	public void translate(double dx, double dy) {
		originX += dx;
		originY -= dy;
		updateAxes();
	}
	
	public void zoom(double amount) {
		xUnitDist *= amount;
		yUnitDist *= amount;
		updateAxes();
	}
	
	public void setRange(double minX, double maxX, double minY, double maxY) {		
		originY = height-((maxY-minY)*(overflow/2)+minY*height)/(minY-maxY);
		originX = (-(maxX-minX)*(overflow/2)+minX*width)/(minX-maxX);
		yUnitDist = height/(maxY-minY);
		xUnitDist = width/(maxX-minX);
		updateAxes();
	}
	
	public void updateAxes() {
		//Move the yAxis to the origin
		//System.out.println("OriginX: "+ originX + " : " + width + " : " + yAxis.getWidth());
		//System.out.println("yAxis: " + yAxis.toString());
		yAxis.setLayoutX(Tools.clamp(originX-yAxis.getWidth()+1, yAxis.getWidth(), width-yAxis.getWidth()));
		//Update the lower and upper bounds so, that the tick distance remains constant.
		yAxis.setLowerBound((-height+overflow/2+originY)/yUnitDist);
		yAxis.setUpperBound((originY+overflow/2)/yUnitDist);

		//Move the xAxis to the origin.
		//System.out.println("xAxis: " + xAxis.toString());
		//System.out.println("OriginY: "+ originY + " : " + height + " : " + xAxis.getHeight());
		xAxis.setLayoutY(Tools.clamp(originY, xAxis.getHeight(), height-xAxis.getHeight()));
		//Update the lower and upper bounds so, that the tick distance remains constant.
		xAxis.setLowerBound(-(originX+overflow/2)/xUnitDist);
		xAxis.setUpperBound((width-overflow/2-originX)/xUnitDist);
		
		//Make sure that the plots are recentered as well.
		recenterPlots();
		rescalePlots();
		setClip(new Rectangle(0, 0, width-overflow, height-overflow)); //width-overflow = actual pane width
	}
	
	/**
	 * Moves the origin of all plots back to the origin of the system-of-axes. 
	 * Due to the listeners in the Plot class, the plots' ranges will be extended (or shrunk) to fit the new window.
	 */
	public void recenterPlots() {
		for(Node n : getChildren())
			if(n instanceof Plot) {
				n.setLayoutX(originX);
				n.setLayoutY(originY);
				((Plot) n).setContentClip(minX(), minY(), maxX()-minX(), maxY()-minY());
			}
	}
	
	public void rescalePlots() {
		for(Node n : getChildren())
			if(n instanceof Plot) {
				((Plot) n).setScales(xUnitDist, yUnitDist);
				//n.setScaleX(xUnitDist);
				//n.setScaleY(-yUnitDist);
			}
	}
	
	public void zoomFit(Plot plot, double minX, double maxX) {
		Function<Double, Double> f = plot.getFunction();
		double step = (maxX-minX)/200;
		double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
		double val = 0;
		double lastY = Double.NaN;
		
		for(double x = minX; x < maxX; x += step) {
			val = f.apply(x);
			if(Double.isInfinite(val)) val = Double.NaN;
			if(!Double.isNaN(lastY)) {
				double incl = Math.abs(val-lastY)/step;
				//System.out.println("Inclination " + incl + "  " + x);
				if(incl>100) {
					lastY = val;
					continue;
				}
			}
			if(val < min) min = val;
			if(val > max) max = val;
			lastY=val;
		}
		//System.out.println("y-range [" + min + ", " + max + "]");
		setRange(minX, maxX, min, max);		
	}
	
	public Axis getXAxis() {
		return xAxis;
	}
	
	public Axis getYAxis() {
		return yAxis;
	}
	
	public int mapX(double x) {
		return (int) (originX + x*xUnitDist);
	}
	
	public int mapY(double y) {
		return (int) (originY -y*yUnitDist);
	}

	public double minX() {
		return xAxis.getLowerBound();
	}
	
	public double maxX() {
		return xAxis.getUpperBound();
	}
	
	public double minY() {
		return yAxis.getLowerBound();
	}
	
	public double maxY() {
		return yAxis.getUpperBound();
	}
	
	public boolean removePlot(Plot plt) {
		return getChildren().remove(plt);
	}
	
	public void addPlot(Plot plot) {
		getChildren().add(plot);
		plot.setLayoutX(originX);
		plot.setLayoutY(originY);
		plot.setContentClip(minX(), minY(), maxX()-minX(), maxY()-minY());
	}
	
	public Plot addPlot(Function<Double, Double> f, Color color) {
		Plot plot = new Plot(f, minX(), maxX(), xUnitDist, yUnitDist, this, color);
		addPlot(plot);
		return plot;
	}
	
	public Plot addPlot(Function<Double, Double> f, double minX, double  maxX, Color color) {
		Plot plot = addPlot(f, color);
		zoomFit(plot, minX, maxX);
		return plot;
	}
	
	
}