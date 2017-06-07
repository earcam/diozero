package com.diozero.internal.provider.mmap;

/*
 * #%L
 * Device I/O Zero - Java Native provider for the Raspberry Pi
 * %%
 * Copyright (C) 2016 mattjlewis
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


import org.pmw.tinylog.Logger;

import com.diozero.api.*;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class MmapDigitalInputOutputDevice extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputOutputDeviceInterface, InputEventListener<DigitalInputEvent> {
	private MmapDeviceFactory mmapDeviceFactory;
	private int gpio;
	private DeviceMode mode;
	private GpioPullUpDown pud;
	private GpioDigitalInputDeviceInterface sysFsDigitialInput;

	public MmapDigitalInputOutputDevice(MmapDeviceFactory deviceFactory, String key,
			int gpio, DeviceMode mode) {
		super(key, deviceFactory);
		
		this.mmapDeviceFactory = deviceFactory;
		this.gpio = gpio;

		// For when mode is switched to input
		this.pud = GpioPullUpDown.NONE;

		sysFsDigitialInput = mmapDeviceFactory.getSysFsDeviceFactory().provisionDigitalInputDevice(
				gpio, pud, GpioEventTrigger.BOTH);
		
		setMode(mode);
	}
	
	private static void checkMode(DeviceMode mode) {
		if (mode != DeviceMode.DIGITAL_INPUT && mode != DeviceMode.DIGITAL_OUTPUT) {
			throw new IllegalArgumentException("Invalid mode, must be DIGITAL_INPUT or DIGITAL_OUTPUT");
		}
	}

	@Override
	public DeviceMode getMode() {
		return mode;
	}

	@Override
	public void setMode(DeviceMode mode) {
		checkMode(mode);
		
		// No change?
		if (this.mode != null && mode == this.mode) {
			return;
		}

		mmapDeviceFactory.getMmapGpio().setMode(gpio, mode);
		this.mode = mode;
		
		if (mode == DeviceMode.DIGITAL_INPUT) {
			mmapDeviceFactory.getMmapGpio().setPullUpDown(gpio, pud);
		}
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return mmapDeviceFactory.getMmapGpio().gpioRead(gpio);
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		if (mode != DeviceMode.DIGITAL_OUTPUT) {
			throw new IllegalStateException("Can only set output value for digital output pins");
		}
		mmapDeviceFactory.getMmapGpio().gpioWrite(gpio, value);
	}

	@Override
	public int getGpio() {
		return gpio;
	}
	
	@Override
	protected void enableListener() {
		sysFsDigitialInput.setListener(this);
	}
	
	@Override
	protected void disableListener() {
		sysFsDigitialInput.removeListener();
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		disableListener();
		// FIXME No GPIO close method?
		// TODO Revert to default input mode?
		// What do wiringPi / pigpio do?
	}
}