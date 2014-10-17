package org.solhost.folko.slclient.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.solhost.folko.uosl.network.SendableObject;
import org.solhost.folko.uosl.types.Point3D;

public class SLObject implements SendableObject {
    protected LongProperty serial;
    protected Property<Point3D> location;
    protected IntegerProperty graphic, hue;
    protected StringProperty name;

    public SLObject() {
        serial = new SimpleLongProperty();
        location = new SimpleObjectProperty<Point3D>();
        graphic = new SimpleIntegerProperty();
        hue = new SimpleIntegerProperty();
        name = new SimpleStringProperty();
    }

    public long getSerial() {
        return serial.get();
    }

    public LongProperty serialProperty() {
        return serial;
    }

    public void setSerial(long serial) {
        this.serial.set(serial);
    }

    public Point3D getLocation() {
        return location.getValue();
    }

    public Property<Point3D> locationProperty() {
        return location;
    }

    public void setLocation(Point3D location) {
        this.location.setValue(location);
    }

    public int getGraphic() {
        return graphic.get();
    }

    public IntegerProperty graphicProperty() {
        return graphic;
    }

    public void setGraphic(int graphic) {
        this.graphic.set(graphic);
    }

    public int getHue() {
        return hue.get();
    }

    public IntegerProperty hueProperty() {
        return hue;
    }

    public void setHue(int hue) {
        this.hue.set(hue);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }
}
