/*
 * Copyright (C) 2013 47 Degrees, LLC
 * http://47deg.com
 * http://apps.ly
 * hello@47deg.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ly.apps.android.rest.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.*;

/**
 * Uses DiskLruCache to provide a easy api to serialize and unserialize objects
 */
public class ObjectCache {

    private DiskLruCache mDiskCache;
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;

    public ObjectCache(Context context, String uniqueName, int diskCacheSize) {
        try {
            final File diskCacheDir = getDiskCacheDir(context, uniqueName);
            mDiskCache = DiskLruCache.open(diskCacheDir, context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode, VALUE_COUNT, diskCacheSize);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private boolean writeObjectToFile(Object object, DiskLruCache.Editor editor)
            throws IOException, FileNotFoundException {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(editor.newOutputStream(0));
            out.writeObject(object);
            return true;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private File getDiskCacheDir(Context context, String uniqueName) {

        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !FileUtils.isExternalStorageRemovable() ?
                        FileUtils.getExternalCacheDir(context).getPath() :
                        context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    public void put(String key, Object data) {

        DiskLruCache.Editor editor = null;
        try {
            editor = mDiskCache.edit(key);
            if (editor == null) {
                return;
            }

            if (writeObjectToFile(data, editor)) {
                mDiskCache.flush();
                editor.commit();
                Logger.d("object put on disk cache " + key);
            } else {
                editor.abort();
                Logger.d("ERROR object put on disk cache " + key);
            }
        } catch (IOException e) {
            Logger.e("ERROR object put on disk cache " + key, e);
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (IOException ignored) {
            }
        }

    }

    public Object getObject(String key) {

        Object object = null;
        DiskLruCache.Snapshot snapshot = null;
        try {

            snapshot = mDiskCache.get(key);
            if (snapshot == null) {
                return null;
            }
            final InputStream in = snapshot.getInputStream(0);
            if (in != null) {
                final ObjectInputStream buffIn =
                        new ObjectInputStream(in);
                object = buffIn.readObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }

        Logger.d(object == null ? "" : "image read from disk " + key);

        return object;

    }

    public boolean containsKey(String key) {

        boolean contained = false;
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = mDiskCache.get(key);
            contained = snapshot != null;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }

        return contained;

    }

    public void clearCache() {
        Logger.d("cache cleared");
        try {
            mDiskCache.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getCacheFolder() {
        return mDiskCache.getDirectory();
    }

    public void clear(String key) {
        throw new UnsupportedOperationException();
    }
}