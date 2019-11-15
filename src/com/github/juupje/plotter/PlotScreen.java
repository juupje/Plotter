package com.github.juupje.plotter;
	
import com.github.juupje.calculator.helpers.exceptions.InvalidOperationException;
import com.github.juupje.calculator.main.Variable;
import com.github.juupje.plotter.fxml.controllers.GraphViewController;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class PlotScreen extends Application {
	
	GraphViewController controller;
	Stage stage;
	private static Object lock = new Object();
	public static boolean isOpen = false;
	private static boolean launched = false;
	private static PlotScreen instance;
	
	public void show(Stage stage) {
		try {
			Plotter.setPlotScreen(this);
			this.stage = stage;
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(PlotScreen.class.getResource("/com/github/juupje/plotter/fxml/GraphView.fxml"));
			loader.setController(controller = new GraphViewController());
			Scene scene = new Scene(loader.load(),600,600);
			stage.setScene(scene);
			stage.show();
			stage.setOnCloseRequest(event -> isOpen = false);
			isOpen = true;
			synchronized(lock) {
				lock.notify();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}		
	}
	
	public static void create() {
		if(!launched)
			new Thread(() -> {
				launch(PlotScreen.class);
			}).start();
		else {
			Platform.runLater(() -> {
				instance.show(new Stage());
			});
		}
		waitForLock();
	}
	
	private static void waitForLock() {
		try {
			synchronized(lock) {
				lock.wait();
			}
		} catch(InterruptedException e) {}
	}
	
	public void plot(Variable var, double begin, double end, boolean dynamic) {
		if(!isOpen)
			create();
		Platform.runLater(() -> {
			controller.addPlotPane().add(var, begin, end, dynamic);
			synchronized(lock) {
				lock.notify();
			}
		});
		waitForLock();
	}
	
	public void anim(Variable var, String variable, double min, double max, double time) {
		if(!isOpen)
			create();
		Platform.runLater(() -> {
			controller.getSelected().addAnim(var, variable, min, max, time);
			synchronized(lock) {
				lock.notify();
			}
		});
		waitForLock();
	}

	public void plot(Variable var, boolean dynamic) {
		if(!isOpen)
			create();
		if(!controller.hasPlots())
			throw new InvalidOperationException("Cannot add plot if there are no available plots.");
		Platform.runLater(() -> {
			controller.getSelected().add(var, dynamic);
			synchronized(lock) {
				lock.notify();
			}
		});
		waitForLock();
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		launched = true;
		instance = this;
		Platform.setImplicitExit(false);
		show(primaryStage);
	}
}
