package org.maktab.photogallery.repository;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.maktab.photogallery.controller.fragment.PhotoGalleryFragment;
import org.maktab.photogallery.model.GalleryItem;
import org.maktab.photogallery.network.FlickrFetcher;

import java.util.ArrayList;
import java.util.List;

public class PhotoRepository {

    private static PhotoRepository sInstance;
    private List<GalleryItem> mItems = new ArrayList<>();
    private LruCache<String, Bitmap> mPhotoCache;

    public static PhotoRepository getInstance() {
        if (sInstance == null)
            sInstance = new PhotoRepository();

        return sInstance;
    }

    public List<GalleryItem> getItems() {
        String url = FlickrFetcher.generateUrl();

        FlickrFetcher flickrFetcher = new FlickrFetcher();
        String jsonBodyString = null;
        try {
            jsonBodyString = flickrFetcher.getString(url);
            Log.d(PhotoGalleryFragment.TAG, jsonBodyString);
            JSONObject jsonBody = new JSONObject(jsonBodyString);
            mItems = parseJson(jsonBody);
        } catch (Exception e) {
            Log.e(PhotoGalleryFragment.TAG, e.getMessage(), e);
        }
        return mItems;
    }

    public void setItems(List<GalleryItem> items) {
        mItems = items;
    }

    private PhotoRepository() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        mPhotoCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    public Bitmap getBitmapFromPhotoCache(String key) {
        return mPhotoCache.get(key);
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromPhotoCache(key) == null) {
            mPhotoCache.put(key, bitmap);
        }
    }

    private List<GalleryItem> parseJson(JSONObject jsonBody) throws JSONException {
        List<GalleryItem> items = new ArrayList<>();

        JSONObject photosObject = jsonBody.getJSONObject("photos");
        JSONArray photoArray = photosObject.getJSONArray("photo");
        for (int i = 0; i < photoArray.length(); i++) {
            JSONObject photoObject = photoArray.getJSONObject(i);
            if (!photoObject.has("url_s"))
                continue;

            String id = photoObject.getString("id");
            String caption = photoObject.getString("title");
            String url = photoObject.getString("url_s");

            GalleryItem item = new GalleryItem(id, caption, url);
            items.add(item);
        }

        return items;
    }
}
