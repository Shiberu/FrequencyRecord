package com.kkago.frequencyrecord;

import java.io.Serializable;

public class FreqRead implements Serializable{
	double frequencyValue;
	String frequencyName;
	int flag;

	public FreqRead(double fv, String fn) {
		this.frequencyValue = fv;
		this.frequencyName = fn;
		this.flag = 2;
	}

}
