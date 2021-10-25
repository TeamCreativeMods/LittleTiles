package team.creative.littletiles.common.gui;

import java.util.ArrayList;

import com.creativemd.creativecore.common.gui.controls.gui.GuiColorPicker;
import com.creativemd.creativecore.common.gui.controls.gui.GuiComboBoxTranslated;
import com.creativemd.creativecore.common.gui.controls.gui.GuiScrollBox;
import com.creativemd.littletiles.common.util.grid.LittleGridContext;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.item.ItemStack;
import team.creative.creativecore.common.gui.controls.collection.GuiComboBox;
import team.creative.creativecore.common.gui.event.GuiControlChangedEvent;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.common.api.tool.ILittleEditor;
import team.creative.littletiles.common.gui.configure.SubGuiConfigure;
import team.creative.littletiles.common.item.ItemLittlePaintBrush;
import team.creative.littletiles.common.placement.shape.LittleShape;
import team.creative.littletiles.common.placement.shape.ShapeRegistry;

public class SubGuiColorTube extends SubGuiConfigure {
    
    public SubGuiColorTube(ItemStack stack) {
        super(140, 173, stack);
    }
    
    public LittleGridContext getContext() {
        return ((ILittleEditor) stack.getItem()).getPositionContext(stack);
    }
    
    @Override
    public void createControls() {
        Color color = ColorUtils.IntToRGBA(ItemLittlePaintBrush.getColor(stack));
        // color.setAlpha(255);
        controls.add(new GuiColorPicker("picker", 2, 2, color, LittleTiles.CONFIG.isTransparencyEnabled(getPlayer()), LittleTiles.CONFIG.getMinimumTransparency(getPlayer())));
        
        ArrayList<String> shapes = new ArrayList<>(ShapeRegistry.allShapeNames());
        GuiComboBox box = new GuiComboBoxTranslated("shape", 0, 50, 134, "shape.", shapes);
        LittleShape shape = ItemLittlePaintBrush.getShape(stack);
        box.select(shape == null ? "tile" : shape.getKey());
        GuiScrollBox scroll = new GuiScrollBox("settings", 0, 73, 134, 90);
        controls.add(box);
        controls.add(scroll);
        onChange();
    }
    
    @Override
    public void saveConfiguration() {
        GuiComboBox box = (GuiComboBox) get("shape");
        GuiScrollBox scroll = (GuiScrollBox) get("settings");
        LittleShape shape = box.getCaption().equals("tile") || box.getCaption().equals("") ? null : ShapeRegistry.getShape(box.getCaption());
        
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        nbt.setString("shape", shape == null ? "tile" : shape.getKey());
        GuiColorPicker picker = (GuiColorPicker) get("picker");
        nbt.setInteger("color", ColorUtils.RGBAToInt(picker.color));
        if (shape != null)
            shape.saveCustomSettings(scroll, nbt, getContext());
    }
    
    @CustomEventSubscribe
    public void onComboBoxChange(GuiControlChangedEvent event) {
        if (event.source.is("shape"))
            onChange();
    }
    
    public void onChange() {
        GuiComboBox box = (GuiComboBox) get("shape");
        GuiScrollBox scroll = (GuiScrollBox) get("settings");
        
        scroll.controls.clear();
        LittleShape shape = box.getCaption().equals("tile") || box.getCaption().equals("") ? null : ShapeRegistry.getShape(box.getCaption());
        if (shape != null) {
            scroll.controls.addAll(shape.getCustomSettings(stack.getTagCompound(), getContext()));
            scroll.refreshControls();
        }
    }
    
}
