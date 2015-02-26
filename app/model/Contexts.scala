package model

import java.util.concurrent.atomic.{AtomicReference, AtomicLong}

object Contexts {

  val contextIds = new AtomicLong()

  val contexts = new AtomicReference[Map[Long, Context]](Map())

  def startSession = {
    val id = contextIds.incrementAndGet()
    val c = new Context()
    contexts.set(contexts.get + (id -> c))
    id
  }

  def apply(id: Long): Context = {
    contexts.get().apply(id)
  }
}

class Context () {
  var account: AWSAccount = null
  val template: String = ""
  var currentQuestion: Question = new ChooseAccountQuestion

}





