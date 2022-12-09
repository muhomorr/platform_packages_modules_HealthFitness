package android.healthconnect.aidl;

import android.healthconnect.aidl.RecordsParcel;
import android.healthconnect.aidl.HealthConnectExceptionParcel;

/**
 * Callback for {@link IHealthConnectService#readRecord}.
 *
 * {@hide}
 */
interface IReadRecordsResponseCallback {
    // Called on a successful operation
    oneway void onResult(in RecordsParcel parcel);
    // Called when an error is hit
    oneway void onError(in HealthConnectExceptionParcel exception);
}
