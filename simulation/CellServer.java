/***************************************************************************
 *
 *  Cellular server for handling car communications.
 *  Server that handles car messages.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 ***************************************************************************
 */

import java.lang.* ;
import java.util.* ;


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

public class CellServer implements RoadReportInfo
{
  //  Inner class for car information.

  private class CarInfo
  {
    public int              carId ;
    public Integer          carKey ;
    public double           longitude ;
    public double           latitude ;
    public double           speed ;
    public double           time ;

    public int              gridX ;
    public int              gridY ;
    public Integer          gridId ;

    public int              gridMinX ;
    public int              gridMaxX ;
    public int              gridMinY ;
    public int              gridMaxY ;

    //  Number of times the alert has been missed being sent to this car.
    //  -1 = alert received, 0 = alert not wanted,
    //  >0 = times alert not sent.

    public byte         []  missedAlertCnt ;
    public byte             missedMaxCnt ;

    public CarInfo (
      int                   car_id
    )
    {
      carId           = car_id ;
      carKey          = new Integer (carId) ;
    }

    //  Update Location.

    public void updateLocation (
      double                lon,
      double                lat,
      double                spd,
      double                tm
    )
    {
      double                lon_adjust ;
      double                local_km ;
      double                x ;
      double                y ;

      //  Make sure that the information is not older than the last
      //  information collected.

      if (time > tm)
      {
        return ;
      }

      longitude     = lon ;
      latitude      = lat ;
      speed         = spd ;
      time          = tm ;

      //  Determine the grid the car is in.

      lon_adjust    = Math.cos (latitude * Math.PI / 180.0) ;

      x             = longitude * LON2KM * lon_adjust ;
      y             = latitude  * LAT2KM ;

      gridX         = (int) (x / GRID_KM) ;
      gridY         = (int) (y / GRID_KM) ;

      gridId        = new Integer (gridX * GRID_ID_XMULT + gridY) ;

      //  Determine the bounds of the local area for the car.  It includes
      //  one extra grid in each direction to handle the cases where the
      //  car is on one edge of a grid and an alert is just across the
      //  line in another grid.

      local_km      = SEPARATION_BASE + SEPARATION_TIME * speed ;

      gridMinX      = (int) ((x - local_km) / GRID_KM) - 1 ;
      gridMaxX      = (int) ((x + local_km) / GRID_KM) + 1 ;
      gridMinY      = (int) ((y - local_km) / GRID_KM) - 1 ;
      gridMaxY      = (int) ((y + local_km) / GRID_KM) + 1 ;

    } //  END public void updateLocation (


    //  Produce a readable string.

    public String toString ()
    {
      int               i ;
      StringBuilder     result = new StringBuilder () ;

      //  Add the basic message information.

      result.append (
            String.format ("<CarInfo %d %g %g %g %d<%d<%d %d<%d<%d %g ",
                           carId, latitude, longitude, speed,
                           gridMinY, gridY, gridMaxY,
                           gridMinX, gridX, gridMaxX, time)) ;

      //  Append the alerts seen.

      for (i = 0 ; i < missedAlertCnt.length ; i ++)
      {
        result.append ((missedAlertCnt [i] < 0)
                       ? "+"
                       : ((missedAlertCnt [i] == 0)
                          ? "-"
                          : String.format ("%d", missedAlertCnt [i]))) ;
      }

      result.append (">") ;

      return (result.toString ()) ;

    } //  END public String toString ()
  }   //  END private class CarInfo

  //  Inner class for alerts.

  private class Alert
  {
    public AlertInfo              alertInfo ;
    public AlertId                alertId ;
    public int                    gridX ;
    public int                    gridY ;

    public Alert (
      AlertInfo             alert_info
    )
    {
      double                lon_adjust ;

      alertInfo           = alert_info ;
      alertId             = new AlertId (alertInfo.msgId, alertInfo.time) ;

       //  Determine the grid the alert is in.

      lon_adjust    = Math.cos (alertInfo.latitude * Math.PI / 180.0) ;

      gridX         = (int) (alertInfo.longitude * LON2KM *
                             lon_adjust / GRID_KM) ;
      gridY         = (int) (alertInfo.latitude  * LAT2KM / GRID_KM) ;
    }

    //  Produce a readable string.

    public String toString ()
    {
      int               i ;
      StringBuilder     result = new StringBuilder () ;
      MissingAlerts     cur_missing ;

      //  Add the basic message information.

      result.append (String.format ("<AlertAt %d,%d %s>",
                                    gridY, gridX,
                                    alertInfo.toString ())) ;

       return (result.toString ()) ;
    }
  }   //  END private class Alerts

  //  Inner class for grids missing alerts.

