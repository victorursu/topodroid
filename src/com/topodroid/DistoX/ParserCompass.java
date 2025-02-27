/* @file ParserCompass.java
 *
 * @author marco corvi
 * @date nov 2014
 *
 * @brief TopoDroid Compass parser
 *
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * ----------------------------------------------------------
 */
package com.topodroid.DistoX;

// import java.io.File;
import java.io.IOException;
// import java.io.FileReader;
import java.io.BufferedReader;
// import java.util.ArrayList;
// import java.util.Stack;
import java.util.Locale;
// import java.util.regex.Pattern;

// import android.util.Log;

class ParserCompass extends ImportParser
{
  /** Compass parser
   * @param filename name of the file to parse
   * @param apply_declination whether to aapply declination correction
   */
  ParserCompass( String filename, boolean apply_declination ) throws ParserException
  {
    super( apply_declination );
    // Log.v("DistoX", "Parser Compass <" + filename + ">" );
    // mName = survey name is read from the file
    readFile( filename );
    checkValid();
  }

  private boolean isDuplicate( String flag ) { return  ( flag != null && flag.indexOf('L') >= 0 ); }

  private boolean isSurface( String flag ) { return ( flag != null && flag.indexOf('X') >= 0 ); }

  // compass has no flag for backshot, therefore this is always false
  private boolean isBackshot( String flag ) { return false; }

  /** read input file
   * @param br buffered reader on the input file
   */
  @Override
  void readFile( BufferedReader br ) throws ParserException
  {
    // lengths in meters
    // angles  in degrees
    float mLength, mBearing, mClino, mLeft, mUp, mDown, mRight;
    String mFlag=null, mComment=null, mFrom=null, mTo=null;

    String line = "";
    try {
      line = nextLine( br );
      while ( line != null ) {
        line = line.trim();
        // Log.v("DistoX", line );
        if ( line.startsWith("SURVEY NAME") ) {
          if ( mName == null ) {
            int pos = line.indexOf( ':' );
            if ( pos >= 0 ) {
              mName = line.substring( pos+2 );
            }
          }
        } else if ( line.startsWith("SURVEY DATE") ) {
          if ( mDate == null ) {
            String[] vals = splitLine(line); // line.split( "\\s+" );
            try {
              mDate = String.format(Locale.US, "%04d.%02d.%02d",
                Integer.parseInt( vals[4] ), Integer.parseInt( vals[2] ), Integer.parseInt( vals[3] ) );
            } catch ( NumberFormatException e ) { }
            if ( vals.length >= 6 ) {
              int com = line.indexOf("COMMENT:");
              mComment = line.substring( com+8 ).trim();
            }
          }
        } else if ( line.startsWith("SURVEY TEAM") ) {
          String team = nextLine( br );
          if ( mTeam.length() == 0 ) {
            mTeam = team.trim();
          }
        } else if ( line.startsWith("DECLINATION") ) {
          String[] vals = splitLine(line); // line.split( "\\s+" );
          mDeclination = Float.parseFloat( vals[1] );
        } else if ( line.length() > 8 ) {
          String[] vals = splitLine(line); // line.split( "\\s+" );
          int k = 0;
          int kmax = vals.length;
          if ( kmax >= 5 && ! vals[0].equals("FROM") ) {
            mLength = -1;
            mLeft = mRight = mUp = mDown = -1;

            mFrom = vals[k]; ++k;
            mTo   = vals[k]; ++k;
            try {
              mLength  = Float.parseFloat(vals[k]) * TDUtil.FT2M; ++k;
              mBearing = Float.parseFloat(vals[k]); ++k;
              mClino   = Float.parseFloat(vals[k]); ++k;
              if ( k < kmax ) {
                mLeft = Float.parseFloat(vals[k]) * TDUtil.FT2M; ++k;
                if ( k < kmax ) {
                  mUp = Float.parseFloat(vals[k]) * TDUtil.FT2M; ++k;
                  if ( k < kmax ) {
                    mDown = Float.parseFloat(vals[k]) * TDUtil.FT2M; ++k;
                    if ( k < kmax ) {
                      mRight = Float.parseFloat(vals[k]) * TDUtil.FT2M; ++k;
                      mFlag = null;
                      mComment = "";
                      if ( k < kmax ) {
                        if (vals[k].startsWith("#")) {
                          mFlag = vals[k]; ++k;
                          if ( k < kmax ) mComment = TDUtil.concat( vals, k );
                        } else if ( mBearing < -900 || mClino < -900 ) {
                          float bearing = TDMath.add180( Float.parseFloat(vals[k]) ); ++k; 
                          if ( mBearing < -900 ) {
                            mBearing = bearing;
                          } else if ( bearing >= 0 && bearing <= 360 ) {
                            if ( Math.abs( mBearing - bearing ) > 180 ) {
                              mBearing = TDMath.in360( ( mBearing + bearing + 360 ) / 2 );
                            } else {
                              mBearing = ( mBearing + bearing ) / 2;
                            }
                          }
                          if ( k < kmax ) {
                            float clino = Float.parseFloat(vals[k]); ++k;
                            if ( mClino < -900 ) {
                              mClino = - clino;
                            } else if ( clino >= -90 && clino <= 90 ) {
                              mClino = ( mClino - clino ) / 2;
                            }
                          }
                          if ( k < kmax ) {
                            if (vals[k].startsWith("#")) {
                              mFlag = vals[k]; ++k;
                            }
                            if ( k < kmax ) mComment = TDUtil.concat( vals, k );
                          }
                        }
                      }
                    }
                  }
                }
                // got shot+LRUD
                int extend = 0;
                if ( mLeft > 0 ) {
                  float ber = mBearing + 270;
                  if ( ber > 360 ) ber -= 360;
                  extend = ( TDSetting.mLRExtend )? (int)TDAzimuth.computeSplayExtend( ber ) : DBlock.EXTEND_UNSET;
                  // FIXME splays in the shots array to keep them interleaved with legs, but have leg flag 2
                  shots.add( new ParserShot( mFrom, TDString.EMPTY, mLeft, ber, 0.0f, 0.0f, extend, 2, false, false, false, "" ) );
                }
                if ( mRight > 0 ) {
                  float ber = mBearing + 90;
                  if ( ber > 360 ) ber -= 360;
                  extend = ( TDSetting.mLRExtend )? (int)TDAzimuth.computeSplayExtend( ber ) : DBlock.EXTEND_UNSET;
                  shots.add( new ParserShot( mFrom, TDString.EMPTY, mRight, ber, 0.0f, 0.0f, extend, 2, false, false, false, "" ) );
                }
                if ( mUp > 0 ) {
                  shots.add( new ParserShot( mFrom, TDString.EMPTY, mUp, 0.0f, 90.0f, 0.0f, DBlock.EXTEND_VERT, 2, false, false, false, "" ) );
                }
                if ( mDown > 0 ) {
                  shots.add( new ParserShot( mFrom, TDString.EMPTY, mDown, 0.0f, -90.0f, 0.0f, DBlock.EXTEND_VERT, 2, false, false, false, "" ) );
                }
                extend = ( mBearing < 90 || mBearing > 270 )? 1 : -1;
                shots.add( new ParserShot( mFrom, mTo, mLength, mBearing, mClino, 0.0f,
                           extend, 0, isDuplicate( mFlag ), isSurface(mFlag), isBackshot(mFlag), mComment ) );
              } else { // got only shot
                int extend = ( mBearing < 90 || mBearing > 270 )? 1 : -1;
                shots.add( new ParserShot( mFrom, mTo, mLength, mBearing, mClino, 0.0f,
                                 extend, 0, isDuplicate( mFlag ), isSurface(mFlag), isBackshot(mFlag), mComment ) );
              }
            } catch ( NumberFormatException e ) {
              TDLog.Error( "ERROR " + mLineCnt + ": " + line + e.getMessage() );
            }
          }
        }
        line = nextLine( br );
      }
    } catch ( IOException e ) {
      // TODO
      TDLog.Error( "ERROR " + mLineCnt + ": " + line );
      throw new ParserException();
    }
    if ( mDate == null ) {
      mDate = TDUtil.currentDate();
    }
    TDLog.Log( TDLog.LOG_THERION, "Parser Compass shots "+ shots.size() +" splays "+ splays.size()  );
    // Log.v( TopoDroidApp.TAG, "Parser Compass shots "+ shots.size() + " splays "+ splays.size() );
  }

