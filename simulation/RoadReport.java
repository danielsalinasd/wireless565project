/*****************************************************************************
 *
 *  Simulate a VANET/Cellular Road Reporting system.
 *  Time driven operations.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 *****************************************************************************
 */


/*****************************************************************************
 *
 *  Road Reporting simulation driver class.
 *  Drives the simulation.
 *
 *  @author     Emery Newlon
 *
 *****************************************************************************
 */

public class RoadReport implements RoadReportInfo
{
  //  The simulation object.

  private static RoadReport     simulation ;

  //  Random number generator.

  private long                  randomSeed = 0xABCDEF987653L ;
  public Random                 randomGen ;

  //  Communication objects.

  public CarComm                carComm ;
  public CellComm               cellComm ;

  //  Table of Routes to run cars on.

  private static Vector<Route>  routeTbl = new Vector () ;
  private static int            routeCnt = 0 ;

  //  Car Table.  New cars are added to the table as they are removed.

  public Vector<Car>            carTbl = new Vector () ;
  public int                    carCnt = 0 ;

  public int                    nextCarId = 1 ;

  //  Timers and the current time.

  public double                 curTime ;

  private double                nextTimer ;

  private double                addAlertTime  = 0.0 ;
  private double                addCarTime    = 0.0 ;


  /*************************************************************************
   *
   *  Start the simulation program.
   *  Simulate road reporting for the given period of time..
   *
   *  @param      args    List of command line arguments.
   *
   *************************************************************************
   */

  public static void main (
    String              []  args
  )
  {
    //  Create the simulation object.

    simulation  = new RoadReport () ;

    //  Create a random number generator.

    simulation.randomGen = new Random (randomSeed) ;

    //  Create the communication ojects.

    simulation.carComm  = new CarComm  (simulation, TX_CLARITY_RANGE,
                                                    RX_CLARITY_RANGE) ;
    simulation.cellComm = new CellComm (simulation) ;

    //  Fill the route table.

    for (double i = 0.0 ; i < 4.0 ; i += 1.0)
    {
      for (double s = 10.0 ; s < 100.0 ; s += 20.0)
      {
        routeTbl.add (new Route (1.0 * 0.14 - 100.42, i   * 0.1 + 40.00,
                                  90.0, s, 3600 * 10.0 / s)) ;
        routeTbl.add (new Route (1.0 * 0.14 - 100.00, i   * 0.1 + 40.00,
                                 180.0, s, 3600 * 10.0 / s)) ;
        routeTbl.add (new Route (i   * 0.14 - 100.42, 1.0 * 0.1 + 40.00,
                                   0.0, s, 3600 * 10.0 / s)) ;
        routeTbl.add (new Route (i   * 0.14 - 100.42, 1.0 * 0.1 + 40.30,
                                 270.0, s, 3600 * 10.0 / s)) ;
      }
    }

    //  Start the timer handler.

    simulation.curTime   = 0.0 ;
    simulation.nextTimer = 1.0 ;

    simulation.timerHandler () ;


    System.exit (0) ;

  } // END public static void main


  /*************************************************************************
   *
   *  Handle the simulation's timers.
   *  Move the current time to the next timer's time and call all the
   *  objects' timer handling functions.  These functions are expected
   *  to determine whether it is time for them to take action themselves
   *  rather than having this handler to that.
   *
   *************************************************************************
   */

  public void timerHandler (void)
  {
    //  Update the current time to the next timer value.

    curTime   = nextTimer ;
    nextTimer = 0.0 ;

    //  Create any new cars.

    addCar () ;

    //  Create any new alerts.

    addAlert () ;

    //  Handle car timers.

    updateCars () ;
  }


  /*************************************************************************
   *
   *  Add an alert to a car.
   *  Add an alert to a car periodically.
   *
   *************************************************************************
   */

  public void addAlert (void)
  {
    int                 alert_number ;
    int                 car_index ;

    if (addAlertTime > curTime)
    {
      timerUpdate (addAlertTime) ;
      return ;
    }

    //  Choose an alert and car to send it randomly.

    alert_number  = (int) ((double) MT_ALERT_COUNT *
                           randomGen.nextDouble ()) + MT_ALERTS ;
    car_index     = (int) ((double) carCnt * randomGen.nextDouble ()) ;

    //  Send the alert.

    carTbl.elementAt (car_index).genAlert (alert_number) ;

    //  Schedule the next alert.

    addAlertTime = curTime + ALERT_CREATION_INTERVAL ;

    timerUpdate (alertAddTime) ;
  }


  /*************************************************************************
   *
   *  Add a car to the simulation.
   *  Add a car to the simulation periodically.
   *
   *************************************************************************
   */

  public void addCar (void)
  {
    Route         carRoute ;

    if (addCarTime > curTime)
    {
      timerUpdate (addCarTime) ;
      return ;
    }

    //  Choose a route from the route table.  The next route to use is
    //  chosen by adding a prime number to the last one and wrapping the
    //  results.  This should provide a reasonably unordered route usage.

    carRoute = routeTbl.elementAt (nextRoute) ;

    nextRoute = (nextRoute + 33) % routeTbl.size () ;

    //  Create a car using this route.

    carTbl.setElementAt (new Car (this, nextCarId ++, carRoute), carCnt) ;

    carCnt ++ ;

    //  Schedule the next car add.

    addCarTime = curTime + CAR_CREATION_INTERVAL ;

    timerUpdate (addCarTime) ;
  }


  /*************************************************************************
   *
   *  Update the time for all cars.
   *  Update the cars for the current time.  Cars whose routes have ended
   *  are removed.
   *
   *************************************************************************
   */

  public void updateCars (void)
  {
    int                 car_index ;
    Car                 cur_car ;

    //  Update all cars in the table.

    car_index = 0 ;

    while (car_index < carCnt)
    {
      cur_car = carTbl.elementAt (car_index) ;

      try
      {
        cur_car.updateTime () ;

        car_index ++ ;
      }
      catch (RouteExpiredException e)
      {
        //  Remove the car from the car table.

        carCnt -- ;

        if (car_index < carCnt)
        {
          carTbl.setElementAt (carTbl.elementAt (carCnt), car_index) ;
          carTbl.setElementAt (null, carCnt) ;
        }
      }
    }
  } //  END public void updateCars (void)


  /*************************************************************************
   *
   *  Update the timer value.
   *  Update the next timer value to use with a new time if it is sooner
   *  that the currently set timer value.
   *
   *  @param    timer_value   New value to update the timer with.
   *
   *************************************************************************
   */

  public void timerUpdate (
    double              timer_value
  )
  {
    if (nextTimer == 0.0 || nextTimer > timer_value)
    {
      nextTimer = timer_value ;
    }
  }

} // END public class RoadReport