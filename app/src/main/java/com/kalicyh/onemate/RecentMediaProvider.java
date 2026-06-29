package com.kalicyh.onemate;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.provider.MediaStore;
import android.util.Log;

import java.io.FileNotFoundException;

public final class RecentMediaProvider extends ContentProvider {
    private static final String TAG = "OneMate";
    static final String COL_CONTENT_URI = "content_uri";
    static final String COL_MIME_TYPE = "mime_type";
    static final String COL_MEDIA_TYPE = "media_type";
    private static final String[] RESULT_COLUMNS = {
            "_id",
            COL_CONTENT_URI,
            COL_MIME_TYPE,
            COL_MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED
    };

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        String path = firstPath(uri);
        if ("item".equals(path)) {
            return queryItem(uri, projection);
        }
        if (!"recent".equals(path)) {
            return null;
        }
        MatrixCursor result = new MatrixCursor(RESULT_COLUMNS);
        Uri files = MediaStore.Files.getContentUri("external");
        String[] columns = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATE_ADDED
        };
        String mediaSelection = MediaStore.Files.FileColumns.MEDIA_TYPE + " IN (?,?)";
        String[] mediaArgs = {
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
        };
        try (Cursor cursor = getContext().getContentResolver().query(
                files,
                columns,
                mediaSelection,
                mediaArgs,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC")) {
            if (cursor == null) {
                return result;
            }
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
            int typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
            int mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
            int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED);
            int count = 0;
            while (cursor.moveToNext() && count < 6) {
                long id = cursor.getLong(idColumn);
                int mediaType = cursor.getInt(typeColumn);
                String type = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ? "video" : "image";
                String mime = cursor.getString(mimeColumn);
                if (mime == null || mime.isEmpty()) {
                    mime = "video".equals(type) ? "video/*" : "image/*";
                }
                Uri contentUri = new Uri.Builder()
                        .scheme("content")
                        .authority(ToolbarConfig.MEDIA_PROVIDER_AUTHORITY)
                        .appendPath("item")
                        .appendPath(type)
                        .appendPath(String.valueOf(id))
                        .build();
                result.addRow(new Object[]{id, contentUri.toString(), mime, type, cursor.getLong(dateColumn)});
                count++;
            }
        }
        return result;
    }

    @Override
    public String getType(Uri uri) {
        String type = mediaType(uri);
        if (type == null) {
            return null;
        }
        Uri mediaUri;
        try {
            mediaUri = mediaStoreUri(uri);
        } catch (FileNotFoundException e) {
            return null;
        }
        String[] projection = {MediaStore.Files.FileColumns.MIME_TYPE};
        try (Cursor cursor = getContext().getContentResolver().query(mediaUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String mime = cursor.getString(0);
                if (mime != null && !mime.isEmpty()) {
                    return mime;
                }
            }
        }
        return "video".equals(type) ? "video/*" : "image/*";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode) && !"rt".equals(mode)) {
            throw new FileNotFoundException("read only");
        }
        Log.i(TAG, "open recent media " + uri);
        return getContext().getContentResolver().openFileDescriptor(mediaStoreUri(uri), "r");
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        return new AssetFileDescriptor(openFile(uri, mode), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
    }

    @Override
    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts)
            throws FileNotFoundException {
        String type = getType(uri);
        if (!matchesMime(type, mimeTypeFilter)) {
            throw new FileNotFoundException("unsupported type " + mimeTypeFilter);
        }
        Log.i(TAG, "open typed recent media " + uri + " as " + mimeTypeFilter);
        return openAssetFile(uri, "r");
    }

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        String type = getType(uri);
        return matchesMime(type, mimeTypeFilter) ? new String[]{type} : null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    private static String firstPath(Uri uri) {
        return uri.getPathSegments().isEmpty() ? null : uri.getPathSegments().get(0);
    }

    private static String mediaType(Uri uri) {
        return uri.getPathSegments().size() < 3 ? null : uri.getPathSegments().get(1);
    }

    private Cursor queryItem(Uri uri, String[] projection) {
        Uri mediaUri;
        try {
            mediaUri = mediaStoreUri(uri);
        } catch (FileNotFoundException e) {
            return null;
        }
        String[] requested = projection == null || projection.length == 0
                ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, MediaStore.MediaColumns.MIME_TYPE}
                : projection;
        String displayName = fallbackDisplayName(uri);
        long size = -1L;
        String mime = getType(uri);
        String[] mediaProjection = {
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE
        };
        try (Cursor cursor = getContext().getContentResolver().query(mediaUri, mediaProjection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String value = cursor.getString(0);
                if (value != null && !value.isEmpty()) {
                    displayName = value;
                }
                size = cursor.getLong(1);
                value = cursor.getString(2);
                if (value != null && !value.isEmpty()) {
                    mime = value;
                }
            }
        }
        MatrixCursor result = new MatrixCursor(requested);
        Object[] row = new Object[requested.length];
        for (int i = 0; i < requested.length; i++) {
            String column = requested[i];
            if (OpenableColumns.DISPLAY_NAME.equals(column) || MediaStore.MediaColumns.DISPLAY_NAME.equals(column)) {
                row[i] = displayName;
            } else if (OpenableColumns.SIZE.equals(column) || MediaStore.MediaColumns.SIZE.equals(column)) {
                row[i] = size;
            } else if (MediaStore.MediaColumns.MIME_TYPE.equals(column) || COL_MIME_TYPE.equals(column)) {
                row[i] = mime;
            } else if (MediaStore.Files.FileColumns._ID.equals(column) || "_id".equals(column)) {
                row[i] = uri.getLastPathSegment();
            }
        }
        result.addRow(row);
        Log.i(TAG, "query recent media item " + uri);
        return result;
    }

    private static String fallbackDisplayName(Uri uri) {
        String type = mediaType(uri);
        String extension = "video".equals(type) ? ".mp4" : ".jpg";
        return "onemate_" + uri.getLastPathSegment() + extension;
    }

    private static boolean matchesMime(String type, String filter) {
        if (type == null || filter == null) {
            return false;
        }
        if ("*/*".equals(filter) || type.equals(filter)) {
            return true;
        }
        int slash = filter.indexOf('/');
        return slash > 0
                && filter.endsWith("/*")
                && type.startsWith(filter.substring(0, slash + 1));
    }

    private static Uri mediaStoreUri(Uri uri) throws FileNotFoundException {
        if (!"item".equals(firstPath(uri))) {
            throw new FileNotFoundException(uri.toString());
        }
        String type = mediaType(uri);
        if (!"image".equals(type) && !"video".equals(type)) {
            throw new FileNotFoundException(uri.toString());
        }
        long id;
        try {
            id = Long.parseLong(uri.getPathSegments().get(2));
        } catch (RuntimeException e) {
            throw new FileNotFoundException(uri.toString());
        }
        Uri base = "video".equals(type)
                ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        return ContentUris.withAppendedId(base, id);
    }
}
