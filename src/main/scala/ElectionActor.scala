package upmc.akka.leader

import akka.actor._

abstract class NodeStatus
case class Passive () extends NodeStatus
case class Candidate () extends NodeStatus
case class Dummy () extends NodeStatus
case class Waiting () extends NodeStatus
case class Leader () extends NodeStatus

abstract class LeaderAlgoMessage
case class Initiate () extends LeaderAlgoMessage
case class ALG (list:List[Int], nodeId:Int) extends LeaderAlgoMessage
case class AVS (list:List[Int], nodeId:Int) extends LeaderAlgoMessage
case class AVSRSP (list:List[Int], nodeId:Int) extends LeaderAlgoMessage

case class StartWithNodeList (list:List[Int])

case class Init ()

class ElectionActor (val id:Int, val terminaux:List[Terminal]) extends Actor {

     val father = context.parent
     var nodesAlive:List[Int] = List(id)

     var candSucc:Int = -1
     var candPred:Int = -1
     var status:NodeStatus = new Passive ()
    var allNodes:Map[Int,ActorSelection] = Map()

     terminaux.foreach(n => {
               val remote = context.actorSelection("akka.tcp://LeaderSystem" + n.id + "@" + n.ip + ":" + n.port + "/user/Node")
               // Mise a jour de la liste des nodes
               allNodes = allNodes + (n.id -> remote)

     })
     def neigh : Int =  nodesAlive( ( (nodesAlive.indexOf(id)) +1) % nodesAlive.size )


     def receive = {

          // Initialisation
          case Start => {
            father ! Message("ElectionActor start ...")
               self ! Initiate
          }

          case StartWithNodeList (list) => {
               if (list.isEmpty) {
                    this.nodesAlive = this.nodesAlive:::List(id)
               }
               else {
                    this.nodesAlive = list
               }
               // Debut de l'algorithme d'election
               self ! Initiate
          }

          case Initiate => {
               candSucc = -1
               candPred = -1
               status = new Candidate()
               allNodes(neigh) ! ALG(nodesAlive, id)
          }

          case ALG (list, init) => {
               nodesAlive = list
               if(status.isInstanceOf[Passive]){
                    status = new Dummy()
                    allNodes(neigh) ! ALG(nodesAlive, init)
               }
               if(status.isInstanceOf[Candidate]){
                    candPred = init
                    if(id > init) {
                         if(candSucc == -1){
                              status = new Waiting()
                              allNodes(init) ! AVS(list, id )
                         }
                         else {
                              allNodes(candSucc) ! AVSRSP(list, candPred)
                              status = new Dummy()
                         }
                    }
                    if (id == init) {
                        status = new Leader()
                        father ! LeaderChanged (id)
                        allNodes.foreach(r=>{
                          if(r._1 != id ) {
                            r._2 ! Init
                            r._2 ! LeaderChanged(id)
                          }
                        }
                        )

                    }
               }
          }

          case AVS (list, j) => {
               nodesAlive = list
               if(status.isInstanceOf[Candidate]){
                         if(candPred == -1){
                              candSucc = j
                         }
                         else {
                              allNodes(j) ! AVSRSP(list, candPred)
                              status = new  Dummy()
                         }
                    }
               if (status.isInstanceOf[Waiting]){
                         candSucc = j
                    }
               }


          case AVSRSP (list, k) => {
               nodesAlive = list
               if(status.isInstanceOf[Waiting]){
                    if(id == k) {
                      status = new Leader()
                      father ! LeaderChanged (id)
                      allNodes.foreach(r=>{
                        if(r._1 != id ) {
                          r._2 ! Init
                          r._2 ! LeaderChanged(id)
                        }
                      }
                      )
                    }
                    else {
                         candPred = k
                         if(candSucc == -1) {
                              if (k < id) {
                                   status = new Waiting()
                                   allNodes(k) ! AVS(list, id)
                              }
                         }
                         else {
                                   status = new Dummy()
                                   allNodes(candSucc) ! AVSRSP(list,k)
                              }
                    }
               }
          }
          case Init => {
                        status = new Passive()
                        candSucc = -1
                        candPred = -1
          }

     }

}
