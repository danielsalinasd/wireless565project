/*****************************************************************************
 *
 *  VANET/Cellular Road Reporting Simulation System Defintions.
 *  Constants used for the Roud Reporting System.
 *
 *  @copyright  Copyright 2014 Emery Newlon
 *
 *****************************************************************************
 */


/*****************************************************************************
 *
 *  Road Reporting definitions.
 *  Constants used within the Road Reporting System.
 *
 *  @author     Emery Newlon
 *
 *****************************************************************************
 */

public interface RoadReportInfo
{

  //  Timing parameters (in seconds).

  static final double         LOCATION_SEND_INTERVAL        =  5.0 ;

  static final double         LOCATION_LOG_INTERVAL         = 25.0 ;
  static final double         LOCATION_LOG_INTERVAL_ADJ     =  0.1 ;
  static final double         LOCATION_LOG_INTERVAL_FRACT   =  0.5 ;
  static final double         LOCATION_LOG_INTERVAL_BACKOFF =  0.9 ;

  static final double         ALERT_LOG_INTERVAL            = 25.0 ;
  static final double         ALERT_LOG_INTERVAL_ADJ        =  0.1 ;
  static final double         ALERT_LOG_INTERVAL_FRACT      =  0.5 ;
  static final double         ALERT_LOG_INTERVAL_BACKOFF    =  0.9 ;

  static final double         MSG_RESEND_INTERVAL           = 1.0 ;
  static final double         MSG_EXPIRE_INTERVAL           = 60.0 ;

  static final double         CAR_CREATION_INTERVAL         = 3.0 ;

  static final double         ALERT_CREATION_INTERVAL       = 60.0 ;

  static final double         NEXT_UPDATE_INTERVAL          = 60.0 ;

  static final double         SIMULATION_INTERVAL           = 600.0 ;

  //  Message ID built from car ID and message sequence number.
  //  (Car ID is shifted by given number of bits and added to the
  //   message sequence number which is only the same number of bits.)

  static final int            MSG_SEQ_BITS                  = 6 ;
  static final int            MSG_SEQ_MASK                  =
                                    ((1 << MSG_SEQ_BITS) - 1) ;

  //  Message types.

  static final byte           MT_LOCATION                   =  1 ;
  static final byte           MT_LOC_TBL_SENT               =  2 ;
  static final byte           MT_ALERT_RECVD                =  3 ;
  static final byte           MT_ALERT_TBL_SENT             =  4 ;

  static final byte           MT_ALERTS                     = 10 ;
  static final byte           MT_ALERT_COUNT                = 4 ;

  static final byte           MT_ALERT_SLICK                = 10 ;
  static final byte           MT_ALERT_VISION               = 11 ;
  static final byte           MT_ALERT_BLOCKED              = 12 ;
  static final byte           MT_ALERT_SLOW                 = 13 ;

  //  Conversions from latitude and longitude in degrees to kilometers.
  //  Uses the circumference of the Earth in kilometers.
  //  Logitude separation decreases as latitude increases by the cosime of
  //  the latitude.

  static final double         LAT_CIRCUMFERENCE_KM          = 4.0007860e4 ;
  static final double         LON_CIRCUMFERENCE_KM          = 4.0075017e4 ;

  static final double         LAT2KM                        =
                                  LAT_CIRCUMFERENCE_KM / 360.0 ;
  static final double         LON2KM                        =
                                  LON_CIRCUMFERENCE_KM / 360.0 ;

  //  Radio signal control parameters.  (Distance in kilometers.)

  static final double         SIGNAL_MAX_RANGE              = 1.0 ;
  static final double         SIGNAL_STR_MIN                =
                                  1.0 / (SIGNAL_MAX_RANGE *
                                         SIGNAL_MAX_RANGE) ;

  static final double         TX_CLARITY_RANGE              = 1.0 ;
  static final double         RX_CLARITY_RANGE              = 1.0 ;

  //  Number of times a message is received before rebroadcasting it is
  //  skipped.

  static final int            MSG_RECEIVE_MAX               = 4 ;

  //  Local area definition parameters.  (Distance in kilometers, time in
  //  seconds.)

  static final double         SEPARATION_BASE               = 0.3 ;
  static final double         SEPARATION_TIME               = 60.0 ;

  //  Alert Grid parameters.

  static final double         GRID_KM                       = 0.5 ;
  static final int            GRID_ID_XMULT                 = 46340 ;

  static final byte           ALERT_MISS_LIMIT              = 3 ;


} // END public interface RoadReportInfo
