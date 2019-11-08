package com.github.juupje.plotter.fxml.controllers;

import java.util.HashMap;
import java.util.function.Function;

import com.github.juupje.calculator.graph.ValueChangedListener;
import com.github.juupje.calculator.helpers.Shape;
import com.github.juupje.calculator.helpers.exceptions.TreeException;
import com.github.juupje.calculator.main.Calculator;
import com.github.juupje.calculator.main.Variable;
import com.github.juupje.calculator.mathobjects.MFunction;
import com.github.juupje.calculator.mathobjects.MReal;
import com.github.juupje.calculator.mathobjects.MathObject;
import com.github.juupje.plotter.CartesianAxes;
import com.github.juupje.plotter.ColorIterator;
import com.github.juupje.plotter.Plot;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.AnchorPane;

public class Plot2DPaneController {

	@FXML
	private AnchorPane pane;

	private HashMap<Variable, Plot> objectsInGraph;
	private CartesianAxes axes;
	private ColorIterator colors;
	
	@FXML
	private void initialize() {
		colors = new ColorIterator();
		objectsInGraph = new HashMap<>();
		axes = new CartesianAxes(600, 550, -8, 8, 1, -6, 6, 1);
		pane.getChildren().add(axes);
		AnchorPane.setBottomAnchor(axes, 0.0);
		AnchorPane.setTopAnchor(axes, 0.0);
		AnchorPane.setLeftAnchor(axes, 0.0);
		AnchorPane.setRightAnchor(axes, 0.0);
	}
	
	public void add(Variable var, boolean dynamic) {
		if (objectsInGraph.containsKey(var))
			return;

		Plot plt = axes.addPlot(toFunction(var.get()), colors.next());
		if(dynamic) makeDynamic(var, plt);
		if (plt != null)
			objectsInGraph.put(var, plt);
	}
	
	public void add(Variable var, double begin, double end, boolean dynamic) {
		if (objectsInGraph.containsKey(var))
			return;

		Plot plt = axes.addPlot(toFunction(var.get()), begin, end, colors.next());
		if(dynamic) makeDynamic(var, plt);
		if (plt != null)
			objectsInGraph.put(var, plt);
	}
	
	private void makeDynamic(Variable var, final Plot plot) {
		Calculator.dependencyGraph.addListener(var, new ValueChangedListener<Variable>() {
			@Override
			public void onValueChanged(Variable v) {
				plot.updateFunction(toFunction(v.get()));
			}
		});
	}

	private Function<Double, Double> toFunction(MathObject mo) {
		if (mo instanceof MFunction) {
			if (((MFunction) mo).getParameters().length == 1 && mo.shape().equals(Shape.SCALAR)) {
				return x -> {
					try {
						return ((MReal) ((MFunction) mo).evaluateAt(new MReal(x))).getValue();
					} catch (TreeException e) {
						Calculator.errorHandler.handle("Something went wrong while evaluating this function.", e);
						return null;
					}
				};
			} else
				throw new IllegalArgumentException("Can't plot multivariate or multi-dimensional functions.");
		}
		throw new IllegalArgumentException("Only functions can be plotted, got " + mo.getClass().getSimpleName());
	}

	@SuppressWarnings("unlikely-arg-type")
	public boolean remove(String s) {
		return axes.removePlot(objectsInGraph.remove(s));
	}
	
	public boolean remove(Variable var) {
		return axes.removePlot(objectsInGraph.remove(var));
	}
	
	public Plot getPlot(String name) {
		return objectsInGraph.get(new Variable(name));
	}

	// Mouse handlers
	private double lastX, lastY;
	private boolean zooming = false;
	private double oldZoom;
	

	@FXML
	private void onMousePress(MouseEvent e) {
		lastX = e.getSceneX();
		lastY = e.getSceneY();
	}

	@FXML
	private void onMouseRelease(MouseEvent e) {
	}
	
	@FXML
	void onZoomStarted(ZoomEvent e) {
		zooming = true;
		oldZoom = 1;
	}
	
	@FXML
	void onZoomFinished(ZoomEvent e) {
		zooming = false;
	}
	
	@FXML
	void onZoom(ZoomEvent event) {
		axes.zoom(event.getTotalZoomFactor()/oldZoom);//, event.getX(), event.getY());
		oldZoom = event.getTotalZoomFactor();
	}
	
	@FXML
	void onScroll(ScrollEvent event) {
		if(zooming) return;
		if(!event.isDirect()) {
				final double zoomFactor = 1.05;
			if (event.getDeltaY() > 0)
				axes.zoom(zoomFactor * event.getDeltaY() / event.getMultiplierY());
			else
				axes.zoom(1 / zoomFactor * -event.getDeltaY() / event.getMultiplierY());
		} else { //event triggered by touchscreen
			axes.translate(event.getSceneX() - lastX, lastY - event.getSceneY());
			lastX = event.getSceneX();
			lastY = event.getSceneY();
		}
	}

	@FXML
	void onScrollStarted(ScrollEvent e) {
		lastX = e.getX();
		lastY = e.getY();
	}

	@FXML
	void onMouseDrag(MouseEvent e) {
		if(!e.isSynthesized()) {				
			axes.translate(e.getSceneX() - lastX, lastY - e.getSceneY());
			lastX = e.getSceneX();
			lastY = e.getSceneY();
		}
	}
}