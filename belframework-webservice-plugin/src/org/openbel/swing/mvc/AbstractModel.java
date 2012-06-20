package org.openbel.swing.mvc;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

// TODO move this out of web services project
/**
 * TODO document
 * 
 * @author jmcmahon
 */
public abstract class AbstractModel {

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(
            this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    protected void firePropertyChange(String propertyName, Object oldValue,
            Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue,
                newValue);
    }
}
