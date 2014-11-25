/***************************************************************************
 *
 *  Vehicle Class.
 *  Handles all opearations of vehicles.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 ***************************************************************************
 */

import java.lang.* ;
import java.util.* ;


/***************************************************************************
 *
 *  Vehicle operations.
 *  All opeartions carried out be vehicles is done here.
 *
 *  @author     Emery Newlon
 *
 ***************************************************************************
 */

public class Car implements RoadReportInfo
{
  //  Inner class for received message information.

  private class ReceivedMessage
  {
    public int              receivedCount ;
    public double           resendTime ;
    public CarCommMessage   receivedMessage ;

    public ReceivedMessage (
      CarCommMessage        received_message,
      int                   received_count,
      double                resend_time
    )
    {
      receivedMessage     = received_message ;
      receivedCount       = received_count ;
      resendTime          = resend_time ;
    }

    public String toString ()
    {
      return String.format ("<RcvMsg %d %g %s>",
                            receivedCount, resendTime,
                            receivedMessage.toString ()) ;
    }
  } //  END private class ReceivedMessage

  //  Messages that have been received and not expired.

  private Vector<ReceivedMessage>   receivedMsgTbl =
                                        new Vector<ReceivedMessage> () ;
  int                               receivedMsgCnt = 0 ;

  //  Simulator using this object.

  final RoadReport            simulation ;

  //  Route the car is following.

  final Route                 path ;

  //  Initial information.

  final double                creationTime ;
  public final int            carId ;

  //  Current information.

  double                      curTime ;
  MovementVector              location ;
  int                         messageSeq ;

  //	Alert management information.

  Vector<AlertInfo>     alertsReceivedTbl = new Vector<AlertInfo> () ;
  int                   alertsReceivedCnt = 0 ;
  Vector<AlertReceived> carAlertsTbl      = new Vector<AlertReceived> () ;
  int                   carAlertsCnt      = 0 ;

  //  Timers.

  double                    receivedMsgExpire   = 0.0 ;
  double                    receivedMsgResend   = 0.0 ;
  double                    locationSendTime ;
  double                    logLocationTime     = 0.0 ;
  double                    logAlertTime        = 0.0 ;

  double                    logLocationInterval = 0.5 ;
  double                    logAlertInterval    = 0.5 ;


  /*************************************************************************
   *
   *  Constructor.
   *  Create a car object.
   *
   *  @param    sim           Road report simulator using this object.
   *  @param    car_route     Route the car is following.
   *
   *************************************************************************
   */

  public Car (
    RoadReport                sim,
    Route                     car_route
  )
  {
    simulation            = sim ;
    path                  = car_route ;

    messageSeq            = 0 ;

    creationTime          = sim.getCurrentTime () ;
    carId                 = sim.cellServer.newCarId () ;

    //  Log this car's location at the specified interval.  It is the
    //  only timer initialy set.

    locationSendTime      = creationTime ;

    sim.timerUpdate (locationSendTime) ;

    System.out.format ("CarCreated: %g %d %s\n",
                       creationTime, carId, car_route.toString ()) ;

  } // END public Car


  /*************************************************************************
   *
   *  Update the car for the current time.
   *  Update all car components that are time dependent.
   *
   *************************************************************************
   */

  public void updateTime ()
  {
    double                  nextTimer ;

    //  Update the location from the route.  This will throw a
    //  RouteExpiredException if the end of the route has been reached.

    curTime               = simulation.getCurrentTime () ;

    location              = path.routeTime (curTime - creationTime) ;

    nextTimer             = curTime + NEXT_UPDATE_INTERVAL ;

    //  System.out.format ("Start updateTime: %d %g\n", carId, nextTimer) ;

    //  Handle expired messages.

    if (receivedMsgExpire > 0.0 && receivedMsgExpire <= curTime)
    {
      expireMessages () ;
    }

    if (receivedMsgExpire > 0.0 && receivedMsgExpire < nextTimer)
    {
      nextTimer = receivedMsgExpire ;
    }

    //  System.out.format ("After MsgExp: %d %g\n", carId, nextTimer) ;

    //  Handle rebroadcast of messages.

    if (receivedMsgResend > 0.0 && receivedMsgResend <= curTime)
    {
      rebroadcastMessages () ;
    }

    if (receivedMsgResend > 0.0 && receivedMsgResend < nextTimer)
    {
      nextTimer = receivedMsgResend ;
    }

    //  System.out.format ("After MsgResend: %d %g\n", carId, nextTimer) ;

    //  Send the location periodically.

    if (locationSendTime > 0.0 && locationSendTime <= curTime)
    {
      sendLocation () ;
    }

    if (locationSendTime > 0.0 && locationSendTime < nextTimer)
    {
      nextTimer = locationSendTime ;
    }

    //  System.out.format ("After SendLoc: %d %g\n", carId, nextTimer) ;

    //  Log the locations received periodically.

    if (logLocationTime > 0.0 && logLocationTime <= curTime)
    {
      logLocations () ;
    }

    if (logLocationTime > 0.0 && logLocationTime < nextTimer)
    {
      nextTimer = logLocationTime ;
    }

    //  System.out.format ("After LogLoc: %d %g\n", carId, nextTimer) ;

    //  Log the alerts received periodically.

    if (logAlertTime > 0.0 && logAlertTime <= curTime)
    {
      logAlerts () ;
    }

    if (logAlertTime > 0.0 && logAlertTime < nextTimer)
    {
      nextTimer = logAlertTime ;
    }

    //  System.out.format ("After MsgAlerts: %d %g\n", carId, nextTimer) ;

    //  Update the next timer for the car on the simulation timer list.

    simulation.timerUpdate (nextTimer) ;

  } //  END public void updateTime ()


