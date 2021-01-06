package com.github.elrol.run4lessplugin;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.util.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class Run4LessOverlay extends Overlay {

    private final PanelComponent panelComponent = new PanelComponent();
    public String url = "";

    public Run4LessOverlay(){
        setPriority(OverlayPriority.HIGHEST);
        setPosition(OverlayPosition.TOP_LEFT);
        setPreferredSize(new Dimension(20,20));
        panelComponent.setOrientation(ComponentOrientation.HORIZONTAL);
        panelComponent.getChildren().add(new ImageComponent(Run4LessPlugin.getLogo(getClass(), url, 60,60)));
    }

    public void setLogo(String url){
        this.url = url;
        panelComponent.getChildren().clear();
        if(!url.equalsIgnoreCase("none"))panelComponent.getChildren().add(new ImageComponent(Run4LessPlugin.getLogo(getClass(), url, 60,60)));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        return panelComponent.render(graphics);
    }

}
