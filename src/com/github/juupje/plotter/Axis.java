package com.github.juupje.plotter;

import java.util.ArrayList;
import java.util.List;

import com.github.juupje.calculator.main.Calculator;
import com.github.juupje.calculator.printer.Printer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Dimension2D;
import javafx.scene.chart.ValueAxis;
import javafx.util.StringConverter;

public class Axis extends ValueAxis<Number> {

	private NumberFormatter numFormatter = new NumberFormatter();
	private double[] tickUnitOptions = { 1, 2, 5 };
	private BooleanProperty autoTickUnit = new SimpleBooleanProperty(true);
	private SimpleDoubleProperty tickUnit = new SimpleDoubleProperty(1) {
		@Override
		protected void invalidated() {
			invalidateRange();
			requestAxisLayout();
		}

		@Override
		public Object getBean() {
			return Axis.this;
		}

		@Override
		public String getName() {
			return "tickUnit";
		}
	};

	public Axis(double lowerBound, double upperBound, double tickUnit) {
		super(lowerBound, upperBound);
		setTickUnit(tickUnit);
		lowerBoundProperty().addListener(obj -> {
			if (autoTickUnit.get())
				this.tickUnit.setValue(0);
		});
		upperBoundProperty().addListener(obj -> {
			if (autoTickUnit.get())
				this.tickUnit.setValue(0);
		});
	}

	@Override
	protected List<Number> calculateMinorTickMarks() {
		List<Number> minorTickValues = new ArrayList<>();
		if (getMinorTickCount() <= 0)
			return minorTickValues;
		final double lowerBound = getLowerBound();
		final double upperBound = getUpperBound();
		final double tickUnit = getTickUnit();
		final double minorTickUnit = tickUnit / getMinorTickCount();
		if (upperBound > lowerBound && tickUnit > 0) {
			if ((upperBound - lowerBound) / minorTickUnit > 1000)
				Calculator.ioHandler
						.out("Creating more than 1000 minor tick marks, something probably went wrong." + "Lower Bound="
								+ lowerBound + ", Upper Bound=" + upperBound + ", Minor Tick Unit=" + minorTickUnit);
			if (lowerBound > 0 || upperBound < 0) {
				if (lowerBound + tickUnit < upperBound) {
					double start = Math.ceil(lowerBound / tickUnit) * tickUnit;
					for (double major = start; major < upperBound; major += tickUnit)
						for (double minor = major + minorTickUnit; minor < major + tickUnit; minor += minorTickUnit)
							minorTickValues.add(minor);
				}
			} else {
				for (double major = 0; major < upperBound; major += tickUnit)
					for (double minor = major + minorTickUnit; minor < major + tickUnit; minor += minorTickUnit)
						minorTickValues.add(minor);
				for (double major = 0; major > lowerBound; major -= tickUnit) {
					for (double minor = major + minorTickUnit; minor < major + tickUnit; minor += minorTickUnit)
						minorTickValues.add(minor);
				}
			}
		}
		return minorTickValues;
	}

	@Override
	protected List<Number> calculateTickValues(double length, Object range) {
		final Object[] rangeProps = (Object[]) range;
		final double lowerBound = (Double) rangeProps[0];
		final double upperBound = (Double) rangeProps[1];
		double tickUnit = (Double) rangeProps[2];
		List<Number> tickValues = new ArrayList<>();
		if (lowerBound == upperBound) {
			tickValues.add(lowerBound);
			return tickValues;
		}
		if (tickUnit <= 0 && autoTickUnit.get()) // recalculate tickUnit
			tickUnit = calculateTickUnit();
		if (tickUnit > 0) {
			if ((upperBound - lowerBound) / tickUnit > 100) {
				Calculator.ioHandler.out("Creating more than 100 major tick marks, something probably went wrong."
						+ "Lower Bound=" + lowerBound + ", Upper Bound=" + upperBound + ", Tick Unit=" + tickUnit);

			} else {
				if (lowerBound > 0 || upperBound < 0) {
					if (lowerBound + tickUnit < upperBound) {
						double start = Math.ceil(lowerBound / tickUnit) * tickUnit;
						for (double major = start; major < upperBound; major += tickUnit)
							tickValues.add(major);
					}
				} else {
					for (double major = 0; major < upperBound; major += tickUnit)
						tickValues.add(major);
					for (double major = -tickUnit; major > lowerBound; major -= tickUnit) {
						tickValues.add(major);
					}
				}
			}
		}
		if (tickValues.size() == 0) {
			tickValues.add(lowerBound);
			tickValues.add(upperBound);
		}
		return tickValues;
	}

	@Override
	protected void setRange(Object range, boolean animate) {
		final Object[] props = (Object[]) range;
		final double lowerBound = (Double) props[0];
		final double upperBound = (Double) props[1];
		final double tickUnit = (Double) props[2];
		final double scale = (Double) props[3];
		setLowerBound(lowerBound);
		setUpperBound(upperBound);
		setTickUnit(tickUnit);
		currentLowerBound.set(lowerBound);
		setScale(scale);
	}

	@Override
	protected Object getRange() {
		return new Object[] { getLowerBound(), getUpperBound(), getTickUnit(), getScale() };
	}

	@Override
	protected String getTickMarkLabel(Number value) {
		return numFormatter.toString(value);
	}

	/**
	 * Measure the size of the label for given tick mark value. This uses the font
	 * that is set for the tick marks
	 *
	 * @param value tick mark value
	 * @param range range to use during calculations
	 * @return size of tick mark label for given value
	 */
	@Override
	protected Dimension2D measureTickMarkSize(Number value, Object range) {
		return measureTickMarkLabelSize(numFormatter.toString(value), getTickLabelRotation());
	}

	protected double calculateTickUnit() {
		double length = getUpperBound() - getLowerBound();
		double oom = Math.pow(10, Math.floor(Math.log10(length)) - 1);
		double best = 1;
		double minDiff = Double.MAX_VALUE;
		for (double tickUnit : tickUnitOptions) {
			if (Math.abs(20 - length / (tickUnit * oom)) < minDiff) {
				minDiff = Math.abs(20 - length / (tickUnit * oom));
				best = tickUnit;
			}
		}
		setTickUnit(best * oom);
		return best * oom;
	}

	public double[] getTickUnitOptions() {
		return tickUnitOptions;
	}

	public void setTickUnitOptions(double[] d) {
		tickUnitOptions = d;
	}

	// --------------PROPERTIES-----------------------

	public double getTickUnit() {
		return tickUnit.get();
	}

	public void setTickUnit(double value) {
		if (!autoTickUnit.get())
			tickUnit.set(value);
	}

	public SimpleDoubleProperty tickUnitProperty() {
		return tickUnit;
	}

	public boolean isAutoTickUnit() {
		return autoTickUnit.get();
	}

	public void setAutoTickUnit(boolean b) {
		autoTickUnit.set(b);
	}

	public BooleanProperty autoTickUnit() {
		return autoTickUnit;
	}

	@Override
	public String toString() {
		return "Axis [min=" + getLowerBound() + ", max=" + getUpperBound() + ", tickDist=" + tickUnit.getValue() + "]";
	}
	
	private class NumberFormatter extends StringConverter<Number> {

		@Override
		public String toString(Number value) {
			return Printer.numToString(value.doubleValue());
		}

		@Override
		public Number fromString(String string) {
			return Double.valueOf(string);
		}
	}
}