  private class MissingAlerts
  {
    public CarInfo          sendToCar ;
    public byte             maxMissedCnt ;
    public boolean      []  gridAlertNos ;
    public HashMap<AlertId,Alert>
                            missedAlerts = new HashMap<AlertId,Alert> () ;
    public HashMap<Integer,CarInfo>
                            gridCars     = new HashMap<Integer,CarInfo> () ;

    public MissingAlerts (
      CarInfo               car,
      byte                  max_missed
    )
    {
      sendToCar     = car ;
      maxMissedCnt  = max_missed ;
      gridAlertNos  = new boolean [alertCnt] ;
    }
  }

  //  Alerts and Car Information tables.

  private Vector<Alert>       alertTbl  = new Vector<Alert> () ;
  private int                 alertCnt  = 0 ;

  private HashMap<Integer,CarInfo>
                              carTbl =
                                  new HashMap<Integer,CarInfo> () ;

  //  Simulator using this object.

  final RoadReport            simulation ;

  //  Car IDs provided to cars when they are created.

  private int                 carId = 0 ;

  //  Timer used to determine when to resend alerts to cars that want them.

  private double              alertResendTimer = 0.0 ;


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
    double                    cur_time ;
    int                       car_no ;
    int                       alert_no ;
    int                       alert_dst ;
    int                       car_id ;
    int                       alert_msgid ;
    double                    alert_time ;
    Integer                   key ;
    CarInfo                   car_info ;
    Alert                     alert_info = null ;
    int                   []  alert_indecies ;
    byte                  []  new_missed ;

    cur_time = simulation.getCurrentTime () ;

    //  Update cars in the car table.

    if (message.msgType == MT_LOC_TBL_SENT)
    {
      for (car_no = 0 ; car_no < message.carIds.length ; car_no ++)
      {
        car_id  = message.carIds [car_no] ;
        key     = new Integer (car_id) ;

        car_info = carTbl.get (key) ;

        if (car_info == null)
        {
          car_info = new CarInfo (car_id) ;
          carTbl.put (key, car_info) ;
        }

        car_info.updateLocation (message.longitude [car_no],
                                 message.latitude  [car_no],
                                 message.speed     [car_no],
                                 message.msgTime   [car_no]) ;
      }
    }

    //  Update the alerts and the cars that have received them.

    else if (message.msgType == MT_ALERT_TBL_SENT)
    {
      //  Create a translation table from alerts in the message to alerts
      //  in the master table.

      alert_indecies = new int [message.msgAlertTbl.length] ;

      //  Process all the alerts.

      for (alert_no = 0 ;
              alert_no < message.msgAlertTbl.length ;
           alert_no ++)
      {
        alert_msgid = message.msgAlertTbl [alert_no] ;
        alert_time  = message.msgTime     [alert_no] ;

        //  Find the alert in the table if it is there.  Add it if not.

        for (int i = 0 ; i < alertCnt ; i ++)
        {
          alert_info = alertTbl.elementAt (i) ;

          if (alert_info.alertInfo.msgId == alert_msgid &&
              alert_info.alertInfo.time  == alert_time)
          {
            alert_indecies [alert_no] = i ;
            break ;
          }
        }

        if (alertCnt <= 0 ||
            (alert_info.alertInfo.msgId != alert_msgid ||
             alert_info.alertInfo.time  != alert_time))
        {
          alert_info = new Alert (
                          new AlertInfo (alert_msgid,
                                         message.msgAlertType [alert_no],
                                         message.longitude    [alert_no],
                                         message.latitude     [alert_no],
                                         alert_time)) ;
          alertTbl.add (alert_info) ;
          alert_indecies [alert_no] = alertCnt ++ ;
        }
      }

      //  Update the alerts delivered in each car entry.

      for (car_no = 0 ; car_no < message.carIds.length ; car_no ++)
      {
        car_id  = message.carIds [car_no] ;
        key     = new Integer (car_id) ;

        car_info = carTbl.get (key) ;

        if (car_info != null)
        {
          for (alert_no = 0 ;
                  alert_no < message.msgAlertTbl.length ;
               alert_no ++)
          {
            alert_dst = alert_indecies [alert_no] ;

            //  Create a new missed array if the data won't fit in the
            //  current one.

            if (car_info.missedAlertCnt == null)
            {
              car_info.missedAlertCnt = new byte [alertCnt] ;
            }
            else if (alert_dst >= car_info.missedAlertCnt.length)
            {
              new_missed = new byte [alertCnt] ;
              System.arraycopy (car_info.missedAlertCnt, 0,
                                new_missed, 0,
                                car_info.missedAlertCnt.length) ;
              car_info.missedAlertCnt = new_missed ;
            }

            //  Set the alert received state if it was received by the car.

            if (message.carAlertTbl [car_no] [alert_no])
            {
              car_info.missedAlertCnt [alert_dst] = -1 ;
            }
          }
        } //  IF (car_info != null)
      }   //  FOR (car_no = 0 ; car_no < message.carIds.length ; car_no ++)

      //  Update the alert resend time if needed.

      if (alertResendTimer == 0.0 && alertCnt > 0)
      {
        alertResendTimer = simulation.getCurrentTime () +
                           ALERT_RESEND_INTERVAL ;

        simulation.timerUpdate (alertResendTimer) ;
      }
    } //  ELSE IF (message.msgType == MT_ALERT_TBL_SENT)

