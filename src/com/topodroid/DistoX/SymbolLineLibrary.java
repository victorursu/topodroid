/* @file SymbolLineLibrary.java
 *
 * @author marco corvi
 * @date dec 2012
 *
 * @brief TopoDroid drawing: line symbol library
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.DistoX;

// import android.util.Log;

import java.util.Locale;
// import java.util.ArrayList;
// import java.util.TreeSet;
import java.io.File;
// import java.io.PrintWriter;
// import java.io.DataOutputStream;
// import java.io.IOException;

import android.graphics.Paint;
import android.graphics.DashPathEffect;
import android.content.res.Resources;

class SymbolLineLibrary extends SymbolLibrary
{
  static final private String[] DefaultLines = {
    "arrow", "border", "chimney", "pit", "presumed", "rock-border", "slope"
  };

  int mLineUserIndex; // PRIVATE
  int mLineWallIndex;
  int mLineSlopeIndex;
  int mLineSectionIndex;

  SymbolLineLibrary( Resources res )
  {
    super( "l_" );
    mLineUserIndex    =  0;
    mLineWallIndex    = -1;
    mLineSlopeIndex   = -1;
    mLineSectionIndex = -1;
    loadSystemLines( res );
    loadUserLines();
    makeEnabledList();
  }

  // int size() { return mLine.size(); }

  boolean isStyleStraight( int k ) { return ( k < 0 || k >= size() ) || ((SymbolLine)mSymbols.get(k)).mStyleStraight; }

  boolean isClosed( int k ) { return k >= 0 && k < size() && ((SymbolLine)mSymbols.get(k)).mClosed; }

  int getStyleX( int k ) { return ( k < 0 || k >= size() )? 1 : ((SymbolLine)mSymbols.get(k)).mStyleX; }

  // String getLineGroup( int k ) { return ( k < 0 || k >= size() )? null : ((SymbolLine)mSymbols.get(k)).mGroup; }

  boolean isWall( int k ) { return k >= 0 && k < size() && "wall".equals(((SymbolLine)mSymbols.get(k)).mGroup); }

  boolean hasEffect( int k ) { return k >= 0  && k < size() && ((SymbolLine)mSymbols.get(k)).mHasEffect; }

  Paint getLinePaint( int k, boolean reversed )
  {
    if ( k < 0 || k >= size() ) return null;
    SymbolLine s = (SymbolLine)mSymbols.get(k);
    return reversed ? s.mRevPaint : s.mPaint;
  }

  int lineCsxLayer( int k )    { return getSymbolCsxLayer(k); }
  int lineCsxType( int k )     { return getSymbolCsxType(k); }
  int lineCsxCategory( int k ) { return getSymbolCsxCategory(k); }
  int lineCsxPen( int k )      { return getSymbolCsxPen(k); }
  
  // ========================================================================

  private void loadSystemLines( Resources res )
  {
    if ( mSymbols.size() > 0 ) return;                                  //  th_name   group fname
    String user = res.getString ( R.string.p_user );
    SymbolLine symbol = new SymbolLine( res.getString( R.string.thl_user ), "u:user", null, user, 0xffffffff, 1, DrawingLevel.LEVEL_USER );
    symbol.mCsxLayer    = 0; // base
    symbol.mCsxType     = 1; // free-hand
    symbol.mCsxCategory = 0; // cSurvey line cat: NONE
    symbol.mCsxPen      = 2; // generic border
    addSymbol( symbol );

    String wall = res.getString ( R.string.p_wall );
    symbol = new SymbolLine( res.getString( R.string.thl_wall ), wall, wall, wall, 0xffff0000, 2, DrawingLevel.LEVEL_WALL );
    symbol.mCsxLayer    = 5; //
    symbol.mCsxType     = 4; // inverted free-hand
    symbol.mCsxCategory = 1; // cSurvey line cat: CAVE_BORDER
    symbol.mCsxPen      = 1; // cave border
    addSymbol( symbol );

    float[] x = new float[2];
    x[0] = 5;
    x[1] = 10;
    DashPathEffect dash = new DashPathEffect( x, 0 );
    String section = res.getString ( R.string.p_section );
    symbol = new SymbolLine( res.getString( R.string.thl_section ),  section, null, section, 0xffcccccc, 1, dash, dash, DrawingLevel.LEVEL_USER );
    symbol.mCsxLayer    = 6; //
    symbol.mCsxType     = 9; // inverted free-hand
    symbol.mCsxCategory = 96; // cSurvey line cat: CAVE_BORDER
    symbol.mCsxPen      = 3; // cave border
    addSymbol( symbol );

    // mSymbolNr = mSymbols.size();
  }

  void loadUserLines()
  {
    String locale = "name-" + TopoDroidApp.mLocale.toString().substring(0,2);
    String iso = "ISO-8859-1";
    // String iso = "UTF-8";
    // if ( locale.equals( "name-es" ) ) iso = "ISO-8859-1";

    File dir = new File( TDPath.APP_LINE_PATH );
    if ( dir.exists() ) {
      int systemNr = mSymbols.size();
      File[] files = dir.listFiles();
      if ( files == null ) return;
      for ( File file : files ) {
        String fname = file.getName();

        // if ( fname.equals("user") || fname.equals("wall") || fname.equals("section") ) continue;

        SymbolLine symbol = new SymbolLine( file.getPath(), fname, locale, iso );
        if ( symbol.mThName == null ) {
          TDLog.Error( "line with null ThName " + fname );
          // Log.v( "DistoX-SL", "line with null ThName " + fname );
          continue;
        }
        if ( ! hasSymbolByThName( symbol.mThName ) ) {
          addSymbol( symbol );
          String thname = symbol.mThName;
          String name = mPrefix + thname;
          boolean enable = false;
          if ( ! TopoDroidApp.mData.hasSymbolName( name ) ) {
            for ( int k=0; k<DefaultLines.length; ++k ) { 
              if ( DefaultLines[k].equals( thname ) ) { enable = true; break; }
            }
            TopoDroidApp.mData.setSymbolEnabled( name, enable );
          } else {
            enable = TopoDroidApp.mData.getSymbolEnabled( name );
          }
          symbol.setEnabled( enable );
        } else {
          TDLog.Error( "line " + symbol.mThName + " already in library" );
          // Log.v( "DistoX-SL", "line " + symbol.mThName + " already in library" );
        }
      }
      // mSymbolNr = mSymbols.size();
      sortSymbolByName( systemNr );
    } else {
      if ( ! dir.mkdirs( ) ) TDLog.Error( "mkdir error" );
    }
  }

  // thname  symbol th-name
  boolean tryLoadMissingLine( String thname )
  {
    String locale = "name-" + Locale.getDefault().toString().substring(0,2);
    String iso = "ISO-8859-1";
    // String iso = "UTF-8";
    // if ( locale.equals( "name-es" ) ) iso = "ISO-8859-1";

    if ( isSymbolEnabled( thname ) ) return true;
    Symbol symbol = getSymbolByThName( thname );
    // APP_SAVE SYMNBOLS
    if ( symbol == null ) {
      String filename = thname.startsWith("u:")? thname.substring(2) : thname; 
      // Log.v( "DistoX", "load missing line " + thname + " filename " + filename );
      File file = new File( TDPath.APP_SAVE_LINE_PATH + filename );
      if ( ! file.exists() ) return false;
      symbol = new SymbolLine( file.getPath(), file.getName(), locale, iso );
      addSymbol( symbol );
    // } else {
    //   // Log.v( TopoDroidApp.TAG, "enabling missing line " + thname );
    }
    // if ( symbol == null ) return false; // ALWAYS false

    symbol.setEnabled( true ); // TopoDroidApp.mData.isSymbolEnabled( "a_" + symbol.mThName ) );
    
    makeEnabledList();
    return true;
  }
  
  @Override
  protected void makeEnabledList()
  {
    // mLine.clear();
    super.makeEnabledList();
    mLineUserIndex    = getSymbolIndexByThName( "user" );
    mLineWallIndex    = getSymbolIndexByThName( "wall" );
    mLineSlopeIndex   = getSymbolIndexByThName( "slope" );
    mLineSectionIndex = getSymbolIndexByThName( "section" );
  }

  void makeEnabledListFromPalette( SymbolsPalette palette, boolean clear )
  {
    makeEnabledListFromStrings( palette.mPaletteLine, clear ); 
  }

  // ArrayList<String> getSymbolNamesNoSection()
  // {
  //   ArrayList<String> ret = new ArrayList<>();
  //   for ( int k = 0; k < mSymbols.size(); ++ k ) {
  //     if ( k != mTypeSection ) ret.add( s.getName() );
  //   }
  //   return ret;
  // }

}    
