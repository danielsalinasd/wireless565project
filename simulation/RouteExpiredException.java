/***************************************************************************
 *
 *  Route Expired Exception.
 *  Used to indicate that the end of a route has been reached.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 ***************************************************************************
 */

import java.lang.RuntimeException ;


/***************************************************************************
 *
 *  Route Expired Exception.
 *  Thrown when the end of a route has been reached.
 *
 *  @author     Emery Newlon
 *
 ***************************************************************************
 */

public class RouteExpiredException extends RuntimeException
{
  public RouteExpiredException ()
  {
    super () ;
  }
}
