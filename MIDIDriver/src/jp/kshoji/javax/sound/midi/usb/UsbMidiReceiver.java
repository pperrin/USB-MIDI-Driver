package jp.kshoji.javax.sound.midi.usb;


import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.javax.sound.midi.MetaMessage;
import jp.kshoji.javax.sound.midi.MidiDevice;
import jp.kshoji.javax.sound.midi.MidiDeviceReceiver;
import jp.kshoji.javax.sound.midi.MidiMessage;
import jp.kshoji.javax.sound.midi.ShortMessage;
import jp.kshoji.javax.sound.midi.SysexMessage;

/**
 * {@link jp.kshoji.javax.sound.midi.Receiver} implementation
 *
 * @author K.Shoji
 *
 */
public final class UsbMidiReceiver implements MidiDeviceReceiver {
    private final UsbMidiDevice usbMidiDevice;
	private final UsbDevice usbDevice;
	private final UsbDeviceConnection usbDeviceConnection;
	private final UsbInterface usbInterface;
	private final UsbEndpoint outputEndpoint;
	private int cableId;
	
	private MidiOutputDevice outputDevice = null;
	
	public UsbMidiReceiver(UsbMidiDevice usbMidiDevice, UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection, UsbInterface usbInterface, UsbEndpoint outputEndpoint) {
        this.usbMidiDevice = usbMidiDevice;
		this.usbDevice = usbDevice;
		this.usbDeviceConnection = usbDeviceConnection;
		this.usbInterface = usbInterface;
		this.outputEndpoint = outputEndpoint;
		cableId = 0;

        open();
    }

	@Override
	public void send(MidiMessage message, long timeStamp) {
        if (outputDevice == null) {
            // already closed
            return;
        }

		if (message instanceof MetaMessage) {
			final MetaMessage metaMessage = (MetaMessage) message;
			outputDevice.sendMidiSystemCommonMessage(cableId, metaMessage.getData());
		} else if (message instanceof SysexMessage) {
			final SysexMessage sysexMessage = (SysexMessage) message;
			outputDevice.sendMidiSystemExclusive(cableId, sysexMessage.getData());
		} else if (message instanceof ShortMessage) {
			final ShortMessage shortMessage = (ShortMessage) message;
			switch (shortMessage.getCommand()) {
				case ShortMessage.CHANNEL_PRESSURE:
					outputDevice.sendMidiChannelAftertouch(cableId, shortMessage.getChannel(), shortMessage.getData1());
					break;
				case ShortMessage.CONTROL_CHANGE:
					outputDevice.sendMidiControlChange(cableId, shortMessage.getChannel(), shortMessage.getData1(), shortMessage.getData2());
					break;
				case ShortMessage.NOTE_OFF:
					outputDevice.sendMidiNoteOff(cableId, shortMessage.getChannel(), shortMessage.getData1(), shortMessage.getData2());
					break;
				case ShortMessage.NOTE_ON:
					outputDevice.sendMidiNoteOn(cableId, shortMessage.getChannel(), shortMessage.getData1(), shortMessage.getData2());
					break;
				case ShortMessage.PITCH_BEND:
					outputDevice.sendMidiPitchWheel(cableId, shortMessage.getChannel(), shortMessage.getData1() | (shortMessage.getData2() << 7));
					break;
				case ShortMessage.POLY_PRESSURE:
					outputDevice.sendMidiPolyphonicAftertouch(cableId, shortMessage.getChannel(), shortMessage.getData1(), shortMessage.getData2());
					break;
				case ShortMessage.PROGRAM_CHANGE:
					outputDevice.sendMidiProgramChange(cableId, shortMessage.getChannel(), shortMessage.getData1());
					break;
				default:
			}
		}
	}

    /**
     * must be called from UI thread.
     */
	public void open() {
        if (outputDevice == null) {
            outputDevice = new MidiOutputDevice(usbDevice, usbDeviceConnection, usbInterface, outputEndpoint);
        }
	}
	
	@Override
	public void close() {
		if (outputDevice != null) {
			outputDevice.stop();
		}
        outputDevice = null;
	}

	public int getCableId() {
		return cableId;
	}

	public void setCableId(int cableId) {
		this.cableId = cableId;
	}

    @Override
    public MidiDevice getMidiDevice() {
        return usbMidiDevice;
    }
}
