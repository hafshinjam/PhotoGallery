package org.maktab.photogallery.services;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import org.maktab.photogallery.network.FlickrFetcher;
import org.maktab.photogallery.repository.PhotoRepository;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ThumbnailDownloader<T> extends HandlerThread {

    public static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private ConcurrentHashMap<T, String> mTargetUrl = new ConcurrentHashMap<>();
    private ThumbnailDownloaderListener mListener;


    public ThumbnailDownloaderListener getListener() {
        return mListener;
    }

    public void setListener(ThumbnailDownloaderListener listener) {
        mListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();

        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                //download the image from flicker. url is in msg
                if (msg.what == MESSAGE_DOWNLOAD) {
                    final T target = (T) msg.obj;
                    final String photoUrl = mTargetUrl.get(target);
                    if (photoUrl == null)
                        return;
                        try {
                            byte[] photoBytes = new FlickrFetcher().getBytes(photoUrl);
                            final Bitmap bitmap = BitmapFactory
                                    .decodeByteArray(photoBytes, 0, photoBytes.length);

                            mResponseHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mTargetUrl.get(target) != photoUrl)
                                        return;

                                    mListener.onDownloadCompleted(target, bitmap);
                                }
                            });
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage(), e);
                        }

                }
            }
        };
    }

    public void queueThumbnailMessage(T target, String url) {
        if (url == null) {
            mTargetUrl.remove(target);
        } else {
            mTargetUrl.put(target, url);

            //1. create a message with a handler (what, obj, target)
            //2. send message to queue
            Message message = mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target);
            message.sendToTarget();
        }
    }

    public interface ThumbnailDownloaderListener<T> {
        void onDownloadCompleted(T target, Bitmap bitmap);
    }
}
