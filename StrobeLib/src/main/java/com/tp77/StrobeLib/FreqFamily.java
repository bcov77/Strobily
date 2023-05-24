package com.tp77.StrobeLib;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class FreqFamily {

	double mBase;
	
	double[] mMagnitudes = new double[10];
	
	int mHighest = 1;
	int mHarmonics = 1;
	double mMagSum = 0;
	

	public void writeExternal(ObjectOutput o) throws IOException {
		o.writeDouble(mBase);
		o.writeInt(mHighest);
		o.writeInt(mHarmonics);
		o.writeDouble(mMagSum);
		for (int iii = 0; iii < 10; iii++)
			o.writeDouble(mMagnitudes[iii]);
	}
	
	public FreqFamily(ObjectInput o) throws IOException {
		mBase = o.readDouble();
		mHighest = o.readInt();
		mHarmonics = o.readInt();
		mMagSum = o.readDouble();
		for (int iii = 0; iii < 10; iii++)
			mMagnitudes[iii] = o.readDouble();
	}
	
	public FreqFamily(double base, double mag) {
		mBase = base;
		mMagnitudes[1] = mag;
		mMagSum += mag;
	}
	
	
	public boolean isHarmonic(double frequency, double magnitude, boolean change) {
		double mod = (mBase % frequency) / frequency;
		if (mod < 0.03f || mod > 0.97f) {
			int harmonic = (int) Math.round(mBase / frequency);
			if (harmonic >= 10)
				return false;
			mMagnitudes[harmonic] = magnitude;
			mHighest = harmonic;
//			mBase = frequency * harmonic;
			mHarmonics++;
			mMagSum += magnitude;
			return true;
		}
		return false;
	}
	
	public double[] getFamily() {

		int len = mHighest;
		if (len < 5)
			len = 5;
		
		double[] toRet = new double[len];
		int added = 0;
		
		for (int iii = 1; iii < len+1; iii++) {
			toRet[added++] = mBase / (double)iii;
		}
		
		return toRet;
		
	}
	

	public double[] getMags() {
		int len = mHighest;
		if (len < 5)
			len = 5;
		
		double[] toRet = new double[len];
		int added = 0;
		
		for (int iii = 1; iii < len+1; iii++) {
			toRet[added++] = mMagnitudes[iii];
		}
		
		return toRet;
		
	}
}
