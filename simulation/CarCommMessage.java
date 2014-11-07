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
 *  Inter-car message information.
 *  Message information sent from one car to others.
 *
 *  @author     Emery Newlon
 *
 ***************************************************************************
 */

public class CarCommMessage implements RoadReportInfo
{

  //  Message Contents.
  //  A Message ID is made up of the car sending the message shifted by
  //  6 bits left bitwise or'ed with that car's 6 bit message sequence
  //  number.  Each car maintains its own message sequence number which
  //  is incremented with each message sent and reset to zero when it
  //  reaches 64.

  public final int            msgId  ;    //  ID of the message by car.
  public final double         longitude ; //  Longitude of the car.
  public final double         latitude ;  //  Latitude of the car.
  public final double         speed ;     //  Speed of the car.

  public final byte           msgType ;   //  Type of message this is.
  public final double         msgTime ;   //  Time (sec) message created.
  public final int        []  carIdTbl ;  //  Table of car IDs for tbl sent.

  //  Alerts that are being reported as received or table sent.

  public final int        []  msgAlertTbl ;   //  Table of message IDs.
  public final boolean  [][]  carAlertTbl ;   //  Table of flags indicating
                                              //  which alerts each car has
                                              //  seen.


  /*************************************************************************
   *
   *  Constructor.
   *  Create an inter-car message.
   *
   *  @param    car_id        Identifier of the car.
   *  @param    msg_seq       Car's sequence number for this message.
   *  @param    lon           Longitude of the car in degrees east.
   *  @param    lat           Latitude of the car in degrees north.
   *  @param    spd           Speed of the object in kph.
   *  @param    msg_type      Type of message this is.
   *  @param    time          Time in seconds message was created at.
   *  @param    car_tbl       Table of car IDs for some messages, null
   *                          otherwise.
   *  @param    msg_alert_tbl Table of message IDs that carried the alerts
   *                          being ack'ed, null otherwise.
   *  @param    car_alert_tbl Table of flags of which alerts in the message
   *                          alert table have been seen by each car.
   *                          First index is the index of the car in the car
   *                          table.  Second is the index of the alert in
   *                          the alert table.
   *
   *************************************************************************
   */

  public CarCommMessage (
    int                       car_id,
    int                       mess_seq,
    double                    lon,
    double                    lat,
    double                    spd,
    byte                      msg_type,
    double                    time,
    int                   []  car_tbl,
    int                   []  msg_alert_tbl,
    boolean             [][]  car_alert_tbl
  )
  {
    msgId       = (car_id << MSG_SEQ_BITS) | (msg_seq & MSG_SEQ_MASK) ;
    longitude   = lon ;
    latitude    = lat ;
    speed       = spd ;

    msgType     = msg_type ;
    msgTime     = time ;
    carIdTbl    = car_tbl ;
    msgAlertTbl = msg_alert_tbl ;
    carAlertTbl = car_alert_tbl ;
  }

} //  END public class CarCommMessage