    //  Unrecognized message.

    else
    {
      System.out.format ("CellSvrMsgNone: %g %s\n",
                         cur_time, message.toString ()) ;
    }
  } //  END public void receiveMessage (


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
    if (! simulation.cellComm.sendMessageToCar (car_id, message))
    {
      System.out.format ("CellToCarFail: %d %s\n",
                         car_id, message.toString ()) ;
    }
  }


  /*************************************************************************
   *
   *  Send Alerts
   *  Send all the alerts needed by any car in each grid.
   *
   *************************************************************************
   */

  public void sendAlerts ()
  {
    double                    now ;
    double                    lon_adjust ;
    double                    lon_diff ;
    double                    lat_diff ;
    double                    dist_sqr ;

    HashMap<Integer,MissingAlerts>
                              missed_cars =
                                  new HashMap<Integer,MissingAlerts> () ;
    Iterator<MissingAlerts>   grid_iterator ;
    MissingAlerts             missed_grid ;
    Iterator<CarInfo>         car_iterator ;
    int                       car_no ;
    CarInfo                   cur_car ;
    byte                      missed_count ;
    byte                  []  new_alerts ;

    int                       alert_no ;
    Alert                     cur_alert ;
    Alert                     missed_alert ;
    Iterator<Alert>           alert_iterator ;
    AlertInfo                 cur_alert_info ;

    CellCommMessage           message ;
    int                   []  alert_ids ;
    byte                  []  alert_types ;
    double                []  longitudes ;
    double                []  latitudes ;
    double                []  times ;

    //  Perform the operation only if there are alerts in the table.
    //  Reschedule the operation as well.

    if (alertCnt <= 0 || alertResendTimer <= 0.0)
    {
      alertResendTimer = 0.0 ;
      return ;
    }

    now = simulation.getCurrentTime () ;

    if (now < alertResendTimer)
    {
      return ;
    }

    alertResendTimer = now + ALERT_RESEND_INTERVAL ;

    simulation.timerUpdate (alertResendTimer) ;

    //  Search for missing alerts for each car.

    car_iterator = carTbl.values ().iterator () ;

    while (car_iterator.hasNext ())
    {
      cur_car = car_iterator.next () ;

      // System.out.format ("FindAlert: %d %d,%d [%d:%d,%d:%d]",
                         // cur_car.carId, cur_car.gridY, cur_car.gridX,
                         // cur_car.gridMinY, cur_car.gridMaxY,
                         // cur_car.gridMinX, cur_car.gridMaxX) ;

      if (cur_car.missedAlertCnt == null)
      {
        // System.out.format (" no-misses\n") ;
        continue ;
      }

      cur_car.missedMaxCnt = -1 ;

      lon_adjust = Math.cos (cur_car.latitude * Math.PI / 180.0) ;

      for (alert_no = 0 ; alert_no < alertCnt ; alert_no ++)
      {
        cur_alert = alertTbl.elementAt (alert_no) ;

        // System.out.format (" %s", cur_alert.toString ()) ;

        if (alert_no >= cur_car.missedAlertCnt.length)
        {
          missed_count = 0 ;
        }
        else
        {
          missed_count = cur_car.missedAlertCnt [alert_no] ;

          if (missed_count < 0)
          {
            //  Car has already received this alert.

            // System.out.format ("+") ;
            continue ;
          }
        }

        //  Check if the alert is in the local area.

        if (missed_count == 0)
        {
          if (cur_car.gridMinX >= cur_alert.gridX ||
              cur_car.gridMaxX <= cur_alert.gridX ||
              cur_car.gridMinY >= cur_alert.gridY ||
              cur_car.gridMaxY <= cur_alert.gridY)
          {
            // System.out.format ("-") ;
            continue ;
          }

          lat_diff      = (cur_car.latitude  -
                           cur_alert.alertInfo.latitude)  * LAT2KM ;
          lon_diff      = (cur_car.longitude -
                           cur_alert.alertInfo.longitude) * LON2KM *
                                                            lon_adjust ;

          dist_sqr      = lat_diff * lat_diff + lon_diff * lon_diff ;

          if (dist_sqr > Math.pow (SEPARATION_BASE +
                                   SEPARATION_TIME * cur_car.speed, 2))
          {
            // System.out.format ("!") ;
            continue ;
          }
        }

        //  Add the car and alert to the missing alerts for the car's grid.

        // System.out.format ("&") ;

        missed_grid = missed_cars.get (cur_car.gridId) ;

        if (missed_grid == null)
        {
          missed_grid = new MissingAlerts (cur_car, (byte) -1) ;

          missed_cars.put (cur_car.gridId, missed_grid) ;
        }

        missed_grid.gridAlertNos [alert_no] = true ;

        if (missed_grid.gridCars.get (cur_car.carKey) == null)
        {
          missed_grid.gridCars.put (cur_car.carKey, cur_car) ;
        }

        missed_alert = missed_grid.missedAlerts.get (cur_alert.alertId) ;

        if (missed_alert == null)
        {
          missed_grid.missedAlerts.put (cur_alert.alertId, cur_alert) ;
        }

        if (missed_count > cur_car.missedMaxCnt)
        {
          cur_car.missedMaxCnt = missed_count ;
        }

        if (missed_grid.maxMissedCnt < cur_car.missedMaxCnt)
        {
          missed_grid.maxMissedCnt = cur_car.missedMaxCnt ;
          missed_grid.sendToCar    = cur_car ;
        }
      } //  FOR (alert_no = 0 ; alert_no < alertCnt ; alert_no ++)

      // System.out.println () ;

    }   //  WHILE (car_interator.hasNext ())

    //  For all grids that have cars missing alerts, send the alerts to one
    //  (or more) of the cars for rebroadcast.

    grid_iterator = missed_cars.values ().iterator () ;

    while (grid_iterator.hasNext ())
    {
      missed_grid = grid_iterator.next () ;

      //  Build the Cell Comm Message to send to cars for this grid.

      alert_no    = missed_grid.missedAlerts.size () ;

      alert_ids   = new int    [alert_no] ;
      alert_types = new byte   [alert_no] ;
      longitudes  = new double [alert_no] ;
      latitudes   = new double [alert_no] ;
      times       = new double [alert_no] ;

      message     = new CellCommMessage (MT_ALERT_TBL_SENT,
                                         null, times,
                                         longitudes, latitudes, null,
                                         alert_ids, alert_types, null) ;

      alert_no    = 0 ;

      alert_iterator = missed_grid.missedAlerts.values ().iterator () ;

      while (alert_iterator.hasNext ())
      {
        cur_alert_info         = alert_iterator.next ().alertInfo ;

        alert_ids   [alert_no] = cur_alert_info.msgId ;
        alert_types [alert_no] = cur_alert_info.msgType ;
        longitudes  [alert_no] = cur_alert_info.longitude ;
        latitudes   [alert_no] = cur_alert_info.latitude ;
        times       [alert_no] = cur_alert_info.time ;

        alert_no ++ ;
      }

      // System.out.format ("CellAlert: %s\n", message.toString ()) ;

      //  Send the message to the grid's car with the highest missed
      //  alerts and all cars with a missed alert count over the limit.

      car_iterator = missed_grid.gridCars.values ().iterator () ;

      while (car_iterator.hasNext ())
      {
        cur_car = car_iterator.next () ;

        //  Make sure all the alerts are represented in the car's missed
        //  alerts table.

        if (cur_car.missedAlertCnt.length < alertCnt)
        {
          new_alerts = new byte [alertCnt] ;

          System.arraycopy (cur_car.missedAlertCnt, 0, new_alerts, 0,
                            cur_car.missedAlertCnt.length) ;
          cur_car.missedAlertCnt = new_alerts ;
        }

        //  Send the message and update the missed counts appropriately.

        if (cur_car == missed_grid.sendToCar ||
            cur_car.missedMaxCnt >= ALERT_MISS_LIMIT)
        {
          // System.out.format ("CellAlertSend: %d\n", cur_car.carId) ;

          sendMessage (cur_car.carId, message) ;

          for (int i = 0 ; i < alertCnt ; i ++)
          {
            if (missed_grid.gridAlertNos [i])
            {
              cur_car.missedAlertCnt [i] = -1 ;
            }
          }
        }
        else
        {
          for (int i = 0 ; i < alertCnt ; i ++)
          {
            if (missed_grid.gridAlertNos [i] &&
                cur_car.missedAlertCnt   [i] > 0)
            {
              cur_car.missedAlertCnt [i] ++ ;
            }
          }
        }
      } //  WHILE (car_iterator.hasNext ())
    }   //  WHILE (grid_interator.hasNext ())

  } //  END private void sendAlerts ()

} //  END public class CellServer