  /*************************************************************************
   *
   *  Remove expired messages.
   *  Remove all messages in the received message table that have passed
   *  their expiration times.
   *
   *************************************************************************
   */

  private void expireMessages ()
  {
    int             message_no ;
    double          cur_msg_time ;
    double          oldest_time ;
    ReceivedMessage message ;

    message_no  = 0 ;
    oldest_time = curTime + MSG_EXPIRE_INTERVAL ;

    while (message_no < receivedMsgCnt)
    {
      //  If the message has passed its expiration time remove it from
      //  the received message table.

      message = receivedMsgTbl.elementAt (message_no) ;

      cur_msg_time = message.receivedMessage.msgTime ;

      if (cur_msg_time + MSG_EXPIRE_INTERVAL <= curTime)
      {
        System.out.format ("ExpMsg: %g %d %s\n",
                           curTime, carId, message.toString ()) ;

        receivedMsgCnt -- ;

        if (message_no < receivedMsgCnt)
        {
          receivedMsgTbl
              .setElementAt (receivedMsgTbl.elementAt (receivedMsgCnt),
                             message_no) ;
        }

        receivedMsgTbl.removeElementAt (receivedMsgCnt) ;
        message_no -- ;
      }

      //  Determine the oldest message time left in the table.

      else if (oldest_time > cur_msg_time)
      {
        oldest_time = cur_msg_time ;
      }

      message_no ++ ;

    } //  WHILE (message_no < receivedMsgCnt)

    //  Set the next time this function needs to be run at.

    if (receivedMsgCnt > 0)
    {
      receivedMsgExpire = oldest_time + MSG_EXPIRE_INTERVAL ;
    }
    else
    {
      receivedMsgExpire = 0.0 ;
    }

  } //  END private void expireMessages ()


  /*************************************************************************
   *
   *  Rebroadcast received messages.
   *  Rebroadcast received when their resend time has arrived.  A
   *  resend time of 0 indicates that the message has already been
   *  rebroadcast.
   *
   *************************************************************************
   */

  private void rebroadcastMessages ()
  {
    double            oldest_time ;
    ReceivedMessage   cur_message ;

    oldest_time = 0.0 ;

    for (int i = 0 ; i < receivedMsgCnt ; i ++)
    {
      cur_message = receivedMsgTbl.elementAt (i) ;

      //  Resend the message if its time has arived.

      if (cur_message.resendTime > 0.0 &&
          cur_message.resendTime <= curTime)
      {
        if (cur_message.receivedCount < MSG_RECEIVE_MAX)
        {
          System.out.print ("ResendMsg: ") ;

          resendCarComm (cur_message.receivedMessage) ;
          cur_message.resendTime = 0.0 ;
        }
      }
      else
      {
        //  Find the next time a rebroadcast is needed.

        if (cur_message.resendTime > 0.0 &&
            (cur_message.resendTime <= oldest_time || oldest_time == 0.0))
        {
          oldest_time = cur_message.resendTime ;
        }
      }
    }

    //  Update the time to next resend messages.

    receivedMsgResend = oldest_time ;

  } //  END private void rebroadcastMessages ()


  /*************************************************************************
   *
   *  Send the location of this car to other local cars.
   *  Broadcast the location message for this car.
   *
   *************************************************************************
   */

