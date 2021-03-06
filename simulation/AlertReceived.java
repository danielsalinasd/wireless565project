/***************************************************************************
 *
 *  Car's information about alerts that were received.
 *  Information that a car knows about those alerts it has received.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 ***************************************************************************
 */


/***************************************************************************
 *
 *  Car's information about alerts that were received.
 *  Information that a car knows about those alerts it has received.
 *
 *  @author     Emery Newlon
 *
 ***************************************************************************
 */

class AlertReceived
{
  public final int        carId ;         // Car that received these alerts.
  public int              receivedCnt ;   // Number of alerts received.
  public boolean      []  receivedTbl ;   // True if the given alert was
                                          // received by this car.


  /*************************************************************************
   *
   *  Constructor.
   *  Create an alert information table instance.  Initially, no alerts are
   *  known to have been received by the car.
   *
   *  @param    car_id        ID of the car that this information is for.
   *  @param    alert_cnt     Number of alerts that are currently known.
   *
   *************************************************************************
   */

  public AlertReceived (
    int                     car_id,
    int                     alert_cnt
  )
  {
    carId       = car_id ;
    receivedCnt = 0 ;
    receivedTbl = new boolean [alert_cnt] ;
  }


  /*************************************************************************
   *
   *  Format the data as a string.
   *  Return a string of the alert received data formatted into text.
   *
   *  @return           Alert received contents formatted as a text string.
   *
   *************************************************************************
   */

  public String toString ()
  {
    int               i ;
    StringBuilder     result = new StringBuilder () ;

    //  Append the basic information.

    result.append (String.format ("<AlertRcvd %d %d ",
                                  carId, receivedCnt)) ;

    //  Append the Alerts Received table.

    for (i = 0 ; i < receivedTbl.length ; i ++)
    {
      result.append ((receivedTbl [i]) ? "+" : "-") ;
    }

    result.append (">") ;

    return (result.toString ()) ;
  }

} //  END AlertReceived
