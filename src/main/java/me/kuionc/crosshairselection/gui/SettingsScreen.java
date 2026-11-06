package me.kuionc.crosshairselection.gui;

import me.kuionc.crosshairselection.gui.widgets.OptionSlider;
import me.kuionc.crosshairselection.gui.widgets.SliderHost;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared behaviour for the settings screens: controls declare a dependency and are greyed out
 * instead of hidden, so the layout stays stable while a feature is switched off.
 */
public abstract class SettingsScreen extends GuiScreen implements SliderHost {

    /** A control is enabled only while its condition returns true. */
    public interface Condition {
        boolean test();
    }

    private final Map<Integer, Condition> dependencies = new HashMap<Integer, Condition>();
    private final List<GuiButton> managed = new ArrayList<GuiButton>();

    /** Registers a button and makes it follow the given condition. */
    protected GuiButton add(GuiButton button, Condition condition) {
        buttonList.add(button);
        managed.add(button);
        if (condition != null) {
            dependencies.put(button.id, condition);
        }
        return button;
    }

    protected GuiButton add(GuiButton button) {
        return add(button, null);
    }

    protected OptionSlider slider(int id, int x, int y, int width, float min, float max,
                                  float value, boolean integer, int decimals,
                                  String labelKey) {
        return (OptionSlider) add(new OptionSlider(id, x, y, width, 18, min, max, value,
                integer, decimals, labelKey, this));
    }

    protected OptionSlider slider(int id, int x, int y, int width, float min, float max,
                                  float value, boolean integer, String labelKey) {
        return (OptionSlider) add(new OptionSlider(id, x, y, width, 18, min, max, value,
                integer, labelKey, this));
    }

    /**
     * Marks a button as always clickable. Used for controls that open a sub-page or switch tabs,
     * which must stay reachable even when the feature they configure is switched off.
     */
    protected void keepEnabled(GuiButton button) {
        dependencies.remove(button.id);
    }

    protected void keepEnabled(int id) {
        dependencies.remove(id);
    }

    /** Applies every dependency. Call after building the controls and after any change. */
    protected void refreshEnabledStates() {
        for (GuiButton button : managed) {
            Condition condition = dependencies.get(button.id);
            if (condition != null) {
                button.enabled = condition.test();
            }
        }
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }
        onButtonPressed(button);
        refreshEnabledStates();
    }

    @Override
    public void onSliderChanged(int optionId, float value) {
        onSliderValue(optionId, value);
        refreshEnabledStates();
    }

    protected abstract void onButtonPressed(GuiButton button);

    protected abstract void onSliderValue(int optionId, float value);
}
