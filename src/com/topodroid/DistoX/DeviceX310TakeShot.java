/* @file DeviceX310TakeShot.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid DistoX310 shooting class
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.DistoX;

import android.os.AsyncTask;
 
// import android.util.Log;

class DeviceX310TakeShot extends AsyncTask<Integer, Integer, Integer >
{
  private final ILister mILister;      // lister with BT button
  private final ListerHandler mLister; // lister that manages downloaded shots (if null shots are not downloaded)
  private final TopoDroidApp  mApp;    // FIXME LEAK
  private final int mNr;               // number of shots to measure before download
 
  DeviceX310TakeShot( ILister ilister, ListerHandler lister, TopoDroidApp app, int nr )
  {
    super();
    mILister  = ilister; 
    mLister   = lister; 
    mApp      = app;
    mNr       = nr;
  } 

  // 0 off
  // 1 on
  // 2 measure
  // 3 measure and download
  @Override
  protected Integer doInBackground( Integer... ii )
  {
    int i = mNr;
    for ( ; i>1; --i ) {
      // Log.v("DistoX", "take shot " + i + " wait " + TDSetting.mWaitLaser + "/" + TDSetting.mWaitShot );
      mApp.setX310Laser( 1, 0, null );
      TDUtil.slowDown( TDSetting.mWaitLaser ); 
      mApp.setX310Laser( 2, 0, null );   
      TDUtil.slowDown( TDSetting.mWaitShot );
    }
    mApp.setX310Laser( 1, 0, null );
    TDUtil.slowDown( TDSetting.mWaitLaser ); 
    // TDUtil.slowDown( TDSetting.mWaitLaser ); 
    return 0;
  }

  @Override
  protected void onProgressUpdate(Integer... progress) { }

  @Override
  protected void onPreExecute( )
  {
    mILister.enableBluetoothButton(false);
  }

  @Override
  protected void onPostExecute( Integer result ) 
  {
    int nr = ( mLister == null )? 0 : mNr; // number of shots to download
    mApp.setX310Laser( 2, nr, mLister ); // measure and download if nr > 0
    mILister.enableBluetoothButton(true);
  }
}
