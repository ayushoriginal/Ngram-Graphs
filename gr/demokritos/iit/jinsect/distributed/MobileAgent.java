/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
*****************************************************************/

package gr.demokritos.iit.jinsect.distributed;

import java.util.ArrayList;
import java.util.Iterator;
import jade.core.*;

import jade.domain.mobility.*;
import jade.domain.FIPANames;
import jade.content.lang.sl.SLCodec;

/**
This is an example of mobile agent. 
This class contains the resources used by the agent behaviours: the counter, 
the 
flag cntEnabled, and the list of visited locations. 
At the setup it creates a gui and adds behaviours to: get the list of
available locations from AMS, serve the incoming messages, and
to increment the counter. 
In particular, notice the usage of the two methods <code>beforeMove()</code> and
<code>afterMove()</code> to execute some application-specific tasks just before and just after
the agent migration takes effect.

Because this agent has a GUI, it extends the class GuiAgent that, in turn,
extends the class Agent. Being the GUI a different thread, the communication
between the agent and its GUI is based on event passing.

@author Giovanni Caire - CSELT S.p.A
@author ggianna 
@version $Date: 2007-03-14 $ $Revision: 5283 $
*/
public class MobileAgent extends Agent {
  transient protected ArrayList Locations;
  
  // These constants are used by the Gui to post Events to the Agent
  public static final int EXIT = 1000;
  public static final int MOVE_EVENT = 1001;
  public static final int STOP_EVENT = 1002;
  public static final int CONTINUE_EVENT = 1003;
  public static final int REFRESH_EVENT = 1004;
  public static final int CLONE_EVENT = 1005;

  public void setup() {
	  // register the SL0 content language
	  getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL0);
	  // register the mobility ontology
	  getContentManager().registerOntology(MobilityOntology.getInstance());

	  // get the list of available locations and show it in the GUI
	  addBehaviour(new GetAvailableLocationsBehaviour(this));
	}


  /**
   * This method is executed just before moving the agent to another
   * location. It is automatically called by the JADE framework.
   * It disposes the GUI and prints a bye message on the standard output.
   */
    protected void beforeMove() 
    {
        // DEBUG LINES
        synchronized (System.err) {
            System.err.println(getLocalName()+" is now migrating.");
        }
        //////////////
    }

  /**
   * This method is executed as soon as the agent arrives to the new 
   * destination.
   * It registers the communication prerequisites.
   */
   protected void afterMove() {
        // synchronized (System.err) {
            // DEBUG LINES
            // System.err.println(getLocalName()+" is just arrived to this location.");
            //////////////
        // }
       
     // Register again SL0 content language and JADE mobility ontology,
     // since they don't migrate.
     getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL0);
	 getContentManager().registerOntology(MobilityOntology.getInstance());
   }

  public void afterLoad() {
      afterClone();
  }

  public void beforeFreeze() {
      beforeMove();
  }

  public void afterThaw() {
      afterMove();
  }

  public void beforeReload() {
      beforeMove();
  }

  public void afterReload() {
      afterMove();
  }
  
  public void updateLocations(Iterator iLocationIterator) {
      // Re-init locations
      Locations = new ArrayList();
      
      while (iLocationIterator.hasNext()) {
          Locations.add(iLocationIterator.next());
      }
  }
}