  private void sendLocation ()
  {
    System.out.print ("SendLoc: ") ;

    sendCarComm (MT_LOCATION, null, null, null, null) ;

    locationSendTime = curTime + LOCATION_SEND_INTERVAL ;

    if (logLocationTime == 0.0 ||
        logLocationTime > curTime + LOCATION_LOG_INTERVAL)
    {
      logLocationTime = curTime + LOCATION_LOG_INTERVAL ;
      simulation.timerUpdate (logLocationTime) ;
    }
  }


  /*************************************************************************
   *
   *  Update the location for the current time.
   *  Determine the current location along a route.  If the end of the
   *  route has been reached an ExpiredRouteException is thrown.
   *
   *************************************************************************
   */

  private void updateLocation ()
  {
    curTime               = simulation.getCurrentTime () ;

    location              = path.routeTime (curTime - creationTime) ;
  }


  /*************************************************************************
   *
   *  Send a message to all vehicles.
   *  Send the given message to all vehicles.
   *
   *  @param    message       Message to transmit.
   *
   *************************************************************************
   */

  private void sendCarComm (
    CarCommMessage            message
  )
  {
    System.out.format ("SendCarComm: %g %d %g %g %s\n",
                       curTime, carId,
                       location.latitude, location.longitude,
                       message) ;

    simulation.carComm.sendMessage (this,
                                    location.latitude,
                                    location.longitude,
                                    message) ;
  }


  /*************************************************************************
   *
   *  Send a new message to all vehicles.
   *  Create and send a message to all vehicles.
   *
   *  @param    msg_type      Type of message to create.
   *  @param    car_id_tbl    Table of car IDs to send.  Null if not used
   *                          in this type of message.
   *  @param    msg_alert_tbl Table of message IDs for alert messages.
   *                          Null if not used in this type of message.
   *  @param    time_alert_tbl  Table of message times for alert messages.
   *  @param    car_alert_tbl Table of flags indicating which alerts each
   *                          car has received.  Null if not used.
   *
   *************************************************************************
   */

  private void sendCarComm (
    byte                      msg_type,
    int                   []  car_id_tbl,
    int                   []  msg_alert_tbl,
    double                []  time_alert_tbl,
    boolean             [][]  car_alert_tbl
  )
  {
    CarCommMessage            sent_message ;

    //  Create the new message and send it.

    updateLocation () ;

    messageSeq = (messageSeq + 1) & MSG_SEQ_MASK ;

    sent_message = new CarCommMessage (carId, messageSeq,
                                       location.longitude,
                                       location.latitude,
                                       location.speed,
                                       msg_type,
                                       curTime,
                                       car_id_tbl,
                                       msg_alert_tbl,
                                       time_alert_tbl,
                                       car_alert_tbl) ;

    sendCarComm (sent_message) ;

    //  Add the message to the received message table so it is not resent.

    receivedMsgTbl.addElement (new ReceivedMessage (sent_message, 0, 0.0)) ;
    receivedMsgCnt ++ ;

  } //  END void private sendCarComm


  /*************************************************************************
   *
   *  Rebroadcast a message to all vehicles.
   *  Rebroadcast a message that was received earlier.
   *
   *  @param    message       Message to be rebroadcast.
   *
   *************************************************************************
   */

  private void resendCarComm (
    CarCommMessage            message
  )
  {
    updateLocation () ;

    sendCarComm (message) ;
  }


  /*************************************************************************
   *
   *  Add an alert for this car.
   *  Send an alert message for this car and add it to the alerts table.
   *
   *  @param    alert_number  Number of the alert to send.
   *
   *************************************************************************
   */

  public void genAlert (
    byte                    alert_number
  )
  {
    int                     msg_id ;

    System.out.print ("GenAlert: ") ;

    if (alertsReceivedCnt == 0)
    {
      logAlertTime = curTime + ALERT_LOG_INTERVAL *
                               (logAlertInterval +
                                ALERT_LOG_INTERVAL_FRACT -
                                (simulation.randomGen.nextDouble () *
                                 ALERT_LOG_INTERVAL_ADJ)) ;
      logAlertInterval = logAlertInterval * ALERT_LOG_INTERVAL_BACKOFF ;

      simulation.timerUpdate (logAlertTime) ;
    }

    sendCarComm (alert_number, null, null, null, null) ;

    msg_id = (carId << MSG_SEQ_BITS) | messageSeq ;

    alertsReceivedTbl.addElement (new AlertInfo (msg_id,
                                                 alert_number,
                                                 location.longitude,
                                                 location.latitude,
                                                 curTime)) ;

    alertsReceivedCnt ++ ;
  }


  /*************************************************************************
   *
   *  Log the locations of all vehicles.
   *  Log the locations of all vehicles known by this car to the server and
   *  to all vehicles.
   *
   *************************************************************************
   */

