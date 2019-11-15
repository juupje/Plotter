package com.github.juupje.plotter.fxml.controllers;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.juupje.calculator.main.Calculator;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class GraphViewController {

	@FXML
	private TabPane tabPane;
	
	private PlotTab selected;
	
	@FXML
	void initialize() {
		
	}
	
	public Plot2DPaneController addPlotPane() {
		tabPane.getTabs().add(selected = new PlotTab());
		return select(tabPane.getTabs().size()-1);
	}
	
	public boolean hasPlots() {
		return tabPane.getTabs().size()>0;
	}
	
	public Plot2DPaneController select(int index) {
		selected = (PlotTab) tabPane.getTabs().get(index);
		tabPane.getSelectionModel().clearAndSelect(index);
		return selected.getController();
	}
	
	public int getTabCount() {
		return tabPane.getTabs().size();
	}
	
	public Plot2DPaneController getSelected() {
		return selected.getController();
	}
	
	private static final AtomicInteger idGenerator = new AtomicInteger(1);
	class PlotTab extends Tab {
		
		Plot2DPaneController controller;
		int id;
		
		public PlotTab() {
			id = idGenerator.getAndIncrement();
			try {
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(GraphViewController.class.getResource("/com/github/juupje/plotter/fxml/Plot2DPane.fxml"));
				loader.setController(controller = new Plot2DPaneController());
				setContent(loader.load());
				setText("Plot " + id);
			} catch (IOException e) {
				Calculator.errorHandler.handle("Plotpane could not be loaded.", e);
			}
		}
		
		public Plot2DPaneController getController() {
			return controller;
		}		
	}
}