package com.megster.cordova;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.lang.Integer;

import android.database.Cursor;
import android.provider.MediaStore;
import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import android.Manifest;
import android.Manifest.permission;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.ClipData;
import android.os.Bundle;
import android.os.Build;
import android.content.ContentResolver;
import android.webkit.MimeTypeMap;

import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FileChooser extends CordovaPlugin {

    private final String PLUGIN_NAME = "FileChooser";
    private static final String ACTION_OPEN = "open";
    private static final int PICK_FILE_REQUEST = 1;
    static final String READ = Manifest.permission.READ_EXTERNAL_STORAGE;
    public static final int SEARCH_REQ_CODE = 0;

    private String mimeType = "*/*"; // What type of media to retrieve
    private boolean allowMultipleSelection = true; // Source type (needs to be saved for the permission handling)

    CallbackContext callback;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        this.mimeType = args.getString(0);
        this.allowMultipleSelection = args.getBoolean(1);

        if (action.equals(ACTION_OPEN)) {
            chooseFile(callbackContext);
            return true;
        }

        return false;
    }

    public void chooseFile(CallbackContext callbackContext) {

        if (cordova.hasPermission(READ)) {
            startFileChooser();
        } else {
            getReadPermission(SEARCH_REQ_CODE);
        }

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callback = callbackContext;
        callbackContext.sendPluginResult(pluginResult);
    }

    protected void getReadPermission(int requestCode) {
        cordova.requestPermission(this, requestCode, READ);
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == 0 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startFileChooser();
        } else {
            callback.error("Permission to read external storage denied");
        }
    }

    private void startFileChooser() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(this.mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, this.allowMultipleSelection);

        Intent chooser = Intent.createChooser(intent, "Select File");
        cordova.startActivityForResult(this, chooser, PICK_FILE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == PICK_FILE_REQUEST && callback != null) {

            if (resultCode == Activity.RESULT_OK) {

                JSONObject intentJson = getIntentJson(intent);

                Log.w(PLUGIN_NAME, intentJson.toString());
                callback.success(intentJson);

            } else if (resultCode == Activity.RESULT_CANCELED) {

                // TODO NO_RESULT or error callback?
                PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
                callback.sendPluginResult(pluginResult);

            } else {

                callback.error(resultCode);
            }
        }
    }

    /**
     * Return JSON representation of intent attributes
     *
     * @param intent
     * @return
     */
    private JSONObject getIntentJson(Intent intent) {
        JSONObject intentJSON = null;
        ClipData clipData = null;
        JSONObject[] items = null;
        ContentResolver cR = this.cordova.getActivity().getApplicationContext().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            clipData = intent.getClipData();
            if (clipData != null) {
                int clipItemCount = clipData.getItemCount();
                items = new JSONObject[clipItemCount];

                for (int i = 0; i < clipItemCount; i++) {

                    ClipData.Item item = clipData.getItemAt(i);

                    try {
                        items[i] = new JSONObject();

                        if (item.getUri() != null) {

                            items[i].put("uri", item.getUri());

                            String type = cR.getType(item.getUri());
                            String extension = mime.getExtensionFromMimeType(cR.getType(item.getUri()));

                            items[i].put("type", type);
                            items[i].put("extension", extension);
                        }

                    } catch (JSONException e) {
                        Log.d(PLUGIN_NAME, PLUGIN_NAME + " Error thrown during intent > JSON conversion");
                        Log.d(PLUGIN_NAME, e.getMessage());
                        Log.d(PLUGIN_NAME, Arrays.toString(e.getStackTrace()));
                    }

                }
            }
        }

        try {
            intentJSON = new JSONObject();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (items != null) {
                    intentJSON.put("items", new JSONArray(items));
                }
            }

            if (intent.getData() != null) {
                items = new JSONObject[1];
                items[0] = new JSONObject();
                items[0].put("uri", intent.getData());

                if (intent.getData() != null) {
                    String type = cR.getType(intent.getData());
                    String extension = mime.getExtensionFromMimeType(cR.getType(intent.getData()));

                    items[0].put("type", type);
                    items[0].put("extension", extension);
                }
                intentJSON.put("items", new JSONArray(items));
            }

            return intentJSON;
        } catch (JSONException e) {
            Log.d(PLUGIN_NAME, PLUGIN_NAME + " Error thrown during intent > JSON conversion");
            Log.d(PLUGIN_NAME, e.getMessage());
            Log.d(PLUGIN_NAME, Arrays.toString(e.getStackTrace()));

            return null;
        }
    }

}
