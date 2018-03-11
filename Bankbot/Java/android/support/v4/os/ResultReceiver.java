package android.support.v4.os;

import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.support.v4.os.IResultReceiver.Stub;

public class ResultReceiver implements Parcelable {
    public static final Creator CREATOR = new C01091();
    final Handler mHandler;
    final boolean mLocal;
    IResultReceiver mReceiver;

    final class C01091 implements Creator {
        C01091() {
        }

        public ResultReceiver createFromParcel(Parcel parcel) {
            return new ResultReceiver(parcel);
        }

        public ResultReceiver[] newArray(int i) {
            return new ResultReceiver[i];
        }
    }

    class MyResultReceiver extends Stub {
        MyResultReceiver() {
        }

        public void send(int i, Bundle bundle) {
            if (ResultReceiver.this.mHandler != null) {
                ResultReceiver.this.mHandler.post(new MyRunnable(i, bundle));
            } else {
                ResultReceiver.this.onReceiveResult(i, bundle);
            }
        }
    }

    class MyRunnable implements Runnable {
        final int mResultCode;
        final Bundle mResultData;

        MyRunnable(int i, Bundle bundle) {
            this.mResultCode = i;
            this.mResultData = bundle;
        }

        public void run() {
            ResultReceiver.this.onReceiveResult(this.mResultCode, this.mResultData);
        }
    }

    public ResultReceiver(Handler handler) {
        this.mLocal = true;
        this.mHandler = handler;
    }

    ResultReceiver(Parcel parcel) {
        this.mLocal = false;
        this.mHandler = null;
        this.mReceiver = Stub.asInterface(parcel.readStrongBinder());
    }

    public int describeContents() {
        return 0;
    }

    protected void onReceiveResult(int i, Bundle bundle) {
    }

    public void send(int i, Bundle bundle) {
        if (this.mLocal) {
            if (this.mHandler != null) {
                this.mHandler.post(new MyRunnable(i, bundle));
            } else {
                onReceiveResult(i, bundle);
            }
        } else if (this.mReceiver != null) {
            try {
                this.mReceiver.send(i, bundle);
            } catch (RemoteException e) {
            }
        }
    }

    public void writeToParcel(Parcel parcel, int i) {
        synchronized (this) {
            if (this.mReceiver == null) {
                this.mReceiver = new MyResultReceiver();
            }
            parcel.writeStrongBinder(this.mReceiver.asBinder());
        }
    }
}
