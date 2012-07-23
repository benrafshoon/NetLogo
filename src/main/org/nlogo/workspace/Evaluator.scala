// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.workspace

import java.util.ArrayList
import org.nlogo.agent.ArrayAgentSet
import org.nlogo.agent.{Agent, AgentSet, Observer, Turtle, Patch, Link}
import org.nlogo.api.{AgentKind, CompilerException, JobOwner, LogoException, ReporterLogoThunk, CommandLogoThunk}
import org.nlogo.nvm.{ExclusiveJob, Activation, Context, Procedure}

class Evaluator(workspace: AbstractWorkspace) {

  @throws(classOf[CompilerException])
  def evaluateCommands(owner: JobOwner,
                       source: String,
                       agentSet: AgentSet = workspace.world.observers,
                       waitForCompletion: Boolean = true) = {
    val procedure = invokeCompiler(source, None, true, agentSet.kind)
    workspace.jobManager.addJob(
      workspace.jobManager.makeConcurrentJob(owner, agentSet, procedure),
      waitForCompletion)
  }

  @throws(classOf[CompilerException])
  def evaluateReporter(owner: JobOwner, source: String, agents: AgentSet = workspace.world.observers): Object = {
    val procedure = invokeCompiler(source, None, false, agents.kind)
    workspace.jobManager.addReporterJobAndWait(owner, agents, procedure)
  }

  @throws(classOf[CompilerException])
  def compileCommands(source: String, kind: AgentKind): Procedure =
    invokeCompiler(source, None, true, kind)

  @throws(classOf[CompilerException])
  def compileReporter(source: String) =
    invokeCompiler(source, None, false, AgentKind.Observer)

  /**
   * @return whether the code did a "stop" at the top level
   */
  def runCompiledCommands(owner: JobOwner, procedure: Procedure) = {
    val job = workspace.jobManager.makeConcurrentJob(owner, workspace.world.observers, procedure)
    workspace.jobManager.addJob(job, true)
    job.stopping
  }

  def runCompiledReporter(owner: JobOwner, procedure: Procedure) =
    workspace.jobManager.addReporterJobAndWait(owner, workspace.world.observers, procedure)

  ///

  @throws(classOf[CompilerException])
  def compileForRun(source: String, context: Context,reporter: Boolean) =
    invokeCompilerForRun(source, context.agent.kind, context.activation.procedure, reporter)

  ///

  private[workspace] def withContext(context: Context)(f: => Unit) {
    val oldContext = ProcedureRunner.context
    ProcedureRunner.context = context
    try f
    finally ProcedureRunner.context = oldContext
  }

  private object ProcedureRunner {
    var context: Context = null
    def run(p: Procedure): Boolean = {
      val oldActivation = context.activation
      val newActivation = new Activation(p, context.activation, 1)
      val oldRandom = context.job.random
      context.activation = newActivation
      context.job.random = workspace.world.mainRNG.clone
      try {
        context.runExclusiveJob(workspace.world.observers, 0)
        val stopped = workspace.completedActivations.get(newActivation) != true
        stopped
      }
      catch {
        case ex @ (_: LogoException | _: RuntimeException) =>
          // it would be nice if the pattern matcher would infer that ex is an Exception, not just a
          // Throwable, since Exception is the common supertype of LogoException and
          // RuntimeException, but it isn't that smart, so we have to cast - ST 7/1/10
          if(!Thread.currentThread.isInterrupted)
            context.runtimeError(ex.asInstanceOf[Exception])
          throw ex
      }
      finally {
        context.activation = oldActivation
        context.job.random = oldRandom
      }
    }
  }

  @throws(classOf[CompilerException])
  def makeReporterThunk(source: String, agent: Agent, owner: JobOwner): ReporterLogoThunk =
    if(source.trim.isEmpty) throw new IllegalStateException("empty reporter source")
    else new MyLogoThunk(source, agent, owner, false) with ReporterLogoThunk {
      @throws(classOf[LogoException])
      def call(): Object  = {
        // This really ought to create a job and submit it through the job manager, instead of just
        // calling the procedure directly.  This is temporary code that never got cleaned up.
        // Submitting jobs through the job manager is supposed to be the only way that NetLogo code
        // is ever run. - ST 1/8/10
        val job = new ExclusiveJob(owner, agentset, procedure, 0, null, owner.random)
        val context = new Context(job, agent, 0, null)
        try context.callReporterProcedure(new Activation(procedure, null, 0))
        catch {
          case ex @ (_: LogoException | _: RuntimeException) =>
            // it would be nice if the pattern matcher would infer that ex is an Exception, not just a
            // Throwable, since Exception is the common supertype of LogoException and
            // RuntimeException, but it isn't that smart, so we have to cast - ST 7/1/10
            if(!Thread.currentThread.isInterrupted)
              context.runtimeError(ex.asInstanceOf[Exception])
            throw ex
        }
        // this code was:
        // workspace.jobManager.callReporterProcedure(owner, agentset, procedure)
        // but i changed it so that we could have a context. this is all subject to change
        // possibly in the near future. - JC 9/22/10
      }
    }

