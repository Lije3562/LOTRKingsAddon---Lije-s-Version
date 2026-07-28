package com.enovak.lotrmoremobs.render.entity;

import com.enovak.lotrmoremobs.model.mumakil.LOTRModelMumakilDriver;
import lotr.client.render.entity.LOTRRenderNearHaradrim;

/**
 * Preserves LOTR's Southron renderer while allowing the mounted driver model to
 * display its synchronized horn pose.
 */
public class LOTRRenderMumakilDriver extends LOTRRenderNearHaradrim {
    public LOTRRenderMumakilDriver() {
        super();
        LOTRModelMumakilDriver driverModel =
                new LOTRModelMumakilDriver();
        this.mainModel = driverModel;
        this.modelBipedMain = driverModel;
    }
}
