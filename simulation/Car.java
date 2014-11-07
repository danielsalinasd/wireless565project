/***************************************************************************
 *
 *  Vehicle Class.
 *  Handles all opearations of vehicles.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 ***************************************************************************
 */

import java.lang.Math.* ;


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
    public boolean          hasSent ;
    public CarCommMessage   receivedMessage ;

    public ReceivedMessage (
      CarCommMessage        received_message,
      int                   received_count,
      boolean               sent_out
    )
    {
      receivedMessage     = received_message ;
      receivedCount       = received_count ;
      hasSent             = sent_out ;
    }
  } //  END private class ReceivedMessage

  //  Messages that have been received and not expired.

  private Vector<ReceivedMessage>   receivedMsgTbl ;
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

  Vector<AlertInfo>     alertsReceivedTbl = new Vector () ;
  int                   alertsReceivedCnt = 0 ;
  Vector<AlertReceived> carAlertsTbl      = new Vector () ;
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
   *  @param    car_id        Identifier the car is known by.
   *  @param    car_route     Route the car is following.
   *
   *************************************************************************
   */

  public Car (
    RoadReport                sim,
    int                       car_id,
    Route                     car_route
  )
  {
    simulation            = sim ;
    carId                 = car_id ;
    path                  = car_route ;

    messageSeq            = 0 ;

    creationTime          = simulation.getCurrentTime () ;

    //  Log this car's location at the specified interval.  It is the
    //  only timer initialy set.

    locationSendTime      = creationTime +
                            LOCATION_SEND_INTERVAL *
                              (simulation.randomGen.nextDouble () *
                               LOCATION_SEND_INTERVAL_ADJ +
                               (1.0 - LOCATION_SEND_INTERVAL_ADJ)) ;

    simulation.timerUpdate (locationSendTime) ;

  } // END public Car


  /*************************************************************************
   *
   *  Update the car for the current time.
   *  Update all car components that are time dependent.
   *
   *************************************************************************
   */

  void public updateTime (void)
  {
    double                  nextTimer ;

    //  Update the location from the route.  This will throw a
    //  RouteExpiredException if the end of the route has been reached.

    location              = path.routeTime (curTime - creationTime) ;

    nextTimer             = curTime ;

    //  Handle expired messages.

    if (receivedMsgExpire > 0.0 && receivedMsgExpire <= curTime)
    {
      expireMessages () ;
    }

    if (receivedMsgExpire > 0.0 && receivedMsgExpire < nextTimer)
    {
      nextTimer = receivedMsgExpire ;
    }

    //  Handle rebroadcast of messages.

    if (receivedMsgResend > 0.0 && receivedMsgResend <= curTime)
    {
      rebroadcastMessages () ;
    }

    if (receivedMsgResend > 0.0 && receivedMsgResend < nextTimer)
    {
      nextTimer = receivedMsgResend ;
    }

    //  Send the location periodically.

    if (locationSendTime > 0.0 && locationSendTime <= curTime)
    {
      sendLocation () ;
    }

    if (locationSendTime > 0.0 && locationSendTIme < nextTimer)
    {
      nextTimer = locationSendTime ;
    }

    //  Log the locations received periodically.

    if (logLocationTime > 0.0 && logLocationTime <= curTime)
    {
      logLocations () ;
    }

    if (logLocationTime > 0.0 && logLocationTIme < nextTimer)
    {
      nextTimer = logLocationTime ;
    }

    //  Log the alerts received periodically.

    if (logAlertTime > 0.0 && logAlertTime <= curTime)
    {
      logAlerts () ;
    }

    if (logAlertTime > 0.0 && logAlertTime < nextTimer)
    {
      nextTimer = logAlertTime ;
    }

    //  Update the next timer for the car on the simulation timer list.

    simulation.timerUpdate (nextTimer) ;

  } //  END void public updateTime (void)


  /*************************************************************************
   *
   *  Update the location for the current time.
   *  Determine the current location along a route.  If the end of the
   *  route has been reached an ExpiredRouteException is thrown.
   *
   *************************************************************************
   */

  void public updateLocation (void)
  {
    curTime               = simulator.getCurrentTime () ;

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

  void public sendCarComm (
    CarCommMessage            message
  )
  {
    simulator.carComm.sendMessage (location.latitude,
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
   *  @param    car_alert_tbl Table of flags indicating which alerts each
   *                          car has received.  Null if not used.
   *
   *************************************************************************
   */

  void public sendCarComm (
    int                       msg_type,
    int                   []  car_id_tbl,
    int                   []  msg_alert_tbl,
    int                 [][]  car_alert_tbl
  )
  {
    updateLocation () ;

    messageSeq = (messageSeq + 1) & MSG_SEQ_MASK ;

    sendCarComm (location.longitude, location.latitude,
                 new CarCommMessage (carId, messageSeq,
                                     location.longitude,
                                     location.latitude,
                                     location.speed,
                                     msg_type,
                                     cur_time,
                                     car_id_tbl,
                                     msg_alert_tbl,
                                     car_alert_tbl)) ;
  }


  /*************************************************************************
   *
   *  Rebroadcast a message to all vehicles.
   *  Rebroadcast a message that was received earlier.
   *
   *  @param    message       Message to be rebroadcast.
   *
   *************************************************************************
   */

  void public resendCarComm (
    CarCommMessage            message
  )
  {
    updateLocation () ;

    sendCarComm (location.longitude, location.latitude, message) ;
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

  void public genAlert (
    int                     alert_number
  )
  {
    int                     msg_id ;

    sendCarComm (alert_number, null, null, null) ;

    msg_id = (carId << MSG_SEQ_BITS) | messageSeq ;

    alertsReceivedTbl.setElementAt (new AlertInfo (msg_id,
                                                   alert_number,
                                                   location.longitude,
                                                   location.latitude,
                                                   curTime),
                                    alertsReceivedCnt) ;

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

  void public logLocations (void)
  {
    ReceivedMessage cur_msg ;
    Vector<int>     car_ids   = new Vector () ;
    Vector<int>     car_times = new Vector () ;
    Vector<double>  car_lon   = new Vector () ;
    Vector<double>  car_lat   = new Vector () ;
    Vector<double>  car_speed = new Vector () ;

    int             car_cnt ;
    ReceivedMessate cur_msg ;

    int         []  car_id_tbl ;
    double      []  car_times_tbl ;
    double      []  car_lon_tbl ;
    double      []  car_lat_tbl ;
    double      []  car_speed_tbl ;

    updateLocation () ;

    //  Build a list of all car location information.

    car_ids.setElementAt    (carId, 0) ;
    car_times.setElementAt  (curTime, 0) ;
    car_lon.setElementAt    (location.longitude, 0) ;
    car_lat.setElementAt    (location.latitude, 0) ;
    car_speed.setElementAt  (location.speed, 0) ;

    car_cnt = 1 ;

    for (int i = 0 ; i < receivedMsgCnt ; i ++)
    {
      cur_msg = receivedMsgTbl [i] ;

      if (cur_msg.receivedMessage.msgType == MT_LOCATION)
      {
        car_ids.setElementAt    ((cur_msg.receivedMessage.msgId >>
                                  MSG_SEQ_BITS),
                                 car_cnt) ;
        car_times.setElementAt  (cur_msg.receivedMessage.msgTime,
                                 car_cnt) ;
        car_lon.setElementAt    (cur_msg.receivedMessage.longitude,
                                 car_cnt) ;
        car_lat.setElementAt    (cur_msg.receivedMessage.latitude,
                                 car_cnt) ;
        car_speed.setElementAt  (cur_msg.receivedMessage.speed,
                                 car_cnt) ;

        car_cnt ++ ;
      }
    }

    //  Send a location table to the server.

    car_id_tbl    = new int     [car_cnt] ;
    car_times_tbl = new double  [car_cnt] ;
    car_lon_tbl   = new double  [car_cnt] ;
    car_lat_tbl   = new double  [car_cnt] ;
    car_speed_tbl = new double  [car_cnt] ;

    car_id_tbl    = car_ids.toArray   (car_id_tbl) ;
    car_times_tbl = car_times.toArray (car_times_tbl) ;
    car_lon_tbl   = car_lon.toArray   (car_lon_tbl) ;
    car_lat_tbl   = car_lat.toArray   (car_lat_tbl) ;
    car_speed_tbl = car_speed.toArray (car_speed_tbl) ;

    sendCellComm (MT_LOC_TBL_SENT, car_id_tbl, car_times_tbl, car_lon_tbl,
                  car_lat_tbl, car_speed_tbl, null, null, null) ;

    //  Send a location table sent message.

    sendCarComm (MT_LOC_TBL_SENT, car_id_tbl, null, null) ;

    //  Log the locations after a full location logging interval.

    logLocationInterval = LOCATION_LOG_INTERVAL_FRACT ;
    logLocationTime     = curTime + LOCATION_LOG_INTERVAL *
                                    (simulation.randomGen.nextDouble () *
                                     LOCATION_LOG_INTERVAL_ADJ +
                                     (1.0 - LOCATION_LOG_INTERVAL_ADJ)) ;
    updateTimers () ;

  } //  END void public logLocations (void)


  /*************************************************************************
   *
   *  Log all alerts received.
   *  Log all the alerts received as well as which cars have received them.
   *
   *************************************************************************
   */

  void public logAlerts (void)
  {
    int         []  car_tbl         = new int [carAlertsCnt] ;
    int         []  msg_alert_tbl   = new int [alertsReceivedCnt] ;
    boolean   [][]  car_alert_tbl   = new int [carAlertsCnt]
                                              [alertsReceivedCnt] ;

    AlertReceived   car_alert ;
    AlertInfo       cur_alert ;

    //  Build the car table and the car alert table.

    for (int car_no = 0 ; car_no < carAlertsCnt ; car_no ++)
    {
      car_alert = carAlertsTbl [car_no] ;

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
      cur_alert = alertsReceivedTbl [alert_no] ;

      msg_alert_tbl [alert_no] = cur_alert.msgId ;
      msg_alert_tp  [alert_no] = cur_alert.msgType ;
      time_tbl      [alert_no] = cur_alert.time ;
      lon_tbl       [alert_no] = cur_alert.longitude ;
      lat_tbl       [alert_no] = cur_alert.latitude ;
    }

    //  Send the alerts to the server.

    semdCellComm (MT_ALERT_TBL_SENT, car_tbl, lon_tbl, lat_tbl, null,
                  msg_alert_tbl, msg_alert_tp, car_alert_tbl) ;

    //  Send the alert table to the other cars.

    sendCarComm (MT_ALERT_TBL_SENT, car_tbl, msg_alert_tbl, car_alert_tbl) ;

    //  All alerts have been logged.  They can be deleted.

    carAlertsTbl      = new Vector<AlertReceived> () ;
    carAlertsCnt      = 0 ;
    alertsReceivedTbl = new Vector<AlertInfo> () ;
    alertsReceivedCnt = 0 ;

    //  Don't need to log any alerts until more are received.

    logAlertInterval  = ALERT_LOG_INTERVAL_FRACT ;
    logAlertTime      = 0.0 ;
    updateTimers () ;

  } //  END void public logAlerts (void)


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

  void public receiveCarMessage (
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
    double                    speed ;
    int                       loc_index ;
    int                       car_index ;
    int                       alert_no ;
    int                       car_id ;
    int                       message_id ;
    ReceivedMessage           cur_msg ;
    AlertReceived             cur_alert ;

    updateLocation () ;

    //  Determine if the message was acturally received.  (Signal was
    //  strong enough.)

    lon_adjust    = cos (location.latitude * PI / 180.0) ;

    lat_diff      = (location.latitude  - lat) * LAT2KM ;
    lon_diff      = (location.longitude - lon) * LON2KM * lon_adjust ;
    dist_sqr      = lat_diff * lat_diff + lon_diff * lon_diff ;

    if (tx_clarity * (rx_clarity / dist_sqr) < SIGNAL_STR_MIN)
    {
      return ;
    }

    //  Determine if the message has already been received.

    loc_index     = -1 ;

    for (int i = 0 ; i < receivedMsgTblCnt ; i ++)
    {
      cur_msg     = receivedMsgTbl.elementAt (i) ;

      if (message.msgId == cur_msg.receivedMessage.msgId)
      {
        cur_msg.receivedCount ++ ;
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
    //  between sender and this car and the max possible approach speed.

    speed         = location.speed + message.speed ;

    lat_diff      = (location.latitude  - message.latitude)  * LAT2KM ;
    lon_diff      = (location.longitude - message.longitude) * LON2KM *
                                                               lon_adjust ;
    dist_sqr      = lat_diff * lat_diff + lon_diff * lon_diff ;

    if (dist_sqr > Math.pow (SEPARATION_BASE + SEPARATION_TIME * speed))
    {
      return ;
    }

    //  Add the message to the message table.

    if (receivedMsgCnt == 0)
    {
      receivedMsgExpire = curTime + MSG_EXPIRE_TIME ;
      updateTimers () ;
    }

    cur_msg = new ReceivedMessage (message, 1, false) ;

    receivedMsgTbl.setElementAt (cur_msg, receivedMsgCnt) ;
    receivedMsgCnt ++ ;

    //  Handle location messages.  Replace the last one sent with this one.

    if (message.msgType == MT_LOCATION)
    {
      if (loc_index >= 0)
      {
        receivedMsgCnt -- ;
        receivedMsgTbl.setElementAt (cur_msg, loc_index) ;
        receivedMsgTbl.setElementAt (null, receivedMsgCnt) ;
      }
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
        updateTimers () ;
      }

      alertsReceivedTbl.addElement (new AlertInfo (message.msgId,
                                                   message.msgType,
                                                   message.longitude,
                                                   message.latitude,
                                                   message.msgTime)) ;
      alertsReceivedCnt ++ ;
    }

    //  Find out if this car's location has been logged to the server.

    else if (message.msgType == MT_LOC_TBL_SENT)
    {
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
          updateTimers () ;
        }
      }
    }

    //  Find out if all this car's alerts have been logged to the server.

    else if (message.msgType == MT_ALERT_TBL_SENT)
    {
      for (car_index = 0 ;
              car_index < message.carAlertTbl.length ;
           car_index ++)
      {
        if (message.carIdTbl [car_index] = carId)
        {
          //  Remove all alerts that have been logged to the server.

          alert_no = 0 ;

          while (alert_no < alertsReceivedCnt)
          {
            //  Find this alert's message in the message's alert table.

            message_id  = alertsReceivedTbl [alert_no].msgId ;

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
                    alertsReceivedTbl [alert_no] =
                              alertsReceivedTbl [alertsReceivedCnt] ;
                    alert_no -- ;
                  }

                  alertsReceivedTbl [alertsReceivedCnt] = null ;
                  break ;
                }
              }
            }

            alert_no ++ ;

          }   //  WHILE (alert_no < alertsReceivedCnt)

          break ;

        } //  IF (message.carIdTbl [car_index] = carId)
      }   //  FOR (car_index = 0 ;
    }     //  ELSE IF (message.msgType == MT_AlertTblSent)

    //  Update a car's alerts received entry.

    else if (message.msgType == MT_ALERT_RECVD)
    {
      car_id      = (message.msgId >> MSG_SEQ_BITS) ;

      //  Find the car in the car alerts table.

      car_index   = -1 ;

      for (int i = 0 ; i < carAlertsCnt ; i ++)
      {
        if (carAlertsTbl [i].carId == car_id)
        {
          car_index = i ;
          break ;
        }
      }

      if (car_index < 0)
      {
        car_index = carAlertsCnt ++ ;
      }

      //  Create a new car alert entry and set the flags in it for all
      //  known alerts it has registered.

      cur_alert     = new AlertReceived (car_id, alertsReceivedCnt) ;

      for (alert_no = 0 ; alert_no < alertsReceivedCnt ; alert_no ++)
      {
        message_id  = alertsReceivedTbl [alert_no].msgId ;

        for (int msgid_no = 0 ;
                msgid_no < message.msgAlertTbl.length ;
             msgid_no ++)
        {
          if (message.msgAlertTbl [msgid_no] == message_id)
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
        carAlertsTbl [car_index] = cur_alert ;
      }
      else
      {
        carAlertsCnt -- ;

        if (car_index < carAlertsCnt)
        {
          carAlertsTbl [car_index]   = carAlertsTbl [carAlertCnt] ;
          carAlertsTbl [carAlertCnt] = null ;
        }
        else
        {
          carAlertsTbl [car_index]   = null ;
        }
      }
    } //  ELSE IF (message.msgType == MT_AlertRecvd)

  } //  END void public receiveCarMessage


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
    updateLocation () ;

    //  Save all alerts in the alerts received table.

    if (message.msgType >= MT_ALERTS)
    {
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
          updateTimers () ;
        }

        alertsReceivedTbl.addElement (
                      new AlertInfo (message.msgAlertTbl  [i],
                                     message.msgAlertType [i],
                                     message.longitude    [i],
                                     message.latitude     [i],
                                     message.msgTime      [i])) ;
        alertsReceivedCnt ++ ;
      }
    }

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
          cur_car     = carAlertsTbl.elementAt (carAlertsCnt - 1) ;
          carAlertsTbl.setElementAt (carAlertsCnt - 1) = null ;
          carAlertsCnt -- ;

          if (car_no < carAlertsCnt)
          {
            carAlertsTbl.setElmentAt (car_no) = cur_car ;
            car_no -- ;
          }
        }
      } //  IF (cur_car.receivedTbl.length > alert_no)

      car_no ++ ;

    } //  WHILE (car_no < carAlertsCnt)
  }   //  END private void removeAlert

} //  END public class Car
