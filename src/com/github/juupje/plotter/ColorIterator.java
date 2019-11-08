package com.github.juupje.plotter;

import javafx.scene.paint.Color;

public class ColorIterator {
	
	int n = 0;
	float p = 1;
	float saturation = 0.8f, brightness=1;

	public ColorIterator() {}
	public ColorIterator(float saturation, float brightness) {
		this.saturation = saturation;
		this.brightness = brightness;
	}
	
	public Color next() {
		double f = (2*n+1)/p;
		if(f>1) {
			n = 0;
			p *= 2;
			f = (2*n+1)/p;
		}
		//System.out.println("Color: " + f + ", " + (2*n+1) + ", " + p +"  "  + this);
		n++;
		return Color.hsb(f*360, saturation, brightness);
	}
}
