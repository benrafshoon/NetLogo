// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app

import org.nlogo.swing.Implicits._
import org.nlogo.window.{EditDialogFactoryInterface, GUIWorkspace}
import org.nlogo.window.Events._
import org.nlogo.swing.RichAction
import org.nlogo.api.I18N

class Tabs(val workspace: GUIWorkspace,
           dialogFactory: EditDialogFactoryInterface) extends javax.swing.JTabbedPane
  with javax.swing.event.ChangeListener with org.nlogo.window.Event.LinkParent
  with LoadBeginEvent.Handler with RuntimeErrorEvent.Handler with CompiledEvent.Handler{

  locally{
    setOpaque(false)
    setFocusable(false)
    addChangeListener(this)
  }

  val interfaceTab = new InterfaceTab(workspace, dialogFactory)
  val proceduresTab = new MainProceduresTab(workspace)
  val reviewTab = new ReviewTab(workspace)

  var previousTab: java.awt.Component = interfaceTab
  var currentTab: java.awt.Component = interfaceTab

  def init(moreTabs: (String, java.awt.Component) *) {
    addTab(I18N.gui.get("tabs.run"), interfaceTab)
    addTab(I18N.gui.get("tabs.code"), proceduresTab)
    addTab("Review", reviewTab)
    for((name, tab) <- moreTabs)
      addTab(name, tab)
  }

  def stateChanged(e: javax.swing.event.ChangeEvent) {
    previousTab = currentTab
    currentTab = getSelectedComponent
    currentTab.requestFocus()
    new Events.SwitchedTabsEvent(previousTab, currentTab).raise(this)
  }

  override def requestFocus() { currentTab.requestFocus() }
  def handle(e: LoadBeginEvent) { setSelectedComponent(interfaceTab) }
  def handle(e: RuntimeErrorEvent) {
    if(!e.jobOwner.isInstanceOf[org.nlogo.window.MonitorWidget] &&
       e.sourceOwner == proceduresTab)
      highlightRuntimeError(proceduresTab, e)
  }

  def highlightRuntimeError(tab: ProceduresTab, e: RuntimeErrorEvent) {
    setSelectedComponent(tab)
    // the use of invokeLater here is a desperate attempt to work around the Mac bug where sometimes
    // the selection happens and sometime it doesn't - ST 8/28/04
    org.nlogo.awt.EventQueue.invokeLater( () => tab.select(e.pos, e.pos + e.length) )
  }

  val errorColor = java.awt.Color.RED

  def handle(e: CompiledEvent) {
    def clearErrors() {
      for(i <- 0 until getTabCount)
        if(getComponentAt(i).isInstanceOf[ProceduresTab])
          setForegroundAt(i, null)
    }
    def recolorTab(component: java.awt.Component, hasError: Boolean) {
      setForegroundAt(indexOfComponent(component), if(hasError) errorColor else null)
    }

    // recolor tabs
    if(e.sourceOwner.isInstanceOf[ProceduresTab]) {
      val tab = e.sourceOwner.asInstanceOf[ProceduresTab]
      if(e.error != null) setSelectedComponent(tab)
      // on null error, clear all errors, as we only get one event for all the files
      if(e.error == null) clearErrors() else recolorTab(tab, e.error != null)
      // I don't really know why this is necessary when you delete a slider (by using the menu
      // item *not* the button) which causes an error in the Code tab the focus gets lost,
      // so request the focus by a known component 7/18/07
      requestFocus()
    }
    if((e.sourceOwner.isInstanceOf[org.nlogo.window.JobWidget] &&
        !e.sourceOwner.asInstanceOf[org.nlogo.window.JobWidget].isCommandCenter
        || e.sourceOwner == null // i'm assuming this is only true when
        // we've deleted that last widget. not a great sol'n - AZS 5/16/05
      )) {
      if(e.error != null) setSelectedIndex(0)
      recolorTab(interfaceTab, e.error != null)
    }
  }

  def getIndexOfComponent(tab: ProceduresTab): Int =
    (0 until getTabCount).find(n => getComponentAt(n) == tab).get

  private def stripPath(filename: String): String =
    filename.substring(filename.lastIndexOf(System.getProperty("file.separator")) + 1, filename.length)

  /// LinkComponent stuff

  val linkComponents = new collection.mutable.ArrayBuffer[AnyRef]
  def addLinkComponent(c: AnyRef) { linkComponents += c }
  def getLinkChildren = linkComponents.toArray
}