  private void logLocations ()
  {
    int             car_id ;
    ReceivedMessage cur_msg ;
    Vector<Integer> car_ids   = new Vector<Integer> () ;
    Vector<Double>  car_times = new Vector<Double> () ;
    Vector<Double>  car_lon   = new Vector<Double> () ;
    Vector<Double>  car_lat   = new Vector<Double> () ;
    Vector<Double>  car_speed = new Vector<Double> () ;

    int             car_cnt ;

    int         []  car_id_tbl ;
    double      []  car_times_tbl ;
    double      []  car_lon_tbl ;
    double      []  car_lat_tbl ;
    double      []  car_speed_tbl ;

    CellCommMessage log_message ;

    updateLocation () ;

    //  Build a list of all car location information.

    car_ids.addElement    (new Integer (carId)) ;
    car_times.addElement  (new Double (curTime)) ;
    car_lon.addElement    (new Double (location.longitude)) ;
    car_lat.addElement    (new Double (location.latitude)) ;
    car_speed.addElement  (new Double (location.speed)) ;

    car_cnt = 1 ;

    for (int i = 0 ; i < receivedMsgCnt ; i ++)
    {
      cur_msg = receivedMsgTbl.elementAt (i) ;
      car_id  = (cur_msg.receivedMessage.msgId >> MSG_SEQ_BITS) ;

      if (cur_msg.receivedMessage.msgType == MT_LOCATION && car_id != carId)
      {
        car_ids.addElement    (
            new Integer (car_id)) ;
        car_times.addElement  (
            new Double (cur_msg.receivedMessage.msgTime)) ;
        car_lon.addElement    (
            new Double (cur_msg.receivedMessage.longitude)) ;
        car_lat.addElement    (
            new Double (cur_msg.receivedMessage.latitude)) ;
        car_speed.addElement  (
            new Double (cur_msg.receivedMessage.speed)) ;

        car_cnt ++ ;
      }
    }

    //  Send a location table to the server.

    car_id_tbl    = new int     [car_cnt] ;
    car_times_tbl = new double  [car_cnt] ;
    car_lon_tbl   = new double  [car_cnt] ;
    car_lat_tbl   = new double  [car_cnt] ;
    car_speed_tbl = new double  [car_cnt] ;

    for (int i = 0 ; i < car_cnt ; i ++)
    {
      car_id_tbl    [i] = car_ids.elementAt   (i).intValue    () ;
      car_times_tbl [i] = car_times.elementAt (i).doubleValue () ;
      car_lon_tbl   [i] = car_lon.elementAt   (i).doubleValue () ;
      car_lat_tbl   [i] = car_lat.elementAt   (i).doubleValue () ;
      car_speed_tbl [i] = car_speed.elementAt (i).doubleValue () ;
    }

    log_message = new CellCommMessage (MT_LOC_TBL_SENT, car_id_tbl,
                                       car_times_tbl, car_lon_tbl,
                                       car_lat_tbl, car_speed_tbl,
                                       null, null, null) ;

    System.out.format ("LogLocs: %g %d ", curTime, carId) ;
    System.out.println (log_message.toString ()) ;

    simulation.cellComm.sendMessageToServer (log_message) ;

    //  Send a location table sent message.

    System.out.print ("LogLocsSent: ") ;

    sendCarComm (MT_LOC_TBL_SENT, car_id_tbl, null, null, null) ;

    //  Log the locations after a full location logging interval.

    logLocationInterval = LOCATION_LOG_INTERVAL_FRACT ;
    logLocationTime     = curTime + LOCATION_LOG_INTERVAL *
                                    (simulation.randomGen.nextDouble () *
                                     LOCATION_LOG_INTERVAL_ADJ +
                                     (1.0 - LOCATION_LOG_INTERVAL_ADJ)) ;

  } //  END private void logLocations ()


  /*************************************************************************
   *
   *  Log all alerts received.
   *  Log all the alerts received as well as which cars have received them.
   *
   *************************************************************************
   */

