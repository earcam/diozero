package com.diozero.internal.provider;

import com.diozero.api.DeviceAlreadyOpenedException;
import com.diozero.api.DeviceMode;
import com.diozero.api.PinInfo;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.diozero.util.RuntimeIOException;

public interface AnalogOutputDeviceFactoryInterface extends DeviceFactoryInterface {
	default AnalogOutputDeviceInterface provisionAnalogOutputDevice(int dacNumber) throws RuntimeIOException {
		PinInfo pin_info = getBoardPinInfo().getByDacNumber(dacNumber);
		if (pin_info == null || ! pin_info.isSupported(DeviceMode.ANALOG_OUTPUT)) {
			throw new IllegalArgumentException("Invalid mode (analog output) for DAC " + dacNumber);
		}
		
		String key = createPinKey(pin_info);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		AnalogOutputDeviceInterface device = createAnalogOutputDevice(key, pin_info);
		deviceOpened(device);
		
		return device;
	}

	AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo);
}