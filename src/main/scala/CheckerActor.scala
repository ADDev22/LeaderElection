package upmc.akka.leader

import java.util
import java.util.Date

import akka.actor._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

abstract class Tick
case class CheckerTick () extends Tick

class CheckerActor (val id:Int, val terminaux:List[Terminal], electionActor:ActorRef) extends Actor {

     var time : Int = 200
     val father = context.parent
     val schedule = context.system.scheduler.schedule(time milliseconds, time milliseconds, self, CheckerTick)

     var nodesAlive:List[Int] = List(id)
     var datesForChecking:Map[Int, Date] = Map()
     var lastDate:Date = new Date()

     var leader : Int = -1

    def receive = {

         // Initialisation
        case Start => {
              father ! Message("CheckActor start ...")
             self ! CheckerTick
        }

        // A chaque fois qu'on recoit un Beat : on met a jour la liste des nodes
        case IsAlive (nodeId) =>{
          if(nodesAlive.indexOf(nodeId) == -1) nodesAlive = nodeId::nodesAlive
          datesForChecking =  datesForChecking.updated(nodeId, new  Date())
        }

        case IsAliveLeader (nodeId) => {
          leader = nodeId
          if(nodesAlive.indexOf(nodeId) == -1) nodesAlive = nodeId::nodesAlive
          datesForChecking = datesForChecking.updated(nodeId, new  Date())
        }

        // A chaque fois qu'on recoit un CheckerTick : on verifie qui est mort ou pas
        // Objectif : lancer l'election si le leader est mort
        case CheckerTick => {
          lastDate = new Date()
          var deadNodes:List[Int] = List()
          for ((nodeId,date) <- datesForChecking) {
            if( lastDate.getTime - date.getTime > 3 * time && nodeId != id ) {
              deadNodes = nodeId::deadNodes
             }
          }
          var leaderIsDead = false
          deadNodes.foreach((e) =>{
            if(leader == e) leaderIsDead = true
            datesForChecking = datesForChecking - e
            nodesAlive = nodesAlive.filter(_ != e)
            father ! Message("The Node : " + e + " is dead")
          })
          nodesAlive = nodesAlive.sorted
          if(leaderIsDead) {
            father ! Message("The leader is dead")
            electionActor ! StartWithNodeList(nodesAlive)
          }
        }
    }


}