  private void logAlerts ()
  {
    int         []  car_tbl         = new int     [carAlertsCnt] ;
    int         []  msg_alert_tbl   = new int     [alertsReceivedCnt] ;
    boolean   [][]  car_alert_tbl   = new boolean [carAlertsCnt]
                                                  [alertsReceivedCnt] ;
    byte        []  msg_alert_tp    = new byte    [alertsReceivedCnt] ;
    double      []  time_tbl        = new double  [alertsReceivedCnt] ;
    double      []  lon_tbl         = new double  [alertsReceivedCnt] ;
    double      []  lat_tbl         = new double  [alertsReceivedCnt] ;

    AlertReceived   car_alert ;
    AlertInfo       cur_alert ;

    CellCommMessage log_message ;

    updateLocation () ;

    //  Build the car table and the car alert table.

    for (int car_no = 0 ; car_no < carAlertsCnt ; car_no ++)
    {
      car_alert = carAlertsTbl.elementAt (car_no) ;

      car_tbl [car_no] = car_alert.carId ;

      for (int alert_no = 0 ;
              alert_no < car_alert.receivedTbl.length ;
           alert_no ++)
      {
        car_alert_tbl [car_no] [alert_no] =
                  car_alert.receivedTbl [alert_no] ;
      }
    }

    //  Build the alert table.

    for (int alert_no = 0 ; alert_no < alertsReceivedCnt ; alert_no ++)
    {
      cur_alert = alertsReceivedTbl.elementAt (alert_no) ;

      msg_alert_tbl [alert_no] = cur_alert.msgId ;
      msg_alert_tp  [alert_no] = cur_alert.msgType ;
      time_tbl      [alert_no] = cur_alert.time ;
      lon_tbl       [alert_no] = cur_alert.longitude ;
      lat_tbl       [alert_no] = cur_alert.latitude ;
    }

    //  Send the alerts to the server.

    log_message = new CellCommMessage (MT_ALERT_TBL_SENT, car_tbl, time_tbl,
                                       lon_tbl, lat_tbl, null,
                                       msg_alert_tbl, msg_alert_tp,
                                       car_alert_tbl) ;

    System.out.format ("LogAlert: %g %d %s\n",
                       curTime, carId, log_message.toString ()) ;

    simulation.cellComm.sendMessageToServer (log_message) ;

    //  Send the alert table to the other cars.

    System.out.print ("LogAlertSent: ") ;

    sendCarComm (MT_ALERT_TBL_SENT, car_tbl, msg_alert_tbl, time_tbl,
                 car_alert_tbl) ;

    //  All alerts have been logged.  They can be deleted.

    carAlertsTbl      = new Vector<AlertReceived> () ;
    carAlertsCnt      = 0 ;
    alertsReceivedTbl = new Vector<AlertInfo> () ;
    alertsReceivedCnt = 0 ;

    //  Don't need to log any alerts until more are received.

    logAlertInterval  = ALERT_LOG_INTERVAL_FRACT ;
    logAlertTime      = 0.0 ;

  } //  END private void logAlerts ()


  /*************************************************************************
   *
   *  Receive a message from another vehicle.
   *  Receive and process messages from other vehicles.  This must also
   *  reject messages the car sent itself.
   *
   *  @param    lat           Latitude of message rebroadcaster.
   *  @param    lon           Logitude of message rebroadcaster.
   *  @param    tx_clarity    Clarity of the message when sent.
   *  @param    rx_clarity    Clarity adjustment at receiver.
   *  @param    message       Message sent or rebroadcast.
   *
   *************************************************************************
   */

