package org.solhost.folko.slclient.models;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.LongProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.solhost.folko.uosl.network.SendableMobile;
import org.solhost.folko.uosl.types.Attribute;
import org.solhost.folko.uosl.types.Direction;

public class SLMobile extends SLObject implements SendableMobile {
    private final Property<Direction> facing;
    private final Map<Attribute, LongProperty> attributes;

    public SLMobile(long serial, int graphic) {
        super(serial, graphic);
        facing = new SimpleObjectProperty<>();
        attributes = new HashMap<>();
        for(Attribute attr : Attribute.values()) {
            attributes.put(attr, new SimpleLongProperty());
        }
    }

    @Override
    public long getAttribute(Attribute attr) {
        return attributes.get(attr).get();
    }

    public ReadOnlyLongProperty getAttributeProperty(Attribute attr) {
        return attributes.get(attr);
    }

    public void setAttribute(Attribute attr, long value) {
        attributes.get(attr).set(value);
    }

    public void setFacing(Direction dir) {
        this.facing.setValue(dir);
    }

    public ReadOnlyProperty<Direction> facingProperty() {
        return facing;
    }

    @Override
    public Direction getFacing() {
        return facing.getValue();
    }
}
