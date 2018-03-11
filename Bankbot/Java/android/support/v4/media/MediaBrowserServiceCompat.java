package android.support.v4.media;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompatApi23.ItemCallback;
import android.support.v4.media.session.MediaSessionCompat.Token;
import android.support.v4.os.ResultReceiver;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public abstract class MediaBrowserServiceCompat extends Service {
    private static final boolean DBG = false;
    public static final String KEY_MEDIA_ITEM = "media_item";
    private static final int RESULT_FLAG_OPTION_NOT_HANDLED = 1;
    public static final String SERVICE_INTERFACE = "android.media.browse.MediaBrowserService";
    private static final String TAG = "MediaBrowserServiceCompat";
    private final ArrayMap mConnections = new ArrayMap();
    private final ServiceHandler mHandler = new ServiceHandler();
    private MediaBrowserServiceImpl mImpl;
    Token mSession;

    public class Result {
        private Object mDebug;
        private boolean mDetachCalled;
        private int mFlags;
        private boolean mSendResultCalled;

        Result(Object obj) {
            this.mDebug = obj;
        }

        public void detach() {
            if (this.mDetachCalled) {
                throw new IllegalStateException("detach() called when detach() had already been called for: " + this.mDebug);
            } else if (this.mSendResultCalled) {
                throw new IllegalStateException("detach() called when sendResult() had already been called for: " + this.mDebug);
            } else {
                this.mDetachCalled = true;
            }
        }

        boolean isDone() {
            return (this.mDetachCalled || this.mSendResultCalled) ? true : MediaBrowserServiceCompat.DBG;
        }

        void onResultSent(Object obj, int i) {
        }

        public void sendResult(Object obj) {
            if (this.mSendResultCalled) {
                throw new IllegalStateException("sendResult() called twice for: " + this.mDebug);
            }
            this.mSendResultCalled = true;
            onResultSent(obj, this.mFlags);
        }

        void setFlags(int i) {
            this.mFlags = i;
        }
    }

    public final class BrowserRoot {
        public static final String EXTRA_OFFLINE = "android.service.media.extra.OFFLINE";
        public static final String EXTRA_RECENT = "android.service.media.extra.RECENT";
        public static final String EXTRA_SUGGESTED = "android.service.media.extra.SUGGESTED";
        private final Bundle mExtras;
        private final String mRootId;

        public BrowserRoot(String str, Bundle bundle) {
            if (str == null) {
                throw new IllegalArgumentException("The root id in BrowserRoot cannot be null. Use null for BrowserRoot instead.");
            }
            this.mRootId = str;
            this.mExtras = bundle;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }

        public String getRootId() {
            return this.mRootId;
        }
    }

    class ConnectionRecord {
        ServiceCallbacks callbacks;
        String pkg;
        BrowserRoot root;
        Bundle rootHints;
        HashMap subscriptions;

        private ConnectionRecord() {
            this.subscriptions = new HashMap();
        }
    }

    interface MediaBrowserServiceImpl {
        IBinder onBind(Intent intent);

        void onCreate();
    }

    class MediaBrowserServiceImplApi21 implements MediaBrowserServiceImpl {
        private Object mServiceObj;

        MediaBrowserServiceImplApi21() {
        }

        public IBinder onBind(Intent intent) {
            return MediaBrowserServiceCompatApi21.onBind(this.mServiceObj, intent);
        }

        public void onCreate() {
            this.mServiceObj = MediaBrowserServiceCompatApi21.createService();
            MediaBrowserServiceCompatApi21.onCreate(this.mServiceObj, new ServiceImplApi21());
        }
    }

    class MediaBrowserServiceImplApi23 implements MediaBrowserServiceImpl {
        private Object mServiceObj;

        MediaBrowserServiceImplApi23() {
        }

        public IBinder onBind(Intent intent) {
            return MediaBrowserServiceCompatApi21.onBind(this.mServiceObj, intent);
        }

        public void onCreate() {
            this.mServiceObj = MediaBrowserServiceCompatApi23.createService();
            MediaBrowserServiceCompatApi23.onCreate(this.mServiceObj, new ServiceImplApi23());
        }
    }

    class MediaBrowserServiceImplBase implements MediaBrowserServiceImpl {
        private Messenger mMessenger;

        MediaBrowserServiceImplBase() {
        }

        public IBinder onBind(Intent intent) {
            return MediaBrowserServiceCompat.SERVICE_INTERFACE.equals(intent.getAction()) ? this.mMessenger.getBinder() : null;
        }

        public void onCreate() {
            this.mMessenger = new Messenger(MediaBrowserServiceCompat.this.mHandler);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface ResultFlags {
    }

    interface ServiceCallbacks {
        IBinder asBinder();

        void onConnect(String str, Token token, Bundle bundle);

        void onConnectFailed();

        void onLoadChildren(String str, List list, Bundle bundle);
    }

    class ServiceCallbacksApi21 implements ServiceCallbacks {
        final android.support.v4.media.MediaBrowserServiceCompatApi21.ServiceCallbacks mCallbacks;
        Messenger mMessenger;

        ServiceCallbacksApi21(android.support.v4.media.MediaBrowserServiceCompatApi21.ServiceCallbacks serviceCallbacks) {
            this.mCallbacks = serviceCallbacks;
        }

        public IBinder asBinder() {
            return this.mCallbacks.asBinder();
        }

        public void onConnect(String str, Token token, Bundle bundle) {
            if (bundle == null) {
                bundle = new Bundle();
            }
            this.mMessenger = new Messenger(MediaBrowserServiceCompat.this.mHandler);
            BundleCompat.putBinder(bundle, MediaBrowserProtocol.EXTRA_MESSENGER_BINDER, this.mMessenger.getBinder());
            bundle.putInt(MediaBrowserProtocol.EXTRA_SERVICE_VERSION, 1);
            this.mCallbacks.onConnect(str, token.getToken(), bundle);
        }

        public void onConnectFailed() {
            this.mCallbacks.onConnectFailed();
        }

        public void onLoadChildren(String str, List list, Bundle bundle) {
            List list2 = null;
            if (list != null) {
                List arrayList = new ArrayList();
                for (MediaItem mediaItem : list) {
                    Parcel obtain = Parcel.obtain();
                    mediaItem.writeToParcel(obtain, 0);
                    arrayList.add(obtain);
                }
                list2 = arrayList;
            }
            this.mCallbacks.onLoadChildren(str, list2);
        }
    }

    class ServiceCallbacksCompat implements ServiceCallbacks {
        final Messenger mCallbacks;

        ServiceCallbacksCompat(Messenger messenger) {
            this.mCallbacks = messenger;
        }

        private void sendRequest(int i, Bundle bundle) {
            Message obtain = Message.obtain();
            obtain.what = i;
            obtain.arg1 = 1;
            obtain.setData(bundle);
            this.mCallbacks.send(obtain);
        }

        public IBinder asBinder() {
            return this.mCallbacks.getBinder();
        }

        public void onConnect(String str, Token token, Bundle bundle) {
            if (bundle == null) {
                bundle = new Bundle();
            }
            bundle.putInt(MediaBrowserProtocol.EXTRA_SERVICE_VERSION, 1);
            Bundle bundle2 = new Bundle();
            bundle2.putString(MediaBrowserProtocol.DATA_MEDIA_ITEM_ID, str);
            bundle2.putParcelable(MediaBrowserProtocol.DATA_MEDIA_SESSION_TOKEN, token);
            bundle2.putBundle(MediaBrowserProtocol.DATA_ROOT_HINTS, bundle);
            sendRequest(1, bundle2);
        }

        public void onConnectFailed() {
            sendRequest(2, null);
        }

        public void onLoadChildren(String str, List list, Bundle bundle) {
            Bundle bundle2 = new Bundle();
            bundle2.putString(MediaBrowserProtocol.DATA_MEDIA_ITEM_ID, str);
            bundle2.putBundle(MediaBrowserProtocol.DATA_OPTIONS, bundle);
            if (list != null) {
                bundle2.putParcelableArrayList(MediaBrowserProtocol.DATA_MEDIA_ITEM_LIST, list instanceof ArrayList ? (ArrayList) list : new ArrayList(list));
            }
            sendRequest(3, bundle2);
        }
    }

    final class ServiceHandler extends Handler {
        private final ServiceImpl mServiceImpl;

        private ServiceHandler() {
            this.mServiceImpl = new ServiceImpl();
        }

        public ServiceImpl getServiceImpl() {
            return this.mServiceImpl;
        }

        public void handleMessage(Message message) {
            Bundle data = message.getData();
            switch (message.what) {
                case 1:
                    this.mServiceImpl.connect(data.getString(MediaBrowserProtocol.DATA_PACKAGE_NAME), data.getInt(MediaBrowserProtocol.DATA_CALLING_UID), data.getBundle(MediaBrowserProtocol.DATA_ROOT_HINTS), new ServiceCallbacksCompat(message.replyTo));
                    return;
                case 2:
                    this.mServiceImpl.disconnect(new ServiceCallbacksCompat(message.replyTo));
                    return;
                case 3:
                    this.mServiceImpl.addSubscription(data.getString(MediaBrowserProtocol.DATA_MEDIA_ITEM_ID), data.getBundle(MediaBrowserProtocol.DATA_OPTIONS), new ServiceCallbacksCompat(message.replyTo));
                    return;
                case 4:
                    this.mServiceImpl.removeSubscription(data.getString(MediaBrowserProtocol.DATA_MEDIA_ITEM_ID), data.getBundle(MediaBrowserProtocol.DATA_OPTIONS), new ServiceCallbacksCompat(message.replyTo));
                    return;
                case 5:
                    this.mServiceImpl.getMediaItem(data.getString(MediaBrowserProtocol.DATA_MEDIA_ITEM_ID), (ResultReceiver) data.getParcelable(MediaBrowserProtocol.DATA_RESULT_RECEIVER));
                    return;
                case 6:
                    this.mServiceImpl.registerCallbacks(new ServiceCallbacksCompat(message.replyTo));
                    return;
                default:
                    Log.w(MediaBrowserServiceCompat.TAG, "Unhandled message: " + message + "\n  Service version: " + 1 + "\n  Client version: " + message.arg1);
                    return;
            }
        }

        public void postOrRun(Runnable runnable) {
            if (Thread.currentThread() == getLooper().getThread()) {
                runnable.run();
            } else {
                post(runnable);
            }
        }

        public boolean sendMessageAtTime(Message message, long j) {
            Bundle data = message.getData();
            data.setClassLoader(MediaBrowserCompat.class.getClassLoader());
            data.putInt(MediaBrowserProtocol.DATA_CALLING_UID, Binder.getCallingUid());
            return super.sendMessageAtTime(message, j);
        }
    }

    class ServiceImpl {
        private ServiceImpl() {
        }

        public void addSubscription(final String str, final Bundle bundle, final ServiceCallbacks serviceCallbacks) {
            MediaBrowserServiceCompat.this.mHandler.postOrRun(new Runnable() {
                public void run() {
                    ConnectionRecord connectionRecord = (ConnectionRecord) MediaBrowserServiceCompat.this.mConnections.get(serviceCallbacks.asBinder());
                    if (connectionRecord == null) {
                        Log.w(MediaBrowserServiceCompat.TAG, "addSubscription for callback that isn't registered id=" + str);
                    } else {
                        MediaBrowserServiceCompat.this.addSubscription(str, connectionRecord, bundle);
                    }
                }
            });
        }

        public void connect(String str, int i, Bundle bundle, ServiceCallbacks serviceCallbacks) {
            if (MediaBrowserServiceCompat.this.isValidPackage(str, i)) {
                final ServiceCallbacks serviceCallbacks2 = serviceCallbacks;
                final String str2 = str;
                final Bundle bundle2 = bundle;
                final int i2 = i;
                MediaBrowserServiceCompat.this.mHandler.postOrRun(new Runnable() {
                    public void run() {
                        IBinder asBinder = serviceCallbacks2.asBinder();
                        MediaBrowserServiceCompat.this.mConnections.remove(asBinder);
                        ConnectionRecord connectionRecord = new ConnectionRecord();
                        connectionRecord.pkg = str2;
                        connectionRecord.rootHints = bundle2;
                        connectionRecord.callbacks = serviceCallbacks2;
                        connectionRecord.root = MediaBrowserServiceCompat.this.onGetRoot(str2, i2, bundle2);
                        if (connectionRecord.root == null) {
                            Log.i(MediaBrowserServiceCompat.TAG, "No root for client " + str2 + " from service " + getClass().getName());
                            try {
                                serviceCallbacks2.onConnectFailed();
                                return;
                            } catch (RemoteException e) {
                                Log.w(MediaBrowserServiceCompat.TAG, "Calling onConnectFailed() failed. Ignoring. pkg=" + str2);
                                return;
                            }
                        }
                        try {
                            MediaBrowserServiceCompat.this.mConnections.put(asBinder, connectionRecord);
                            if (MediaBrowserServiceCompat.this.mSession != null) {
                                serviceCallbacks2.onConnect(connectionRecord.root.getRootId(), MediaBrowserServiceCompat.this.mSession, connectionRecord.root.getExtras());
                            }
                        } catch (RemoteException e2) {
                            Log.w(MediaBrowserServiceCompat.TAG, "Calling onConnect() failed. Dropping client. pkg=" + str2);
                            MediaBrowserServiceCompat.this.mConnections.remove(asBinder);
                        }
                    }
                });
                return;
            }
            throw new IllegalArgumentException("Package/uid mismatch: uid=" + i + " package=" + str);
        }

        public void disconnect(final ServiceCallbacks serviceCallbacks) {
            MediaBrowserServiceCompat.this.mHandler.postOrRun(new Runnable() {
                public void run() {
                    if (((ConnectionRecord) MediaBrowserServiceCompat.this.mConnections.remove(serviceCallbacks.asBinder())) == null) {
                    }
                }
            });
        }

        public void getMediaItem(final String str, final ResultReceiver resultReceiver) {
            if (!TextUtils.isEmpty(str) && resultReceiver != null) {
                MediaBrowserServiceCompat.this.mHandler.postOrRun(new Runnable() {
                    public void run() {
                        MediaBrowserServiceCompat.this.performLoadItem(str, resultReceiver);
                    }
                });
            }
        }

        public void registerCallbacks(final ServiceCallbacks serviceCallbacks) {
            MediaBrowserServiceCompat.this.mHandler.postOrRun(new Runnable() {
                public void run() {
                    IBinder asBinder = serviceCallbacks.asBinder();
                    MediaBrowserServiceCompat.this.mConnections.remove(asBinder);
                    ConnectionRecord connectionRecord = new ConnectionRecord();
                    connectionRecord.callbacks = serviceCallbacks;
                    MediaBrowserServiceCompat.this.mConnections.put(asBinder, connectionRecord);
                }
            });
        }

        public void removeSubscription(final String str, final Bundle bundle, final ServiceCallbacks serviceCallbacks) {
            MediaBrowserServiceCompat.this.mHandler.postOrRun(new Runnable() {
                public void run() {
                    ConnectionRecord connectionRecord = (ConnectionRecord) MediaBrowserServiceCompat.this.mConnections.get(serviceCallbacks.asBinder());
                    if (connectionRecord == null) {
                        Log.w(MediaBrowserServiceCompat.TAG, "removeSubscription for callback that isn't registered id=" + str);
                    } else if (!MediaBrowserServiceCompat.this.removeSubscription(str, connectionRecord, bundle)) {
                        Log.w(MediaBrowserServiceCompat.TAG, "removeSubscription called for " + str + " which is not subscribed");
                    }
                }
            });
        }
    }

    class ServiceImplApi21 implements android.support.v4.media.MediaBrowserServiceCompatApi21.ServiceImplApi21 {
        final ServiceImpl mServiceImpl;

        ServiceImplApi21() {
            this.mServiceImpl = MediaBrowserServiceCompat.this.mHandler.getServiceImpl();
        }

        public void addSubscription(String str, android.support.v4.media.MediaBrowserServiceCompatApi21.ServiceCallbacks serviceCallbacks) {
            this.mServiceImpl.addSubscription(str, null, new ServiceCallbacksApi21(serviceCallbacks));
        }

        public void connect(String str, Bundle bundle, android.support.v4.media.MediaBrowserServiceCompatApi21.ServiceCallbacks serviceCallbacks) {
            this.mServiceImpl.connect(str, Binder.getCallingUid(), bundle, new ServiceCallbacksApi21(serviceCallbacks));
        }

        public void disconnect(android.support.v4.media.MediaBrowserServiceCompatApi21.ServiceCallbacks serviceCallbacks) {
            this.mServiceImpl.disconnect(new ServiceCallbacksApi21(serviceCallbacks));
        }

        public void removeSubscription(String str, android.support.v4.media.MediaBrowserServiceCompatApi21.ServiceCallbacks serviceCallbacks) {
            this.mServiceImpl.removeSubscription(str, null, new ServiceCallbacksApi21(serviceCallbacks));
        }
    }

    class ServiceImplApi23 extends ServiceImplApi21 implements android.support.v4.media.MediaBrowserServiceCompatApi23.ServiceImplApi23 {
        private ServiceImplApi23() {
            super();
        }

        public void getMediaItem(String str, final ItemCallback itemCallback) {
            this.mServiceImpl.getMediaItem(str, new ResultReceiver(MediaBrowserServiceCompat.this.mHandler) {
                protected void onReceiveResult(int i, Bundle bundle) {
                    Parcel parcel;
                    MediaItem mediaItem = (MediaItem) bundle.getParcelable(MediaBrowserServiceCompat.KEY_MEDIA_ITEM);
                    if (mediaItem != null) {
                        Parcel obtain = Parcel.obtain();
                        mediaItem.writeToParcel(obtain, 0);
                        parcel = obtain;
                    } else {
                        parcel = null;
                    }
                    itemCallback.onItemLoaded(i, bundle, parcel);
                }
            });
        }
    }

    private void addSubscription(String str, ConnectionRecord connectionRecord, Bundle bundle) {
        List list = (List) connectionRecord.subscriptions.get(str);
        List<Bundle> arrayList = list == null ? new ArrayList() : list;
        for (Bundle areSameOptions : arrayList) {
            if (MediaBrowserCompatUtils.areSameOptions(bundle, areSameOptions)) {
                return;
            }
        }
        arrayList.add(bundle);
        connectionRecord.subscriptions.put(str, arrayList);
        performLoadChildren(str, connectionRecord, bundle);
    }

    private List applyOptions(List list, Bundle bundle) {
        int i = bundle.getInt(MediaBrowserCompat.EXTRA_PAGE, -1);
        int i2 = bundle.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, -1);
        if (i == -1 && i2 == -1) {
            return list;
        }
        int i3 = i2 * (i - 1);
        int i4 = i3 + i2;
        if (i < 1 || i2 < 1 || i3 >= list.size()) {
            return Collections.emptyList();
        }
        if (i4 > list.size()) {
            i4 = list.size();
        }
        return list.subList(i3, i4);
    }

    private boolean isValidPackage(String str, int i) {
        if (str == null) {
            return DBG;
        }
        for (String equals : getPackageManager().getPackagesForUid(i)) {
            if (equals.equals(str)) {
                return true;
            }
        }
        return DBG;
    }

    private void notifyChildrenChangedInternal(final String str, final Bundle bundle) {
        if (str == null) {
            throw new IllegalArgumentException("parentId cannot be null in notifyChildrenChanged");
        }
        this.mHandler.post(new Runnable() {
            public void run() {
                for (IBinder iBinder : MediaBrowserServiceCompat.this.mConnections.keySet()) {
                    ConnectionRecord connectionRecord = (ConnectionRecord) MediaBrowserServiceCompat.this.mConnections.get(iBinder);
                    List<Bundle> list = (List) connectionRecord.subscriptions.get(str);
                    if (list != null) {
                        for (Bundle bundle : list) {
                            if (MediaBrowserCompatUtils.hasDuplicatedItems(bundle, bundle)) {
                                MediaBrowserServiceCompat.this.performLoadChildren(str, connectionRecord, bundle);
                                break;
                            }
                        }
                    }
                }
            }
        });
    }

    private void performLoadChildren(String str, ConnectionRecord connectionRecord, Bundle bundle) {
        final ConnectionRecord connectionRecord2 = connectionRecord;
        final String str2 = str;
        final Bundle bundle2 = bundle;
        Result c00743 = new Result(str) {
            void onResultSent(List list, int i) {
                if (MediaBrowserServiceCompat.this.mConnections.get(connectionRecord2.callbacks.asBinder()) == connectionRecord2) {
                    if ((i & 1) != 0) {
                        list = MediaBrowserCompatUtils.applyOptions(list, bundle2);
                    }
                    try {
                        connectionRecord2.callbacks.onLoadChildren(str2, list, bundle2);
                    } catch (RemoteException e) {
                        Log.w(MediaBrowserServiceCompat.TAG, "Calling onLoadChildren() failed for id=" + str2 + " package=" + connectionRecord2.pkg);
                    }
                }
            }
        };
        if (bundle == null) {
            onLoadChildren(str, c00743);
        } else {
            onLoadChildren(str, c00743, bundle);
        }
        if (!c00743.isDone()) {
            throw new IllegalStateException("onLoadChildren must call detach() or sendResult() before returning for package=" + connectionRecord.pkg + " id=" + str);
        }
    }

    private void performLoadItem(String str, final ResultReceiver resultReceiver) {
        Result c00754 = new Result(str) {
            void onResultSent(MediaItem mediaItem, int i) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(MediaBrowserServiceCompat.KEY_MEDIA_ITEM, mediaItem);
                resultReceiver.send(0, bundle);
            }
        };
        onLoadItem(str, c00754);
        if (!c00754.isDone()) {
            throw new IllegalStateException("onLoadItem must call detach() or sendResult() before returning for id=" + str);
        }
    }

    private boolean removeSubscription(String str, ConnectionRecord connectionRecord, Bundle bundle) {
        List<Bundle> list = (List) connectionRecord.subscriptions.get(str);
        if (list == null) {
            return DBG;
        }
        boolean z;
        for (Bundle bundle2 : list) {
            if (MediaBrowserCompatUtils.areSameOptions(bundle, bundle2)) {
                list.remove(bundle2);
                z = true;
                break;
            }
        }
        z = DBG;
        if (list.size() != 0) {
            return z;
        }
        connectionRecord.subscriptions.remove(str);
        return z;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
    }

    public Token getSessionToken() {
        return this.mSession;
    }

    public void notifyChildrenChanged(String str) {
        notifyChildrenChangedInternal(str, null);
    }

    public void notifyChildrenChanged(String str, Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("options cannot be null in notifyChildrenChanged");
        }
        notifyChildrenChangedInternal(str, bundle);
    }

    public IBinder onBind(Intent intent) {
        return this.mImpl.onBind(intent);
    }

    public void onCreate() {
        super.onCreate();
        if (VERSION.SDK_INT >= 23) {
            this.mImpl = new MediaBrowserServiceImplApi23();
        } else if (VERSION.SDK_INT >= 21) {
            this.mImpl = new MediaBrowserServiceImplApi21();
        } else {
            this.mImpl = new MediaBrowserServiceImplBase();
        }
        this.mImpl.onCreate();
    }

    public abstract BrowserRoot onGetRoot(String str, int i, Bundle bundle);

    public abstract void onLoadChildren(String str, Result result);

    public void onLoadChildren(String str, Result result, Bundle bundle) {
        result.setFlags(1);
        onLoadChildren(str, result);
    }

    public void onLoadItem(String str, Result result) {
        result.sendResult(null);
    }

    public void setSessionToken(final Token token) {
        if (token == null) {
            throw new IllegalArgumentException("Session token may not be null.");
        } else if (this.mSession != null) {
            throw new IllegalStateException("The session token has already been set.");
        } else {
            this.mSession = token;
            this.mHandler.post(new Runnable() {
                public void run() {
                    for (IBinder iBinder : MediaBrowserServiceCompat.this.mConnections.keySet()) {
                        ConnectionRecord connectionRecord = (ConnectionRecord) MediaBrowserServiceCompat.this.mConnections.get(iBinder);
                        try {
                            connectionRecord.callbacks.onConnect(connectionRecord.root.getRootId(), token, connectionRecord.root.getExtras());
                        } catch (RemoteException e) {
                            Log.w(MediaBrowserServiceCompat.TAG, "Connection for " + connectionRecord.pkg + " is no longer valid.");
                            MediaBrowserServiceCompat.this.mConnections.remove(iBinder);
                        }
                    }
                }
            });
        }
    }
}