  public void receiveCarMessage (
    double                    lat,
    double                    lon,
    double                    tx_clarity,
    double                    rx_clarity,
    CarCommMessage            message
  )
  {
    double                    lon_adjust ;
    double                    lon_diff ;
    double                    lat_diff ;
    double                    dist_sqr ;
    double                    sig_strength ;
    double                    speed ;
    double                    resend_time ;
    int                       loc_index ;
    int                       car_index ;
    int                       alert_no ;
    int                       car_id ;
    int                       message_id ;
    double                    message_time ;
    ReceivedMessage           cur_msg ;
    AlertReceived             cur_alert ;
    AlertInfo                 alert_info ;

    updateLocation () ;

    //  Determine if the message was acturally received.  (Signal was
    //  strong enough.)

    lon_adjust    = Math.cos (location.latitude * Math.PI / 180.0) ;

    lat_diff      = (location.latitude  - lat) * LAT2KM ;
    lon_diff      = (location.longitude - lon) * LON2KM * lon_adjust ;
    dist_sqr      = lat_diff * lat_diff + lon_diff * lon_diff ;

    sig_strength  = tx_clarity * (rx_clarity / dist_sqr) ;

    if (sig_strength < SIGNAL_STR_MIN)
    {
      System.out.format ("RcvMsgWeak: %g %d %g %g %g %s\n",
                         curTime, carId, lat, lon, sig_strength,
                         message.toString ()) ;
      return ;
    }

    //  Determine if the message has already been received.

    loc_index     = -1 ;

    for (int i = 0 ; i < receivedMsgCnt ; i ++)
    {
      cur_msg     = receivedMsgTbl.elementAt (i) ;

      if (message.msgId == cur_msg.receivedMessage.msgId)
      {
        cur_msg.receivedCount ++ ;

        System.out.format ("RcvMsgAgain: %g %d %g %g %s\n",
                           curTime, carId, lat, lon, message.toString ()) ;
        return ;
      }

      //  Note the message index of the last location message sent by
      //  the car sending the message.

      else if ((message.msgId >> MSG_SEQ_BITS) ==
               (cur_msg.receivedMessage.msgId >> MSG_SEQ_BITS) &&
               message.msgType == MT_LOCATION)
      {
        loc_index = i ;
      }
    }

    //  Determine if the message is local based on separation distance
    //  between sender and this car and the max speed.

    speed         = (location.speed > message.speed)
                    ? location.speed
                    : message.speed ;

    lat_diff      = (location.latitude  - message.latitude)  * LAT2KM ;
    lon_diff      = (location.longitude - message.longitude) * LON2KM *
                                                               lon_adjust ;
    dist_sqr      = lat_diff * lat_diff + lon_diff * lon_diff ;

    if (dist_sqr > Math.pow (SEPARATION_BASE + SEPARATION_TIME * speed, 2))
    {
      System.out.format ("RcvMsgFar: %g %d %g %g %s\n",
                         curTime, carId, lat, lon, message.toString ()) ;
      return ;
    }

    //  Add the message to the message table.

    if (receivedMsgCnt == 0)
    {
      receivedMsgExpire = curTime + MSG_EXPIRE_INTERVAL ;
    }

    resend_time = curTime + MSG_RESEND_INTERVAL / (dist_sqr + 1.0) ;

    cur_msg = new ReceivedMessage (message, 1, resend_time) ;

    receivedMsgTbl.addElement (cur_msg) ;
    receivedMsgCnt ++ ;

    if (receivedMsgResend == 0.0 || receivedMsgResend > resend_time)
    {
      receivedMsgResend = resend_time ;
      simulation.timerUpdate (resend_time) ;
    }

    //  Handle location messages.  Replace the last one sent with this one.

    if (message.msgType == MT_LOCATION)
    {
      if (loc_index >= 0)
      {
        receivedMsgCnt -- ;
        receivedMsgTbl.setElementAt (cur_msg, loc_index) ;
        receivedMsgTbl.removeElementAt (receivedMsgCnt) ;
      }

      if (logLocationTime == 0.0 ||
          logLocationTime > curTime + LOCATION_LOG_INTERVAL)
      {
        logLocationTime = curTime + LOCATION_LOG_INTERVAL ;
        simulation.timerUpdate (logLocationTime) ;
      }

      System.out.format ("RcvMsgLoc: %g %d %g %g %s\n",
                         curTime, carId, lat, lon, cur_msg.toString ()) ;
    }

    //  Save all alerts in the alerts received table.

    else if (message.msgType >= MT_ALERTS)
    {
      if (alertsReceivedCnt == 0)
      {
        logAlertTime = curTime + ALERT_LOG_INTERVAL *
                                 (logAlertInterval +
                                  ALERT_LOG_INTERVAL_FRACT -
                                  (simulation.randomGen.nextDouble () *
                                   ALERT_LOG_INTERVAL_ADJ)) ;
        logAlertInterval = logAlertInterval * ALERT_LOG_INTERVAL_BACKOFF ;

        simulation.timerUpdate (logAlertTime) ;
      }

      alertsReceivedTbl.addElement (new AlertInfo (message.msgId,
                                                   message.msgType,
                                                   message.longitude,
                                                   message.latitude,
                                                   message.msgTime)) ;
      alertsReceivedCnt ++ ;

      System.out.format ("RcvMsgAlert: %g %d %g %g %s\n",
                         curTime, carId, lat, lon, cur_msg.toString ()) ;
    }

    //  Find out if this car's location has been logged to the server.

    else if (message.msgType == MT_LOC_TBL_SENT)
    {
      System.out.format ("RcvMsgLocSent: %g %d %g %g %s\n",
                         curTime, carId, lat, lon, message.toString ()) ;

      for (int i = 0 ; i < message.carIdTbl.length ; i ++)
      {
        if (message.carIdTbl [i] == carId)
        {
          logLocationTime       = curTime +
                                  LOCATION_LOG_INTERVAL *
                                  (logLocationInterval +
                                   LOCATION_LOG_INTERVAL_FRACT -
                                   simulation.randomGen.nextDouble () *
                                   LOCATION_LOG_INTERVAL_ADJ) ;
          logLocationInterval   = logLocationInterval *
                                  LOCATION_LOG_INTERVAL_BACKOFF ;

          simulation.timerUpdate (logLocationTime) ;
        }
      }
    }

    //  Find out if all this car's alerts have been logged to the server.

    else if (message.msgType == MT_ALERT_TBL_SENT)
    {
      System.out.format ("RcvMsgAlertSent: %g %d %g %g %s\n",
                         curTime, carId, lat, lon, message.toString ()) ;

      for (car_index = 0 ;
              car_index < message.carAlertTbl.length ;
           car_index ++)
      {
        if (message.carIdTbl [car_index] == carId)
        {
          //  Remove all alerts that have been logged to the server.

          alert_no = 0 ;

          while (alert_no < alertsReceivedCnt)
          {
            //  Find this alert's message in the message's alert table.

            message_id  = alertsReceivedTbl.elementAt (alert_no).msgId ;

            for (int msgid_no = 0 ;
                    msgid_no < message.msgAlertTbl.length ;
                 msgid_no ++)
            {
              if (message.msgAlertTbl [msgid_no] == message_id)
              {
                //  If the alert has been logged remove it.

                if (message.carAlertTbl [car_index] [msgid_no])
                {
                  alertsReceivedCnt -- ;

                  removeAlert (alert_no, alertsReceivedCnt) ;

                  if (alert_no < alertsReceivedCnt)
                  {
                    alertsReceivedTbl.setElementAt (
                        alertsReceivedTbl.elementAt (alertsReceivedCnt),
                        alert_no) ;
                    alert_no -- ;
                  }

                  alertsReceivedTbl.removeElement (alertsReceivedCnt) ;
                  break ;
                }
              }
            }

            alert_no ++ ;

          }   //  WHILE (alert_no < alertsReceivedCnt)

          break ;

        } //  IF (message.carIdTbl [car_index] = carId)
      }   //  FOR (car_index = 0 ;

      //  Update the next time for this car to log alerts.

      if (alertsReceivedCnt == 0)
      {
        logAlertTime = 0.0 ;
      }
      else
      {
        logAlertTime = curTime + ALERT_LOG_INTERVAL *
                                 (logAlertInterval +
                                  ALERT_LOG_INTERVAL_FRACT -
                                  (simulation.randomGen.nextDouble () *
                                   ALERT_LOG_INTERVAL_ADJ)) ;
        logAlertInterval = logAlertInterval * ALERT_LOG_INTERVAL_BACKOFF ;

        simulation.timerUpdate (logAlertTime) ;
      }
    } //  ELSE IF (message.msgType == MT_AlertTblSent)

    //  Update a car's alerts received entry.

    else if (message.msgType == MT_ALERT_RECVD)
    {
      System.out.format ("MsgRcvAlertTbl: %g %d %g %g %s\n",
                         curTime, carId, lat, lon, message.toString ()) ;

      car_id      = (message.msgId >> MSG_SEQ_BITS) ;

      //  Find the car in the car alerts table.

      car_index   = -1 ;

      for (int i = 0 ; i < carAlertsCnt ; i ++)
      {
        if (carAlertsTbl.elementAt (i).carId == car_id)
        {
          car_index = i ;
          break ;
        }
      }

      //  Create a new car alert entry and set the flags in it for all
      //  known alerts it has registered.

      cur_alert     = new AlertReceived (car_id, alertsReceivedCnt) ;

      for (alert_no = 0 ; alert_no < alertsReceivedCnt ; alert_no ++)
      {
        alert_info    = alertsReceivedTbl.elementAt (alert_no) ;
        message_id    = alert_info.msgId ;
        message_time  = alert_info.time ;

        for (int msgid_no = 0 ;
                msgid_no < message.msgAlertTbl.length ;
             msgid_no ++)
        {
          if (message.msgAlertTbl  [msgid_no] == message_id &&
              message.timeAlertTbl [msgid_no] == message_time)
          {
            cur_alert.receivedCnt ++ ;
            cur_alert.receivedTbl [alert_no] = true ;
            break ;
          }
        }
      }

      //  Save the car info.  If none of the alerts are recognized don't
      //  save the car info.

      if (cur_alert.receivedCnt > 0)
      {
        if (car_index < 0)
        {
          carAlertsTbl.addElement (cur_alert) ;
          carAlertsCnt ++ ;
        }
        else
        {
          carAlertsTbl.setElementAt (cur_alert, car_index) ;
        }
      }
      else if (car_index >= 0)
      {
        carAlertsCnt -- ;

        if (car_index < carAlertsCnt)
        {
          carAlertsTbl.setElementAt (carAlertsTbl.elementAt (carAlertsCnt),
                                     car_index) ;
        }

        carAlertsTbl.removeElementAt (carAlertsCnt) ;
      }
    } //  ELSE IF (message.msgType == MT_AlertRecvd)

    //  Unknown message.

    else
    {
      System.out.format ("MsgRcvUnkwn: %g %d %g %g %s\n",
                         curTime, carId, lat, lon, message.toString ()) ;
    }
  } //  END public void receiveCarMessage


