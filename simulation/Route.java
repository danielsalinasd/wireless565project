/***************************************************************************
 *
 *  Route of travel.
 *  Route to be followed by an object over time.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 ***************************************************************************
 */

import java.lang.math.* ;


/***************************************************************************
 *
 *  Location and movement information for an object over time.
 *  Provides this information to the object for a given point in time.
 *
 *  @author     Emery Newlon
 *
 ***************************************************************************
 */

public class Route implements RoadReportInfo
{

  //  Initial position and velocity.

  final MovementVector        atStart ;   //  Starting movement vector.
  final double                duration ;  //  Time the route is good for.

  //  Movement information in degrees per second.

  private final               lon_speed ;
  private final               lat_speed ;


  /*************************************************************************
   *
   *  Constructor.
   *  Create a route.
   *
   *  @param    lon           Longitude of the object in degrees east.
   *  @param    lat           Latitude of the object in degrees north.
   *  @param    dir           Compass direction of the object's movement
   *                          in degrees clockwise of north.
   *  @param    spd           Speed of the object in kph.
   *  @param    dur           Time the route is good for in seconds.
   *
   *************************************************************************
   */

  public Route (
    double                    lon,
    double                    lat,
    double                    dir,
    double                    spd,
    double                    dur
  )
  {
    atStart   = new MovementVector (lon, lat, dir, spd) ;
    duration  = dur ;

    //  Determine movement information in degrees per second.
    //  Distance per degree of longitude gets smaller as the latitude
    //  increases.

    lat_speed = (spd * cos (dir * PI / 180.0) / 3600.0) / LAT2KM ;
    lon_speed = (spd * sin (dir * PI / 180.0) / 3600.0) / LON2KM /
                cos (lat * PI / 180.0) ;

  } // END public Route


  /*************************************************************************
   *
   *  Determine the movement vector for the route at a given time.
   *  Using the starting location and speed, determine the location
   *  and velocity at a given time.  If the route has expired null
   *  is returned instead of a movement vector.
   *
   *  @param    seconds       Seconds from the starting time of the route.
   *  @return                 Movement vector indicating the position and
   *                          velocity on the route at the given time.
   *  @throws   RouteExpiredException Thrown when the route has expired.
   *
   *************************************************************************
   */

  MovementVector public routeTime (
    double                    seconds
  )
  {
    if (seconds < 0.0 || seconds > duration)
    {
      throw new RouteExpiredException () ;
    }
    
    return (new MovementVector (atStart.longitude + lon_speed * seconds,
                                atStart.latitude  + lat_speed * seconds,
                                atStart.bearing,    atStart.speed) ;
  }

} //  END public class Route
