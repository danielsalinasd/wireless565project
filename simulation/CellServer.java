/***************************************************************************
 *
 *  Cellular server for handling car communications.
 *  Server that handles car messages.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 ***************************************************************************
 */


/***************************************************************************
 *
 *  Cellular Communications Server.
 *  Receives messages originating from cars to the server through the
 *  cellular network and send other messages back.
 *  Only one such object is used in the simulation.
 *
 *  @author     Emery Newlon
 *
 ***************************************************************************
 */

public class CellServer
{

  //  Simulator using this object.

  final RoadReport            simulation ;

  //  Car IDs provided to cars when they are created.

  private int                 carId = 0 ;


  /*************************************************************************
   *
   *  Constructor.
   *  Create a cellular server car communication handler.
   *
   *  @param    sim           Road report simulator using this object.
   *
   *************************************************************************
   */

  public CellServer (
    RoadReport                sim
  )
  {
    simulation            = sim ;
  }


  /*************************************************************************
   *
   *  Return the next car ID.
   *  Return a new car ID.
   *
   *  @param    message       Message being sent.
   *  @return                 Next available car ID.
   *
   *************************************************************************
   */

  public int newCarId ()
  {
    return (++ carId) ;
  }


  /*************************************************************************
   *
   *  Receive a message from cellular communications.
   *  Messages passed from cars through the cellular network to the server.
   *
   *  @param    message       Message being sent.
   *
   *************************************************************************
   */

  public void receiveMessage (
    CellCommMessage           message
  )
  {

  }


  /*************************************************************************
   *
   *  Send a message to the given car.
   *  The message is send to the car with the given ID.
   *
   *  @param    car_id        ID of the car to send the message to.
   *  @param    message       Message being sent.
   *
   *************************************************************************
   */

  public void sendMessage (
    int                       car_id,
    CellCommMessage           message
  )
  {
    simulation.cellComm.sendMessageToCar (car_id, message) ;
  }

} //  END public class CellServer
