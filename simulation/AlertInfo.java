/***************************************************************************
 *
 *  Road alert information.
 *  Information about a road alert.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 ***************************************************************************
 */


/***************************************************************************
 *
 *  Road alert information.
 *  Information about a road alert.
 *
 *  @author     Emery Newlon
 *
 ***************************************************************************
 */

public class AlertInfo
{
  public final int          msgId ;
  public final byte         msgType ;
  public final double       longitude ;
  public final double       latitude ;
  public final double       time ;


  /*************************************************************************
   *
   *  Constructor.
   *  Create an alert information instance.
   *
   *  @param    msg_id        Message ID of the alert. (car & msg seq)
   *  @param    msg_type      Type of alert.
   *  @param    lon           Longitude the alert occured at.
   *  @param    lat           Latitude the alert occured at.
   *  @param    now           Time the alert occured at.
   *
   *************************************************************************
   */

  public AlertInfo (
    int                     msg_id,
    byte                    msg_type,
    double                  lon,
    double                  lat,
    double                  now
  )
  {
    msgId     = msg_id ;
    msgType   = msg_type ;
    longitude = lon ;
    latitude  = lat ;
    time      = now ;
  }

} //  END public class AlertInfo