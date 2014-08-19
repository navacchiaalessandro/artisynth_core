package artisynth.demos.tutorial;

import java.io.IOException;

import maspack.matrix.*;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.probes.*;
import artisynth.core.util.*;

public class SimpleMuscleWithProbes extends SimpleMuscleWithPanel
{

   public void addInputProbe() throws IOException {

      NumericInputProbe p1probe =
         new NumericInputProbe (
            mech, "particles/p1:targetPosition",
            ArtisynthPath.getSrcRelativePath (this, "simpleMuscleP1Pos.txt"));
      p1probe.setStopTime (5);
      addInputProbe (p1probe);
   }

   public void addOutputProbe() throws IOException {
      
      NumericOutputProbe mkrProbe =
         new NumericOutputProbe (
            mech, "frameMarkers/0:velocity",
            ArtisynthPath.getSrcRelativePath (this, "simpleMuscleMkrVel.txt"),
            0.01);
      mkrProbe.setDefaultDisplayRange (-4, 4);
      mkrProbe.setStopTime (10);
      addOutputProbe (mkrProbe);
   }

   public void build (String[] args) throws IOException {

      super.build (args);

      addInputProbe ();
      addOutputProbe ();

      mech.setBounds (-1, 0, -1, 1, 0, 1);
   }

}