  @throws(classOf[CompilerException])
  def makeCommandThunk(source: String, agent: Agent, owner: JobOwner): CommandLogoThunk =
    if(source.trim.isEmpty)
      new CommandLogoThunk { def call() = false }
    else new MyLogoThunk(source + "\n__thunk-did-finish", agent, owner, true) with CommandLogoThunk {
      @throws(classOf[LogoException])
      def call(): Boolean = ProcedureRunner.run(procedure)
    }

  @throws(classOf[CompilerException])
  private class MyLogoThunk(source: String, agent: Agent, owner: JobOwner, command: Boolean) {
    val agentset = new ArrayAgentSet(agent.kind, 1, false, workspace.world)
    agentset.add(agent)
    val procedure = invokeCompiler(source, Some(owner.displayName), command, agentset.kind)
    procedure.topLevel = false
  }

  ///

  @throws(classOf[CompilerException])
  def invokeCompilerForRun(source: String, kind: AgentKind,
    callingProcedure: Procedure, reporter: Boolean): Procedure = {

    val vars =
      if (callingProcedure == null) new ArrayList[String]()
      else callingProcedure.args
    val agentKindHint = Evaluator.agentKindHint(kind)

    val wrappedSource = if(reporter)
      // we put parens around what comes after "report", because we want to make
      // sure we don't let a malformed reporter like "3 die" past the compiler, since
      // "to-report foo report 3 die end" is syntactically valid but
      // "to-report foo report (3 die) end" isn't. - ST 11/12/09
      "to-report __runresult " +
        vars.toString.replace(',', ' ') + " " +
        agentKindHint + " report ( " + source + " \n) __done end"
    else
      "to __run " + vars.toString.replace(',', ' ') + " " + agentKindHint + " " + source + "\nend"
    val results = workspace.compiler.compileMoreCode(
      wrappedSource,
      Some(if(reporter) "runresult" else "run"),
      workspace.world.program, workspace.getProcedures,
      workspace.getExtensionManager)
    results.head.init(workspace)
    results.head
  }


  @throws(classOf[CompilerException])
  private def invokeCompiler(source: String, displayName: Option[String], commands: Boolean, kind: AgentKind) = {
    val wrappedSource = Evaluator.getHeader(kind, commands) + source + Evaluator.getFooter(commands)
    val results =
      workspace.compiler.compileMoreCode(wrappedSource, displayName, workspace.world.program,
        workspace.getProcedures, workspace.getExtensionManager)
    results.head.init(workspace)
    results.head
  }

  @throws(classOf[CompilerException])
  def readFromString(string: String) =
    workspace.compiler.readFromString(
      string, workspace.world, workspace.getExtensionManager)
}


object Evaluator {

  val agentKindHint = Map[AgentKind, String](
    AgentKind.Observer -> "__observercode",
    AgentKind.Turtle -> "__turtlecode",
    AgentKind.Patch -> "__patchcode",
    AgentKind.Link -> "__linkcode")

  def getHeader(kind: AgentKind, commands: Boolean) = {
    val hint = agentKindHint(kind)
    if(commands) "to __evaluator [] " + hint + " "
    else
      // we put parens around what comes after "report", because we want to make
      // sure we don't let a malformed reporter like "3 die" past the compiler, since
      // "to-report foo report 3 die end" is syntactically valid but
      // "to-report foo report (3 die) end" isn't. - ST 11/12/09
      "to-report __evaluator [] " + hint + " report ( "
  }

  def getFooter(commands: Boolean) =
    if(commands) "\n__done end" else "\n) __done end"

  def sourceOffset(kind: AgentKind, commands: Boolean): Int =
    getHeader(kind, commands).length
}
