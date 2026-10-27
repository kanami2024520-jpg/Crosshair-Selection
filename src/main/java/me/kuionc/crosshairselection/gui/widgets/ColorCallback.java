package me.kuionc.crosshairselection.gui.widgets;

/**
 * Receives the colour chosen in {@link me.kuionc.crosshairselection.gui.ColorPickerScreen}.
 */
public interface ColorCallback {

    void accept(int argb);
}
