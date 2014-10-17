package org.solhost.folko.slclient.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.solhost.folko.uosl.network.SendableItem;

public class SLItem extends SLObject implements SendableItem {
    private final IntegerProperty amount;
    private final Property<Short> layer, facingOverride;

    public SLItem() {
        super();
        amount = new SimpleIntegerProperty();
        layer = new SimpleObjectProperty<>();
        facingOverride = new SimpleObjectProperty<>();
    }

    public void setAmount(int amount) {
        this.amount.set(amount);
    }

    public IntegerProperty amountProperty() {
        return amount;
    }

    @Override
    public int getAmount() {
        return amount.get();
    }

    public void setLayer(short layer) {
        this.layer.setValue(layer);
    }

    public Property<Short> layerProperty() {
        return layer;
    }

    @Override
    public short getLayer() {
        return layer.getValue();
    }

    public void setFacingOverride(short override) {
        this.facingOverride.setValue(override);
    }

    public Property<Short> facingOverrideProperty() {
        return facingOverride;
    }

    @Override
    public short getFacingOverride() {
        return facingOverride.getValue();
    }

}
