// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.window;

import org.nlogo.api.AgentKindJ;
import org.nlogo.api.NetLogoListener;

import java.util.ArrayList;
import java.util.List;

public strictfp class NetLogoListenerManager
    implements
    Events.AddJobEventHandler,
    Events.InterfaceGlobalEventHandler,
    Events.BeforeLoadEventHandler,
    Events.JobRemovedEventHandler,
    Events.CompiledEventHandler {
  private final List<NetLogoListener> listeners =
      new ArrayList<NetLogoListener>();

  public void handle(Events.BeforeLoadEvent e) {
    for (NetLogoListener listener : listeners) {
      listener.modelOpened(e.modelPath());
    }
  }

  public void handle(Events.AddJobEvent e) {
    if (e.owner() instanceof ButtonWidget) {
      for (NetLogoListener listener : listeners) {
        listener.buttonPressed
            (((ButtonWidget) e.owner()).displayName());
      }
    }
  }

  public void handle(Events.InterfaceGlobalEvent e) {
    if (!e.updating()) {
      for (NetLogoListener listener : listeners) {
        if (e.widget() instanceof SliderWidget) {
          SliderWidget slider = (SliderWidget) e.widget();
          listener.sliderChanged(
            e.widget().name(),
            slider.value(),
            slider.minimum(),
            slider.increment(),
            slider.maximum(),
            e.valueChanged(),
            e.buttonReleased());
        } else if (e.widget().classDisplayName().equals("Switch")) {
          listener.switchChanged
              (e.widget().name(),
                 ((Boolean) e.widget().valueObject())
                   .booleanValue(), e.valueChanged());
        } else if (e.widget() instanceof ChooserWidget) {
          listener.chooserChanged
              (e.widget().name(),
               e.widget().valueObject(), e.valueChanged());
        } else if (e.widget() instanceof InputBoxWidget) {
          listener.inputBoxChanged
            (e.widget().name(),
             e.widget().valueObject(), e.valueChanged());
        } else {
          throw new IllegalStateException
            ("unknown widget type: " + e.widget());
        }
      }
    }
  }

  public void handle(Events.JobRemovedEvent e) {
    if (e.owner() instanceof ButtonWidget) {
      for (NetLogoListener listener : listeners) {
        listener.buttonStopped
          (((ButtonWidget) e.owner()).displayName());
      }
    }
  }

  public void handle(Events.CompiledEvent e) {
    if (e.sourceOwner() instanceof org.nlogo.api.JobOwner ) {
      char agentType = 'O';
      if (e.sourceOwner().kind() == AgentKindJ.Turtle()) {
        agentType = 'T';
      } else if (e.sourceOwner().kind() == AgentKindJ.Patch()) {
        agentType = 'P';
      } else if (e.sourceOwner().kind() == AgentKindJ.Link()) {
        agentType = 'L';
      } else if (e.sourceOwner().kind() != AgentKindJ.Observer()) {
        throw new IllegalStateException
            ("unexpected agent class: " + e.sourceOwner().kind());
      }
      for (NetLogoListener listener : listeners) {
        if(((org.nlogo.api.JobOwner) e.sourceOwner()).isCommandCenter()) {
          listener.commandEntered
            (e.sourceOwner().classDisplayName(), e.sourceOwner().innerSource(),
             agentType, e.error());
        }
      }
    }
    else if(e.sourceOwner() instanceof ProceduresInterface) {
      for (NetLogoListener listener : listeners) {
        listener.codeTabCompiled
          (e.sourceOwner().innerSource(), e.error());
      }
    }
  }

  public void tickCounterChanged(double ticks) {
    for (NetLogoListener listener : listeners) {
      listener.tickCounterChanged(ticks);
    }
  }

  public void possibleViewUpdate() {
    for (NetLogoListener listener : listeners) {
      listener.possibleViewUpdate();
    }
  }

  public void addListener(NetLogoListener listener) {
    listeners.add(listener);
  }

  public void removeListener(NetLogoListener listener) {
    listeners.remove(listener);
  }

  public void clearListeners(NetLogoListener listener) {
    listeners.clear();
  }
}