  /*************************************************************************
   *
   *  Receive a message from the server over cell phone.
   *  Receive and process messages from the server over cell phone.
   *
   *  @param    message       Message sent.
   *
   *************************************************************************
   */

  public void receiveCellMessage (
    CellCommMessage         message
  )
  {
    int                     car_id ;
    int                     msg_seq ;
    CarCommMessage          car_message ;

    updateLocation () ;

    //  Save all alerts in the alerts received table and send them out
    //  as well.

    if (message.msgType >= MT_ALERTS)
    {
      System.out.format ("CellMsgAlerts: %g %d %s\n",
                         curTime, carId, message.toString ()) ;

      for (int i = 0 ; i < message.msgTime.length ; i ++)
      {
        if (alertsReceivedCnt == 0)
        {
          logAlertTime = curTime + ALERT_LOG_INTERVAL *
                                   (logAlertInterval +
                                    ALERT_LOG_INTERVAL_FRACT -
                                    simulation.randomGen.nextDouble () *
                                    ALERT_LOG_INTERVAL_ADJ) ;
          logAlertInterval = logAlertInterval * ALERT_LOG_INTERVAL_BACKOFF ;
        }

        alertsReceivedTbl.addElement (
                      new AlertInfo (message.msgAlertTbl  [i],
                                     message.msgAlertType [i],
                                     message.longitude    [i],
                                     message.latitude     [i],
                                     message.msgTime      [i])) ;
        alertsReceivedCnt ++ ;

        //  Send the alert to all cars.

        car_id  = message.msgAlertTbl [i] >> MSG_SEQ_BITS ;
        msg_seq = message.msgAlertTbl [i] &  MSG_SEQ_MASK ;

        car_message = new CarCommMessage (car_id, msg_seq,
                                          message.longitude    [i],
                                          message.latitude     [i],
                                          0.0,
                                          message.msgAlertType [i],
                                          message.msgTime      [i],
                                          null, null, null, null) ;
        sendCarComm (car_message) ;

      } //  FOR (int i = 0 ; i < message.msgTime.length ; i ++)
    }   //  IF (message.msgType >= MT_ALERTS)

  } //  END public void receiveCellMessage


