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

public class CellCommMessage
{

  //  Message Contents.
  //  A Message ID is made up of the car sending the message shifted by
  //  6 bits left bitwise or'ed with that car's 6 bit message sequence
  //  number.  Each car maintains its own message sequence number which
  //  is incremented with each message sent and reset to zero when it
  //  reaches 64.

  public final int            msgType ;   //  Type of message that this is.

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
    int                       msg_type,
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

} //  END public class CellCommMessage
