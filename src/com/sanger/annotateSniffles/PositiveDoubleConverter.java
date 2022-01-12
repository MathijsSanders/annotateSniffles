package com.sanger.annotateSniffles;

import com.beust.jcommander.IStringConverter;

public class PositiveDoubleConverter implements IStringConverter<Double> {
	@Override
	public Double convert(String value) {
		return Double.parseDouble(value);
	}
}