  float parseAngleUnit( String unit ) 
  {
    // not handled "percent"
    if ( unit.startsWith("min") ) return 1/60.0f;
    if ( unit.startsWith("grad") ) return TDUtil.GRAD2DEG;
    if ( unit.startsWith("mil") ) return TDUtil.GRAD2DEG;
    // if ( unit.startsWith("deg") ) return 1.0f;
    return 1.0f;
  }

  float parseLengthUnit( String unit ) 
  {
    if ( unit.startsWith("c") ) return 0.01f; // cm centimeter
    if ( unit.startsWith("f") ) return TDUtil.FT2M; // ft feet
    if ( unit.startsWith("i") ) return TDUtil.IN2M; // in inch
    if ( unit.startsWith("milli") || unit.equals("mm") ) return 0.001f; // mm millimeter
    if ( unit.startsWith("y") ) return TDUtil.YD2M; // yd yard
    // if ( unit.startsWith("m") ) return 1.0f;
    return 1.0f;
  }

  // int parseExtend( String extend, int old_extend )
  // {
  //   // skip: hide, start
  //   if ( extend.equals("hide") || extend.equals("start") ) {
  //     return old_extend;
  //   }
  //   if ( extend.equals("left") || extend.equals("reverse") ) {
  //     return DBlock.EXTEND_LEFT;
  //   } 
  //   if ( extend.startsWith("vert") ) {
  //     return DBlock.EXTEND_VERT;
  //   }
  //   if ( extend.startsWith("ignore") ) {
  //     return DBlock.EXTEND_IGNORE;
  //   }
  //   // if ( extend.equals("right") || extend.equals("normal") ) {
  //   //   return DBlock.EXTEND_RIGHT;
  //   // } 
  //   return DBlock.EXTEND_RIGHT;
  // }
}
