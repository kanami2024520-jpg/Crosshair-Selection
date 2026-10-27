package me.kuionc.crosshairselection.gui.widgets;

/**
 * Callback used by {@link OptionSlider} so the screen can write the value into the config.
 */
public interface SliderHost {

    void onSliderChanged(int optionId, float value);
}
