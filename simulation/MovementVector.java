/***************************************************************************
 *
 *  Movement vector.
 *  Position, direction, and speed of an object.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 ***************************************************************************
 */


/***************************************************************************
 *
 *  Location and movement information for an object at a moment in time.
 *  Provided to the object when it requests for it for a given time.
 *
 *  @author     Emery Newlon
 *
 ***************************************************************************
 */

public class MovementVector
{

  public final double         longitude ; //  West values are negative.
  public final double         latitude ;  //  South values are negative.
  public final double         bearing ;   //  N=0, E=90, S=180, W=270
  public final double         speed ;     //  kilometers per hour.


  /*************************************************************************
   *
   *  Constructor.
   *  Create a movement vector instance.
   *
   *  @param    lon           Longitude of the object in degrees east.
   *  @param    lat           Latitude of the object in degrees north.
   *  @param    dir           Compass direction of the object's movement
   *                          in degrees clockwise of north.
   *  @param    spd           Speed of the object in kph.
   *
   *************************************************************************
   */

  public MovementVector (
    double                    lon,
    double                    lat,
    double                    dir,
    double                    spd
  )
  {
    longitude = lon ;
    latitude  = lat ;
    bearing   = dir ;
    speed     = spd ;
  }


  /*************************************************************************
   *
   *  Format the data as a string.
   *  Return a string of the movement data formatted into text.
   *
   *  @return           Movement contents formatted as a text string.
   *
   *************************************************************************
   */

  public String toString ()
  {
    return (String.format ("<Movement %g %g %g %g>",
                           longitude, latitude, bearing, speed)) ;
  }

} //  END public class MovementVector
