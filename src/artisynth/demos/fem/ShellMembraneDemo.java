package artisynth.demos.fem;

import java.awt.Color;
import java.util.*;

import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.NeoHookeanMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.femmodels.*;
import artisynth.core.workspace.RootModel;
import maspack.geometry.Face;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Point3d;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;
import maspack.widgets.DoubleFieldSlider;
import maspack.widgets.LabeledComponentBase;

/**
 * Square patch of triangular shell elements, subjected to gravity. 
 * Some nodes will be held in-place to demonstrate shell bending forces.
 */
public class ShellMembraneDemo extends RootModel {

   protected static final double EPS = 1e-14;
   
   protected FemModel3d myFem = null;
   protected FemNode3d[] myNodes = null;
   protected MechModel myMech = null;
   protected PolygonalMesh myMesh = null;

   // Dimensions of volume model
   public double myVolX = 5;       
   public double myVolY = 5;
   public double myVolZ = 10;
   
   // Number of volume elements per X, Y and Z
   public int myNumX = 3;
   public int myNumY = 3;
   public int myNumZ = 5;
   
   // number of shell elements (in X,Y) to build on either side of the volume
   public int myShellMargin = 3;

   // Overall density of shell patch
   protected double myDensity = 100;
   
   // Generic particle velocity damping
   protected static double myParticleDamping = 0.5;
   
   // Element stiffness. 0 for water-like, 100 for aluminium-like
   protected static double myStiffnessDamping = 0.0;              
   
   // Affects bending strength
   protected static double myShellThickness = 0.5;
   
   // Affects bending and shear strength
   protected double myShellStiffness = 500000;
   protected double myPoissonsRatio = 0.33;
   protected boolean myUseMembrane = false;
   protected boolean myFreezeCorners = false;
   
   // Rendering radius of nodes
   protected double myNodeRadius = 0.1;
   
   // Dynamic nodes will be given this color.
   protected Color myNodeDynamicColor = Color.GREEN;
   
   // Non-dynamic (i.e. frozen) nodes will be given this color.
   protected Color myNodeNonDynamicColor = Color.GRAY;
   
   protected Vector3d mGravity = new Vector3d(0, 0, -9.81);

   public void build (String[] args) {
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-membrane")) {
            myUseMembrane = true;
         }
         else if (args[i].equals ("-corners")) {
            myFreezeCorners = true;
         }
         else {
            System.out.println (
               "Warning: unrecognized model argument " + args[i]);
         }
      }
      buildStructure();
      setProperties();
      setRenderProperties();
   }    
   
   FemNode3d getOrCreateNode (FemModel3d fem, double x, double y, double z) {
      Point3d pos = new Point3d (x, y, z);
      for (FemNode3d n : fem.getNodes()) {
         if (pos.epsilonEquals (n.getPosition(), EPS)) {
            return n;
         }
      }
      FemNode3d n = new FemNode3d (pos);
      fem.addNode (n);
      return n;
   }

   private boolean near (double x, double y) {
      return Math.abs(x-y) <= EPS;
   }

   private List<FemNode3d> getCornerNodes (FemModel3d fem) {
      ArrayList<FemNode3d> nodes = new ArrayList<FemNode3d>();
      double xmax = (myNumX+2*myShellMargin)*(myVolX/myNumX)/2.0;
      double ymax = (myNumY+2*myShellMargin)*(myVolY/myNumY)/2.0;
      for (FemNode3d n : fem.getNodes()) {
         Point3d p = n.getPosition();
         if ((near(p.x,xmax) && near(p.y,ymax)) ||
             (near(p.x,-xmax) && near(p.y,ymax)) ||
             (near(p.x,-xmax) && near(p.y,-ymax)) ||
             (near(p.x,xmax) && near(p.y,-ymax))) {
            nodes.add (n);
         }
      }
      return nodes;
   }

   private List<FemNode3d> getLeftNodes (FemModel3d fem) {
      ArrayList<FemNode3d> nodes = new ArrayList<FemNode3d>();
      double xmax = (myNumX+2*myShellMargin)*(myVolX/myNumX)/2.0;
      for (FemNode3d n : fem.getNodes()) {
         Point3d p = n.getPosition();
         if (near(p.x,-xmax)) {
            nodes.add (n);
         }
      }
      return nodes;
   }

   protected void buildStructure() {
      myMech = new MechModel ("mech");
      myFem = new FemModel3d();
      boolean membrane = false;

      // number and width of shell elements in X and Y
      int numX = myNumX+2*(myShellMargin);
      double widthX = (myVolX/myNumX);
      int numY = myNumY+2*(myShellMargin);
      double widthY = (myVolY/myNumY);

      for (int i=0; i<numX; i++) {
         for (int j=0; j<numY; j++) {
            double x0 = i*widthX - (numX*widthX)/2;
            double x1 = x0 + widthX;
            double y0 = j*widthY - (numY*widthY)/2;
            double y1 = y0 + widthY;
            double z = myVolZ/2;

            FemNode3d n0 = getOrCreateNode (myFem, x0, y0, z);
            FemNode3d n1 = getOrCreateNode (myFem, x1, y0, z);
            FemNode3d n2 = getOrCreateNode (myFem, x1, y1, z);
            FemNode3d n3 = getOrCreateNode (myFem, x0, y1, z);
            ShellQuadElement e =
               new ShellQuadElement (n0, n1, n2, n3, 0.01, myUseMembrane);
            myFem.addShellElement (e);
         }
      }

      // make the corner nodes non-dynamic
      if (myFreezeCorners) {
         for (FemNode3d n : getCornerNodes(myFem)) {
            n.setDynamic (false);
         }
      }
      else {
         for (FemNode3d n : getLeftNodes(myFem)) {
            n.setDynamic (false);
         }
      }

      myMech.addModel (myFem);
      addModel (myMech);
   }

   protected void setProperties() {
      LinearMaterial lmat =
         new LinearMaterial(myShellStiffness, myPoissonsRatio);
      NeoHookeanMaterial nmat =
         new NeoHookeanMaterial(myShellStiffness, myPoissonsRatio);
      myFem.setMaterial (nmat);
      myFem.setStiffnessDamping (myStiffnessDamping);
      myFem.setGravity (mGravity);
      myFem.setDensity (myDensity);
      myFem.setParticleDamping (myParticleDamping);
   }

   protected void setRenderProperties() {
      // Setup rendering options
      myFem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (myFem, new Color (0.7f, 0.7f, 0.9f));
      RenderProps.setPointColor (myFem, Color.GREEN);
      RenderProps.setShininess (
         myFem, myFem.getRenderProps().getShininess() * 10);
      RenderProps.setVisible (myFem, true);
      RenderProps.setFaceStyle (myFem, Renderer.FaceStyle.FRONT);
      RenderProps.setPointStyle (myFem.getNodes(), 
                                 Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (myFem.getNodes(), myNodeRadius);

      RenderProps.setFaceStyle (myFem, FaceStyle.FRONT_AND_BACK);
      RenderProps.setFaceColor (myFem, Color.CYAN);
      
   }
}
