/***************************************************************************
 *
 *  Inter-car message.
 *  Message sent from one car to others.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 ***************************************************************************
 */


/***************************************************************************
 *
 *  Car to server cell message information.
 *  Message information sent from a car to the server over cell phone.
 *
 *  @author     Emery Newlon
 *
 ***************************************************************************
 */

public class CellCommMessage implements RoadReportInfo
{

  //  Message Contents.
  //  A Message ID is made up of the car sending the message shifted by
  //  6 bits left bitwise or'ed with that car's 6 bit message sequence
  //  number.  Each car maintains its own message sequence number which
  //  is incremented with each message sent and reset to zero when it
  //  reaches 64.

  public final byte           msgType ;   //  Type of message that this is.

  public final int        []  carIds ;    //  Ids of the cars info is for.

  public final double     []  msgTime ;   //  Time (sec) message created.
  public final double     []  longitude ; //  Longitude of the car.
  public final double     []  latitude ;  //  Latitude of the car.
  public final double     []  speed ;     //  Speed of the car.

  //  Alerts that are being reported as received or table sent.

  public final int        []  msgAlertTbl ;   //  Table of message IDs.
  public final byte       []  msgAlertType ;  //  Table of alert types.
  public final boolean  [][]  carAlertTbl ;   //  Table of flags indicating
                                              //  which alerts each car has
                                              //  seen.


  /*************************************************************************
   *
   *  Constructor.
   *  Create a car to server cell phone message.
   *
   *  @param    msg_type      Type of message this is.
   *  @param    car_id        Table of car identifiers.
   *  @param    time          Time in seconds message was created at.
   *  @param    lon           Longitude of the car in degrees east.
   *  @param    lat           Latitude of the car in degrees north.
   *  @param    spd           Speed of the object in kph.
   *  @param    msg_alert_tbl Table of message IDs that carried the alerts
   *                          being reported, null otherwise.
   *  @param    msg_alert_tp  Table of alert types being reported, null
   *                          if not used.
   *  @param    car_alert_tbl Table of flags of which alerts in the message
   *                          alert table have been seen by each car.
   *                          First index is the index of the car in the car
   *                          id table.  Second is the index of the alert in
   *                          the alert table.
   *
   *************************************************************************
   */

  public CellCommMessage (
    byte                      msg_type,
    int                   []  car_ids,
    double                []  time,
    double                []  lon,
    double                []  lat,
    double                []  spd,
    int                   []  msg_alert_tbl,
    byte                  []  msg_alert_tp,
    boolean             [][]  car_alert_tbl
  )
  {
    msgType       = msg_type ;
    carIds        = car_ids ;
    msgTime       = time ;
    longitude     = lon ;
    latitude      = lat ;
    speed         = spd ;

    msgAlertTbl   = msg_alert_tbl ;
    msgAlertType  = msg_alert_tp ;
    carAlertTbl   = car_alert_tbl ;
  }


  /*************************************************************************
   *
   *  Format the data as a string.
   *  Return a string of the message data formatted into text.
   *
   *  @return           Message contents formatted as a text string.
   *
   *************************************************************************
   */

  public String toString ()
  {
    int               i ;
    StringBuilder     result = new StringBuilder () ;
    boolean       []  car_alerts ;
    int               alert_no ;

    //  Add the basic message information.

    result.append (String.format ("<CellCommMsg %d",
                                  msgType)) ;


    //  Append the Car IDs.

    result.append (" [") ;

    if (carIds != null)
    {
      for (i = 0 ; i < carIds.length ; i ++)
      {
        if (i > 0)
        {
          result.append (" ") ;
        }

        result.append (String.format ("%d", carIds [i])) ;
      }
    }

    result.append ("]") ;

    //  Append the Times.

    result.append (" [") ;

    if (msgTime != null)
    {
      for (i = 0 ; i < msgTime.length ; i ++)
      {
        if (i > 0)
        {
          result.append (" ") ;
        }

        result.append (String.format ("%g", msgTime [i])) ;
      }
    }

    result.append ("]") ;

    //  Append the Latitudes.

    result.append (" [") ;

    if (latitude != null)
    {
      for (i = 0 ; i < latitude.length ; i ++)
      {
        if (i > 0)
        {
          result.append (" ") ;
        }

        result.append (String.format ("%g", latitude [i])) ;
      }
    }

    result.append ("]") ;

     //  Append the Longitudes.

    result.append (" [") ;

    if (longitude != null)
    {
      for (i = 0 ; i < longitude.length ; i ++)
      {
        if (i > 0)
        {
          result.append (" ") ;
        }

        result.append (String.format ("%g", longitude [i])) ;
      }
    }

    result.append ("]") ;

    //  Append the Speeds.

    result.append (" [") ;

    if (speed != null)
    {
      for (i = 0 ; i < speed.length ; i ++)
      {
        if (i > 0)
        {
          result.append (" ") ;
        }

        result.append (String.format ("%g", speed [i])) ;
      }
    }

    result.append ("]") ;

    //  Append the Alerts.

    result.append (" [") ;

    if (msgAlertTbl != null)
    {
      for (i = 0 ; i < msgAlertTbl.length ; i ++)
      {
        if (i > 0)
        {
          result.append (" ") ;
        }

        result.append (String.format ("%d.%d",
                                      (msgAlertTbl [i] >> MSG_SEQ_BITS),
                                      (msgAlertTbl [i] & MSG_SEQ_MASK))) ;
      }
    }

    result.append ("]") ;

    //  Append the Alert Types.

    result.append (" [") ;

    if (msgAlertType != null)
    {
      for (i = 0 ; i < msgAlertType.length ; i ++)
      {
        if (i > 0)
        {
          result.append (" ") ;
        }

        result.append (String.format ("%d", msgAlertType [i])) ;
      }
    }

    result.append ("]") ;

   //  Append the Car Alert Table.

    result.append (" [") ;

    if (carAlertTbl != null)
    {
      for (i = 0 ; i < carAlertTbl.length ; i ++)
      {
        if (i > 0)
        {
          result.append (" ") ;
        }

        car_alerts = carAlertTbl [i] ;

        for (alert_no = 0 ; alert_no < car_alerts.length ; alert_no ++)
        {
          result.append ((car_alerts [alert_no]) ? "+" : "-") ;
        }
      }
    }

    result.append ("]>") ;

    return (result.toString ()) ;

  } //  END public String toString ()

} //  END public class CellCommMessage
