/***************************************************************************
 *
 *  Inter-car communication handler.
 *  Handles radio message between cars.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 ***************************************************************************
 */

import java.util.Random ;


/***************************************************************************
 *
 *  Inter-car communications.
 *  Sends messages originating from cars to all other cars.  Only one
 *  such object is used in the simulation.
 *
 *  @author     Emery Newlon
 *
 ***************************************************************************
 */

public class CarComm
{

  //  Simulator using this object.

  final RoadReport            simulation ;

  //  Radio clarity factors.  A clarity of 1.0 is perfectly clear while
  //  0.0 is unhearable.  Clarity ranges run from 0.0 to 1.0.  This number
  //  is multiplied by a random number to produce the clarity of a given
  //  message (either when transmitted or received).  The offset is added
  //  to the result to make sure the highest value is always 1.0.

  final double                tx_clarity_range ;
  final double                tx_clarity_offset ;
  final double                rx_clarity_range ;
  final double                rx_clarity_offset ;


  /*************************************************************************
   *
   *  Constructor.
   *  Create an inter-car communication handler.
   *
   *  @param    sim           Road report simulator using this object.
   *  @param    tx_clarity    The degree of clarity degredation transmitted
   *                          messages are subject to.  (0.0 - 1.0)
   *  @param    rx_clarity    The degree of clarity degredation received
   *                          messages are subject to.  (0.0 - 1.0)
   *
   *************************************************************************
   */

  public CarComm (
    RoadReport                sim,
    double                    tx_clarity,
    double                    rx_clarity
  )
  {
    simulation            = sim ;

    //  Clarity values outside the allowed range result in perfect clarity
    //  all the time.

    if (tx_clarity < 0.0 || tx_clarity > 1.0)
    {
      tx_clarity_range    = 0.0 ;
      tx_clarity_offset   = 1.0 ;
    }
    else
    {
      tx_clarity_range    = tx_clarity ;
      tx_clarity_offset   = 1.0 - tx_clarity ;
    }

    if (rx_clarity < 0.0 || rx_clarity > 1.0)
    {
      rx_clarity_range    = 0.0 ;
      rx_clarity_offset   = 1.0 ;
    }
    else
    {
      rx_clarity_range    = rx_clarity ;
      rx_clarity_offset   = 1.0 - rx_clarity ;
    }
  } // END public CarComm


  /*************************************************************************
   *
   *  Pass a message to all cars.
   *  The message can loose clarity both on transmission and on reception.
   *  Reception clarity also depends on distance between sender and
   *  receiver.
   *
   *  @param    lat           Latitude of the sender of the message.
   *  @param    lon           Longitude of the sender of the message.
   *  @param    message       Message being sent.
   *
   *************************************************************************
   */

  public void sendMessage (
    double                    lat,
    double                    lon,
    CarCommMessage            message
  )
  {
    double                    tx_clarity ;
    double                    rx_clarity ;
    Car                       cur_car ;

    //  Determine the transmission clarity of the message when sent.

    tx_clarity = tx_clarity_range * simulation.randomGen.nextDouble () +
                 tx_clarity_offset ;

    //  Get all the cars from the simulator's car list and send them the
    //  message.

    for (cur_car = simulation.firstCar () ;
            cur_car != null ;
         cur_car = simulation.nextcar ())
    {
      //  Determine the received clarity for the message for this car.

      rx_clarity = rx_clarity_range * simulation.randomGen.nextDouble () +
                   rx_clarity_offset ;

      //  Send the message to the car.

      try
      {
        cur_car.receiveCarMessage (lat, lon, tx_clarity, rx_clarity,
                                   message) ;
        
      } catch (RouteExpiredException e)
      {
        //  Car has timed out.  This is handled elsewhere.
      }
    }
  } //  END public void sendMessage

} //  END public class CarComm
