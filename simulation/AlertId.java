/***************************************************************************
 *
 *  Road alert message ID.
 *  Road alert message ID that can be used as a key for hash tables.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 ***************************************************************************
 */


/***************************************************************************
 *
 *  Road alert message ID.
 *  Road alert message ID that can be used as a key for hash tables.
 *
 *  @author     Emery Newlon
 *
 ***************************************************************************
 */

public class AlertId implements RoadReportInfo
{
  public final Integer      msgId ;
  public final Double       time ;


  /*************************************************************************
   *
   *  Constructor.
   *  Create an alert ID instance.
   *
   *  @param    msg_id        Message ID of the alert. (car & msg seq)
   *  @param    now           Time the alert occured at.
   *
   *************************************************************************
   */

  public AlertId (
    int                     msg_id,
    double                  now
  )
  {
    msgId     = new Integer (msg_id) ;
    time      = new Double  (now) ;
  }


  /*************************************************************************
   *
   *  Compare two message IDs.
   *  Perform the equals comparison on two message IDs.
   *
   *  @param    alert_id    Alert ID to compare to this one.
   *  @return               True if they are equal, false if not.
   *
   *************************************************************************
   */

  public boolean equals (
    AlertId               alert_id
  )
  {
    return (msgId.equals (alert_id.msgId) &&
            time.equals  (alert_id.time)) ;
  }


  /*************************************************************************
   *
   *  Generate the hash code for this alert ID.
   *  Generate a hash code for this alert ID.
   *
   *  @return               Alert ID's hash code.
   *
   *************************************************************************
   */

  public int hashCode ()
  {
    return (msgId.hashCode () ^ time.hashCode ()) ;
  }

} //  END public class AlertInfo