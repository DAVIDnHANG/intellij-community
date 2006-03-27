package com.intellij.uiDesigner.propertyInspector;

import com.intellij.uiDesigner.XmlWriter;
import com.intellij.uiDesigner.radComponents.RadComponent;
import com.intellij.util.ArrayUtil;
import com.intellij.openapi.util.Comparing;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public abstract class IntrospectedProperty<V> extends Property<RadComponent, V> {
  protected final static Object[] EMPTY_OBJECT_ARRAY=new Object[]{};

  /**
   * This method is used to set property value to "delegee" JComponent
   */
  protected final Method myReadMethod;
  /**
   * This method is used to get property value from "delegee" JComponent
   */
  private final Method myWriteMethod;

  public IntrospectedProperty(
    final String name,
    final Method readMethod,
    final Method writeMethod
  ){
    super(null, name);
    myReadMethod = readMethod;
    myWriteMethod = writeMethod;
  }

  /**
   * <b>Do not overide this method without serious reason!</b>
   */
  public V getValue(final RadComponent component){
    //noinspection unchecked
    return (V)invokeGetter(component);
  }

  protected Object invokeGetter(final RadComponent component) {
    try {
      return myReadMethod.invoke(component.getDelegee(), EMPTY_OBJECT_ARRAY);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * <b>Do not overide this method without serious reason!</b>
   */
  protected void setValueImpl(final RadComponent component,final V value) throws Exception{
    invokeSetter(component, value);
  }

  protected void invokeSetter(final RadComponent component, final Object value) throws IllegalAccessException, InvocationTargetException {
    myWriteMethod.invoke(component.getDelegee(), value);
  }

  /**
   * Serializes (writes) propertie's value
   *
   * @param value property value which should be serialized.
   * @param writer writer which should be used for serialization. It is assumed that
   * before invocation of this method <code>writer</code> already has opened tag
   * that corresponds to this property. You can just append some attributes
   * here or add some subtags.
   */
  public abstract void write(@NotNull V value, XmlWriter writer);

  @Override public boolean isModified(final RadComponent component) {
    return component.isMarkedAsModified(this);
  }

  @Override public void resetValue(RadComponent component) throws Exception {
    final V defaultValue = getDefaultValue(component);
    invokeSetter(component, defaultValue);
    markTopmostModified(component, false);
  }

  private V getDefaultValue(final RadComponent component) throws Exception {
    final Constructor constructor = component.getComponentClass().getConstructor(ArrayUtil.EMPTY_CLASS_ARRAY);
    constructor.setAccessible(true);
    JComponent newComponent = (JComponent)constructor.newInstance(ArrayUtil.EMPTY_OBJECT_ARRAY);
    //noinspection unchecked
    return (V) myReadMethod.invoke(newComponent, EMPTY_OBJECT_ARRAY);
  }

  public void importSnapshotValue(final JComponent component, final RadComponent radComponent) {
    try {
      //noinspection unchecked
      V value = (V) myReadMethod.invoke(component, EMPTY_OBJECT_ARRAY);
      V defaultValue = getDefaultValue(radComponent);
      if (!Comparing.equal(value, defaultValue)) {
        setValue(radComponent, value);
      }
    }
    catch (Exception e) {
      // ignore
    }
  }
}
