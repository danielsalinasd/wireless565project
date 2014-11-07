/***************************************************************************
 *
 *  Car to server cellular communication handler.
 *  Handles message between the server and cars.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 ***************************************************************************
 */

 
/***************************************************************************
 *
 *  Car through communications.
 *  Sends messages originating from cars to the server through the
 *  cellular network.  Only one such object is used in the simulation.
 *
 *  @author     Emery Newlon
 *
 ***************************************************************************
 */

public class CellComm
{

  //  Simulator using this object.

  final RoadReport            simulation ;


  /*************************************************************************
   *
   *  Constructor.
   *  Create a car to server communication handler.
   *
   *  @param    sim           Road report simulator using this object.
   *
   *************************************************************************
   */

  public CellComm (
    RoadReport                sim
  )
  {
    simulation            = sim ;
  }


  /*************************************************************************
   *
   *  Pass a message from a car to the server.
   *  The message is passed directly to the server as if through a
   *  cellular network.
   *
   *  @param    message       Message being sent.
   *
   *************************************************************************
   */

  public void sendMessageToServer (
    CellCommMessage           message
  )
  {
    simulation.cellServer.receiveMessage (message) ;
  }


  /*************************************************************************
   *
   *  Pass a message to the given car.
   *  The message is passed to the car with the given ID.
   *
   *  @param    car_id        ID of the car to send the message to.
   *  @param    message       Message being sent.
   *  @return                 True if the message was delivered.  False
   *                          otherwise (car no longer exists).
   *
   *************************************************************************
   */

  public boolean sendMessageToCar (
    int                       car_id,
    CellCommMessage           message
  )
  {
    //  Get all the cars from the simulator's car list and find the
    //  desired car.

    for (cur_car = simulation.firstCar () ;
            cur_car != null ;
         cur_car = simulation.nextcar ())
    {
      if (cur_car.carId == car_id)
      {
        try
        {
          cur_car.receiveCellMessage (message) ;

          return (true) ;

        } catch (RouteExpiredException e)
        {
          //  Car has timed out.  This is handled elsewhere.
        }

        break ;
      }
    }

    return (false) ;

  } //  END public void sendMessage

} //  END public class CellComm
