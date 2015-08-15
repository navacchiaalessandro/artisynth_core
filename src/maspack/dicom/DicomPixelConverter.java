package maspack.dicom;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.properties.CompositeProperty;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyInfo;
import maspack.properties.PropertyList;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.PropertyChangeListener;

public abstract class DicomPixelConverter implements CompositeProperty {

   static Class<?>[] mySubClasses =
      new Class<?>[] { DicomWindowPixelConverter.class };

   public static Class<?>[] getSubClasses() {
      return mySubClasses;
   }

   PropertyInfo myPropInfo;
   HasProperties myPropHost;

   public PropertyInfo getPropertyInfo() {
      return myPropInfo;
   }

   public void setPropertyInfo(PropertyInfo info) {
      myPropInfo = info;
   }

   public HasProperties getPropertyHost() {
      return myPropHost;
   }

   public void setPropertyHost(HasProperties newParent) {
      myPropHost = newParent;
   }

   public static PropertyList myProps = new PropertyList(
      DicomPixelConverter.class);

   public Property getProperty(String name) {
      return PropertyList.getProperty(name, this);
   }

   public boolean hasProperty(String name) {
      return getAllPropertyInfo().get(name) != null;
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   // abstract methods
   public DicomPixelConverter clone() {
      DicomPixelConverter dpc = null;
      try {
         dpc = (DicomPixelConverter)super.clone();
      } catch (CloneNotSupportedException e) {
         throw new InternalErrorException("cannot clone super in MaterialBase");
      }
      return dpc;
   }
   
   protected void notifyHostOfPropertyChange (String name) {
      if (myPropHost instanceof PropertyChangeListener) {
         ((PropertyChangeListener)myPropHost).propertyChanged (
            new PropertyChangeEvent (this, name));
      }
   }
   
   public boolean isWritable() {
      return true;
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref) 
      throws IOException {

      pw.println ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public void scan (ReaderTokenizer rtok, Object ref) 
      throws IOException {

      getAllPropertyInfo().setDefaultValues (this);
      getAllPropertyInfo().setDefaultModes (this);
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         if (!getAllPropertyInfo().scanProp (this, rtok)) {
            throw new IOException ("unexpected input: " + rtok);
         }
      }
   }

   public abstract int interpByteRGB(byte[] in, int idx, byte[] out, int odx);

   public abstract int interpByteByte(byte[] in, int idx, byte[] out, int odx);

   public abstract int interpByteShort(byte[] in, int idx, short[] out, int odx);

   public abstract int interpRGBRGB(byte[] in, int idx, byte[] out, int odx);

   public abstract int interpRGBByte(byte[] in, int idx, byte[] out, int odx);

   public abstract int interpRGBShort(byte[] in, int idx, short[] out, int odx);

   public abstract int interpShortRGB(short[] in, int idx, byte[] out, int odx);

   public abstract int interpShortByte(short[] in, int idx, byte[] out, int odx);

   public abstract int interpShortShort(
      short[] in, int idx, short[] out, int odx);

   public abstract int interp(
      DicomPixelBuffer in, int idx, DicomPixelBuffer out, int odx);

}
