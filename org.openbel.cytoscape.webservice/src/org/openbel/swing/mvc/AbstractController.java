package org.openbel.swing.mvc;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

//TODO move this out of web services project
/**
 * TODO document
 * 
 * @author jmcmahon
 */
public abstract class AbstractController implements PropertyChangeListener {

    private Collection<AbstractModel> models = new HashSet<AbstractModel>();
    private Collection<PropertyChangeListener> views = new HashSet<PropertyChangeListener>();

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        for (PropertyChangeListener view : views) {
            view.propertyChange(event);
        }
    }

    public void addModel(AbstractModel model) {
        models.add(model);
        model.addPropertyChangeListener(this);
    }

    public void removeModel(AbstractModel model) {
        models.remove(model);
        model.removePropertyChangeListener(this);
    }

    public void addView(PropertyChangeListener view) {
        views.add(view);
    }

    public void removeView(PropertyChangeListener view) {
        views.remove(view);
    }

    // Might want to get rid of this, and couple controller + models together
    // while decoupling is nice, reflection is slow, what happens if you have
    // the same propetry name on multiple view classes
    protected void setModelProperty(String propertyName, Object newValue) {
        for (AbstractModel model : models) {
            Method method = null;
            try {
                method = model.getClass().getMethod("set" + propertyName,
                        new Class[] { newValue.getClass() });
            } catch (SecurityException e) {
                continue;
            } catch (NoSuchMethodException e) {
                continue;
            }

            if (method != null) {
                try {
                    method.invoke(model, newValue);
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

}
