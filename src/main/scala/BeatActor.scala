package upmc.akka.leader

import akka.actor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

sealed trait BeatMessage
case class Beat (id:Int) extends BeatMessage
case class BeatLeader (id:Int) extends BeatMessage

case class BeatTick () extends Tick

case class LeaderChanged (nodeId:Int)

class BeatActor (val id:Int) extends Actor {

     val time : Int = 50
     val father = context.parent
     var leader : Int = 0 // On estime que le premier Leader est 0
     val schedule = context.system.scheduler.schedule(0 milliseconds, time milliseconds, self, BeatTick)

    def receive = {

         // Initialisation
        case Start => {
          father ! Message("BeatActor start ...")
             self ! BeatTick
             if (this.id == this.leader) {
                  father ! Message ("I am the leader")
             }
        }
        // Objectif : prevenir tous les autres nodes qu'on est en vie
        case BeatTick => {
          if(id == leader) father ! BeatLeader(id)
          else father ! Beat(id)
        }
        case LeaderChanged (nodeId) => {
          leader = nodeId
          if (this.id == this.leader) {
            father ! Message ("I am the new leader after Election")
          }
        }

    }

}
