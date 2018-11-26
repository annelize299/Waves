package com.wavesplatform.matcher

import java.util.concurrent.ConcurrentHashMap

import akka.actor.{Actor, ActorRef, Props, SupervisorStrategy, Terminated}
import com.wavesplatform.account.Address
import com.wavesplatform.utils.ScorexLogging
import com.wavesplatform.state.EitherExt2

class AddressDirectory(directory: ConcurrentHashMap[Address, ActorRef]) extends Actor with ScorexLogging {
  import AddressDirectory._
  import context._

  override def supervisorStrategy: SupervisorStrategy = SupervisorStrategy.stoppingStrategy

  private def createAddressActor(address: Address): ActorRef =
    watch(actorOf(Props(classOf[AddressActor]), address.toString))

  override def receive: Receive = {
    case Lookup(address) =>
      sender() ! directory.computeIfAbsent(address, createAddressActor)
    case Terminated(child) =>
      val addressString = child.path.name
      val address = Address.fromString(addressString).explicitGet()
      directory.remove(address)
      log.warn(s"Address handler for $addressString terminated")
  }
}

object AddressDirectory {
  case class Lookup(address: Address)
}
