package com.sanger.annotateSniffles;

import com.beust.jcommander.*;

public class PositiveDoubleValidator implements IParameterValidator {
	public void validate(String name, String value) throws ParameterException {
		Double val = Double.parseDouble(value);
		if(val == null || val <= 0)
			throw new ParameterException(String.format("Paramater %s does not exist or is not positive %f", name, value));
	}
}