  /*************************************************************************
   *
   *  Remove an alert from all cars.
   *  Remove an alert from the information kept for which alerts all cars
   *  have received.
   *
   *  @param    alert_no      Index of the alert in the alerts received
   *                          table.
   *  @param    alert_replace Index of the alert that is to replace the
   *                          removed alert in the alerts received table.
   *
   *************************************************************************
   */

  private void removeAlert (
    int               alert_no,
    int               alert_replace
  )
  {
    int               car_no ;
    AlertReceived     cur_car ;

    //  Go through all cars that are known to have received alerts.

    car_no          = 0 ;

    while (car_no < carAlertsCnt)
    {
      cur_car       = carAlertsTbl.elementAt (car_no) ;

      //  If the car has received this alert remove it from its received
      //  table.

      if (cur_car.receivedTbl.length > alert_no)
      {
        if (cur_car.receivedTbl [alert_no])
        {
          cur_car.receivedCnt -- ;
        }

        //  Move the last alert in the alerts table to the position this
        //  alert used to use.

        if (cur_car.receivedTbl.length > alert_replace)
        {
          cur_car.receivedTbl [alert_no] =
                      cur_car.receivedTbl [alert_replace] ;
          cur_car.receivedTbl [alert_replace] = false ;
        }
        else
        {
          cur_car.receivedTbl [alert_no] = false ;
        }

        //  Remove the car from those that have received alerts if it no
        //  longer has any alerts received.  Replace it with the last car
        //  in the list and retry this position the next time through
        //  the loop.

        if (cur_car.receivedCnt <= 0)
        {
          carAlertsCnt -- ;
          cur_car = carAlertsTbl.elementAt (carAlertsCnt) ;
          carAlertsTbl.removeElementAt (carAlertsCnt) ;

          if (car_no < carAlertsCnt)
          {
            carAlertsTbl.setElementAt (cur_car, car_no) ;
            car_no -- ;
          }
        }
      } //  IF (cur_car.receivedTbl.length > alert_no)

      car_no ++ ;

    } //  WHILE (car_no < carAlertsCnt)
  }   //  END private void removeAlert

} //  END public class Car
